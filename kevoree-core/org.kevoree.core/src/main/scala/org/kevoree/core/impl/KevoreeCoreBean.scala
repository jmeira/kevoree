/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kevoree.core.impl

import _root_.org.kevoree.KevoreeFactory
import _root_.org.kevoree.ContainerRoot
import _root_.org.kevoree.api.configuration.ConfigurationService
import _root_.org.slf4j.LoggerFactory
import _root_.org.kevoree.api.configuration.ConfigConstants
import _root_.org.kevoree.framework._
import deploy.PrimitiveCommandExecutionHelper
import _root_.org.kevoree.cloner.ModelCloner
import _root_.org.kevoree.core.basechecker.RootChecker
import _root_.java.util.{UUID, Date}
import org.kevoree.api.service.core.handler._
import org.kevoree.api.service.core.script.KevScriptEngineFactory
import org.kevoree.api.Bootstraper
import org.kevoree.core.basechecker.kevoreeVersionChecker.KevoreeNodeVersionChecker
import java.lang.Long
import reflect.BeanProperty
import java.util.concurrent._
import scala.Some
import scala.Tuple2
import org.kevoree.context.{ContextRoot, ContextModel}


class KevoreeCoreBean extends KevoreeModelHandlerService {

  val modelListeners = new KevoreeListeners

  @BeanProperty var configService: ConfigurationService = null
  var kevsEngineFactory: KevScriptEngineFactory = null

  def setKevsEngineFactory(k: KevScriptEngineFactory) {
    kevsEngineFactory = k
  }

  @BeanProperty var bootstraper: Bootstraper = null

  var nodeName: String = ""

  def getNodeName: String = nodeName

  private var modelVersionChecker: KevoreeNodeVersionChecker = null

  def setNodeName(nn: String) {
    nodeName = nn
    modelVersionChecker = new KevoreeNodeVersionChecker(nodeName)
  }

  @BeanProperty var nodeInstance: org.kevoree.api.NodeType = null

  var models: scala.collection.mutable.ArrayBuffer[ContainerRoot] = new scala.collection.mutable.ArrayBuffer[ContainerRoot]()
  var model: ContainerRoot = KevoreeFactory.eINSTANCE.createContainerRoot
  var lastDate: Date = new Date(System.currentTimeMillis)
  var currentModelUUID: UUID = UUID.randomUUID()

  def getLastModification = lastDate

  var logger = LoggerFactory.getLogger(this.getClass)
  val modelCloner = new ModelCloner
  val modelChecker = new RootChecker
  private var scheduler: ExecutorService = _
  var selfActorPointer = this

  private def checkBootstrapNode(currentModel: ContainerRoot): Unit = {
    try {
      if (nodeInstance == null) {
        currentModel.getNodes.find(n => n.getName == nodeName) match {
          case Some(foundNode) => {
            bootstraper.bootstrapNodeType(currentModel, nodeName, this, kevsEngineFactory) match {
              case Some(ist: org.kevoree.api.NodeType) => {
                nodeInstance = ist
                nodeInstance.startNode()
                //SET CURRENT MODEL
                model = modelCloner.clone(currentModel)
                model.removeAllGroups()
                model.removeAllHubs()
                model.removeAllMBindings()
                model.getNodes.filter(n => n.getName != nodeName).foreach {
                  node =>
                    model.removeNodes(node)
                }
                model.getNodes(0).removeAllComponents()
                model.getNodes(0).removeAllHosts()

              }
              case None => logger.error("TypeDef installation fail !")
            }
          }
          case None => logger.error("Node instance name " + nodeName + " not found in bootstrap model !")
        }
      }
    } catch {
      case _@e => {
        logger.error("Error while bootstraping node instance ", e)
        logger.debug(bootstraper.getKevoreeClassLoaderHandler.getKCLDump)
        try {
          nodeInstance.stopNode()
        } catch {
          case _ =>
        } finally {
          bootstraper.clear
        }
        nodeInstance = null
      }
    }
  }

  private def checkUnbootstrapNode(currentModel: ContainerRoot): Option[ContainerRoot] = {
    try {
      if (nodeInstance != null) {
        currentModel.getNodes.find(n => n.getName == nodeName) match {
          case Some(foundNode) => {
            val modelTmp = modelCloner.clone(currentModel)
            modelTmp.removeAllGroups()
            modelTmp.removeAllHubs()
            modelTmp.removeAllMBindings()
            modelTmp.getNodes.filter(n => n.getName != nodeName).foreach {
              node =>
                modelTmp.removeNodes(node)
            }
            modelTmp.getNodes(0).removeAllComponents()
            modelTmp.getNodes(0).removeAllHosts()
            Some(modelTmp)
          }
          case None => logger.error("TypeDef installation fail !"); None
        }
      } else {
        logger.error("node instance is not available on current model !")
        None
      }
    } catch {
      case _@e => logger.error("Error while unbootstraping node instance ", e); None
    }
  }

  private def switchToNewModel(c: ContainerRoot) = {

    var cc = c
    if(!c.isReadOnly()){
       logger.error("It is not safe to store ReadWrite model")
       cc = modelCloner.clone(c,true)
    }

    //current model is backed-up
    models.append(model)
    // TODO : MAGIC NUMBER ;-) , ONLY KEEP 10 PREVIOUS MODEL
    if (models.size > 15) {
      models = models.drop(5)
      logger.debug("Garbage old previous model")
    }
    //Changes the current model by the new model
    model = cc
    currentModelUUID = UUID.randomUUID()
    lastDate = new Date(System.currentTimeMillis)
    //Fires the update to listeners
    modelListeners.notifyAllListener()
  }

  def start() = {
    if (getNodeName == "") {
      setNodeName(configService.getProperty(ConfigConstants.KEVOREE_NODE_NAME))
    }
    modelListeners.start(getNodeName())
    logger.info("Kevoree Start event : node name = " + getNodeName)
    scheduler = java.util.concurrent.Executors.newSingleThreadExecutor(new ThreadFactory() {
      val s = System.getSecurityManager
      val group = if (s != null) {
        s.getThreadGroup
      } else {
        Thread.currentThread().getThreadGroup
      }
      def newThread(p1: Runnable) = {
        val t = new Thread(group, p1, "Kevoree_Core_Scheduler_" + getNodeName)
        if (t.isDaemon) {
          t.setDaemon(false)
        }
        if (t.getPriority != Thread.NORM_PRIORITY) {
          t.setPriority(Thread.NORM_PRIORITY)
        }
        t
      }
    })
    this
  }


  def stop() {
    logger.warn("Kevoree Core will be stopped !")
    modelListeners.stop()
    scheduler.shutdownNow()
    scheduler = null
    if (nodeInstance != null) {
      try {
        val stopModel = checkUnbootstrapNode(model)
        if (stopModel.isDefined) {
          val adaptationModel = nodeInstance.kompare(model, stopModel.get)
          adaptationModel.setInternalReadOnly()

          def afterUpdateTest(): Boolean = {
            true
            //listenerActor.afterUpdate(model, stopModel.get)
          }
          val rootNode = model.getNodes.find(n => n.getName == getNodeName).get
          val ignoredDeployResult = PrimitiveCommandExecutionHelper.execute(rootNode, adaptationModel, nodeInstance, afterUpdateTest, afterUpdateTest, afterUpdateTest)
        } else {
          logger.error("Unable to use the stopModel !")
        }

      } catch {
        case _@e => {
          logger.error("Error while unbootstrap ", e)
        }
      }
      try {
        logger.debug("Call instance stop")
        nodeInstance.stopNode()
        nodeInstance == null
        bootstraper.clear
      } catch {
        case _@e => {
          logger.error("Error while stopping node instance ", e)
        }
      }
    }
    logger.debug("Kevoree core stopped ")
  }


  // treatment of incoming messages


  private def internal_update_model(proposedNewModel: ContainerRoot): Boolean = {
    if (proposedNewModel == null || !proposedNewModel.getNodes.exists(p => p.getName == getNodeName())) {
      logger.error("Asking for update with a NULL model or node name was not found in target model !")
      false
    } else {
      try {

        val readOnlyNewModel = modelCloner.clone(proposedNewModel,true)
        //Model checking
        val checkResult = modelChecker.check(readOnlyNewModel)
        val versionCheckResult = modelVersionChecker.check(readOnlyNewModel)
        if ((!checkResult.isEmpty) || (!versionCheckResult.isEmpty)) {
          logger.error("There is check failure on update model, update refused !")
          import _root_.scala.collection.JavaConversions._
          (checkResult ++ versionCheckResult).foreach {
            cr =>
              logger.error("error=>" + cr.getMessage + ",objects" + cr.getTargetObjects.mkString(","))
          }
          false
        } else {

          //Model check is OK.
          logger.debug("Before listeners PreCheck !")
          val preCheckResult = modelListeners.preUpdate(model, readOnlyNewModel)
          logger.debug("PreCheck result = " + preCheckResult)

          logger.debug("Before listeners InitUpdate !")
          val initUpdateResult = modelListeners.initUpdate(model, readOnlyNewModel)
          logger.debug("InitUpdate result = " + initUpdateResult)

          if (preCheckResult && initUpdateResult) {

            var newmodel = readOnlyNewModel
            //CHECK FOR HARA KIRI
            var previousHaraKiriModel: ContainerRoot = null
            if (HaraKiriHelper.detectNodeHaraKiri(model, readOnlyNewModel, getNodeName())) {
              logger.warn("HaraKiri detected , flush platform")
              previousHaraKiriModel = model
              // Creates an empty model, removes the current node (harakiri)
              newmodel = checkUnbootstrapNode(model).get
              try {
                // Compare the two models and plan the adaptation
                val adaptationModel = nodeInstance.kompare(model, newmodel)
                adaptationModel.setInternalReadOnly()

                if (logger.isDebugEnabled) {
                  //Avoid the loop if the debug is not activated
                  logger.debug("Adaptation model size " + adaptationModel.getAdaptations.size)
                  adaptationModel.getAdaptations.foreach {
                    adaptation =>
                      logger.debug("primitive " + adaptation.getPrimitiveType.getName)
                  }
                }
                //Executes the adaptation
                def afterUpdateTest(): Boolean = {
                  true
                  //listenerActor.afterUpdate(model, newmodel)
                }
                val rootNode = newmodel.getNodes.find(n => n.getName == getNodeName).get
                PrimitiveCommandExecutionHelper.execute(rootNode, adaptationModel, nodeInstance, afterUpdateTest, afterUpdateTest, afterUpdateTest)
                nodeInstance.stopNode()
                //end of harakiri
                nodeInstance = null
                bootstraper.clear //CLEAR
                //place the current model as an empty model (for backup)
                switchToNewModel(KevoreeFactory.createContainerRoot)

                //prepares for deployment of the new system
                newmodel = readOnlyNewModel
              } catch {
                case _@e => {
                  logger.error("Error while update ", e)
                }
              }
              logger.debug("End HaraKiri")
            }

            //Checks and bootstrap the node
            checkBootstrapNode(newmodel)
            val milli = System.currentTimeMillis
            logger.debug("Begin update model " + milli)
            var deployResult = true
            try {
              // Compare the two models and plan the adaptation
              logger.info("Comparing models and planning adaptation.")
              val adaptationModel = nodeInstance.kompare(model, newmodel)
              adaptationModel.setInternalReadOnly()

              //Execution of the adaptation
              logger.info("Launching adaptation of the system.")
              def afterUpdateTest(): Boolean = {
                modelListeners.afterUpdate(model, newmodel)
              }

              class PreCommand() {
                var alreadyCall = false

                def preRollbackTest(): Boolean = {
                  if (!alreadyCall) {
                    modelListeners.preRollback(model, newmodel)
                    alreadyCall = true
                  }
                  true
                }
              }

              val preCmd = new PreCommand


              def postRollbackTest(): Boolean = {
                modelListeners.postRollback(model, newmodel)
                true
              }
              val rootNode = newmodel.getNodes.find(n => n.getName == getNodeName).get
              deployResult = PrimitiveCommandExecutionHelper.execute(rootNode, adaptationModel, nodeInstance, afterUpdateTest, preCmd.preRollbackTest, postRollbackTest)
            } catch {
              case _@e => {
                logger.error("Error while update ", e)
                deployResult = false
              }
            }
            if (deployResult) {
              switchToNewModel(newmodel)
              logger.info("Update sucessfully completed.")
            } else {
              //KEEP FAIL MODEL, TODO
              logger.warn("Update failed")
              //IF HARAKIRI
              if (previousHaraKiriModel != null) {
                internal_update_model(previousHaraKiriModel)
                previousHaraKiriModel = null //CLEAR
              }
            }
            val milliEnd = System.currentTimeMillis - milli
            logger.debug("End deploy result=" + deployResult + "-" + milliEnd)
            deployResult
          } else {
            logger.debug("PreCheck or InitUpdate Step was refused, update aborded !")
            false
          }
        }
      } catch {
        case _@e =>
          logger.error("Error while update", e)
          false
      }
    }
  }

  /**
   * Returns the current model.
   * @see : Consider using #getLastUUIDModel for concurrency reasons
   */
  @Deprecated
  override def getLastModel: ContainerRoot = {
    scheduler.submit(LastModelCallable()).get()
  }

  private case class LastModelCallable() extends Callable[ContainerRoot] {
    def call(): ContainerRoot = {
      model
    }
  }

  /**
   * Returns the current model with a unique token
   */
  override def getLastUUIDModel: UUIDModel = {
    scheduler.submit(LastUUIDModelCallable()).get()
  }

  private case class LastUUIDModelCallable() extends Callable[UUIDModel] {
    def call(): UUIDModel = {
      UUIDModelImpl(currentModelUUID, model)
    }
  }


  /**
   * Asks for an update of the system to the new system described by the model.
   * @see Consider using #compareAndSwapModel for concurrency reasons
   */
  @Deprecated
  override def updateModel(model: ContainerRoot) {
    scheduler.submit(UpdateModelCallable(model, null))
  }

  case class UpdateModelCallable(model: ContainerRoot, callback: ModelUpdateCallback) extends Callable[Boolean] {
    def call(): Boolean = {
      val res = if (currentLock == null) {
        val internalRes = internal_update_model(model)
        callCallBack(callback, internalRes, null)
        internalRes
      } else {
        logger.debug("Core Locked , UUID mandatory")
        callCallBack(callback, false, ModelUpdateCallBackReturn.CAS_ERROR)
        false
      }
      //System.gc()
      res
    }
  }


  def callCallBack(callback: ModelUpdateCallback, sucess: Boolean, res: ModelUpdateCallBackReturn) {
    if (callback != null) {
      new Thread() {
        override def run() {
          if (res == null) {
            callback.modelProcessed(if (sucess) {
              ModelUpdateCallBackReturn.UPDATED
            } else {
              ModelUpdateCallBackReturn.DEPLOY_ERROR
            })
          } else {
            callback.modelProcessed(res)
          }
        }
      }.start()
    }
  }


  /**
   * Asks for an update of the system to fit the model given in parameter.<br/>
   * This method is blocking until the update is done.
   * @see Consider using #atomicCompareAndSwapModel for concurrency reasons.
   */
  @Deprecated
  override def atomicUpdateModel(model: ContainerRoot) = {
    scheduler.submit(UpdateModelCallable(model, null)).get()
    lastDate
  }

  case class CompareAndSwapCallable(previousModel: UUIDModel, targetModel: ContainerRoot, callback: ModelUpdateCallback) extends Callable[Boolean] {
    def call(): Boolean = {
      val res = if (currentLock != null) {
        if (previousModel.getUUID.compareTo(currentLock._1) == 0) {
          val internalRes = internal_update_model(targetModel)
          callCallBack(callback, internalRes, null)
          internalRes
        } else {
          logger.debug("Core Locked , bad UUID " + previousModel.getUUID)
          callCallBack(callback, false, ModelUpdateCallBackReturn.CAS_ERROR)
          false //LOCK REFUSED !
        }
      } else {
        //COMMON CHECK
        if (previousModel.getUUID.compareTo(currentModelUUID) == 0) {
          //TODO CHECK WITH MODEL SHA-1 HASHCODE
          val internalRes = internal_update_model(targetModel)
          callCallBack(callback, internalRes, null)
          internalRes
        } else {
          callCallBack(callback, false, ModelUpdateCallBackReturn.CAS_ERROR)
          false
        }
      }
      //System.gc()
      res
    }
  }

  /**
   * Compares the UUID of the model and the current UUID to verify that no update occurred in between the moment the model had been delivered and the moment the update is asked.<br/>
   * If OK, updates the system and switches to the new model, asynchronously
   */
  override def compareAndSwapModel(previousModel: UUIDModel, targetModel: ContainerRoot) {
    scheduler.submit(CompareAndSwapCallable(previousModel, targetModel, null))
  }

  /**
   * Compares the UUID of the model and the current UUID to verify that no update occurred in between the moment the model had been delivered and the moment the update is asked.<br/>
   * If OK, updates the system and switches to the new model, synchronously (blocking)
   */
  @throws(classOf[KevoreeModelUpdateException])
  override def atomicCompareAndSwapModel(previousModel: UUIDModel, targetModel: ContainerRoot): Date = {
    val result = scheduler.submit(CompareAndSwapCallable(previousModel, targetModel, null)).get()
    if (!result) {
      throw new KevoreeModelUpdateException //SEND AND EXCEPTION - Compare&Swap fail !
    }
    lastDate
  }

  /**
   * Provides the collection of last models (short system history)
   */
  override def getPreviousModel: java.util.List[ContainerRoot] = {
    scheduler.submit(GetPreviousModelCallable()).get()
  }

  private case class GetPreviousModelCallable() extends Callable[java.util.List[ContainerRoot]] {
    def call(): java.util.List[ContainerRoot] = {
      import scala.collection.JavaConversions._
      models.toList
    }
  }


  override def registerModelListener(listener: ModelListener) {
    modelListeners.addListener(listener)
  }

  override def unregisterModelListener(listener: ModelListener) {
    modelListeners.removeListener(listener)
  }

  def getContextModel: ContextRoot = {
    nodeInstance.getContextModel
  }

  /* Lock Management */

  private var currentLock: Tuple2[UUID, ModelHandlerLockCallBack] = null

  case class RELEASE_LOCK(uuid: UUID)

  def acquireLock(callBack: ModelHandlerLockCallBack, timeout: Long) {
    scheduler.submit(AcquireLock(callBack, timeout))
  }

  private var lockWatchDog: ScheduledExecutorService = _
  private var futurWatchDog: ScheduledFuture[_] = _

  case class AcquireLock(callBack: ModelHandlerLockCallBack, timeout: Long) extends Runnable {
    def run() {
      if (currentLock != null) {
        callBack.lockRejected()
      } else {
        val lockUUID = UUID.randomUUID()
        currentLock = (lockUUID, callBack)
        lockWatchDog = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        futurWatchDog = lockWatchDog.schedule(WatchDogCallable(), timeout, TimeUnit.MILLISECONDS)
        callBack.lockAcquired(lockUUID)
      }
    }
  }

  case class WatchDogCallable() extends Runnable {
    def run() {
      lockTimeout()
    }
  }

  def releaseLock(uuid: UUID) {
    scheduler.submit(ReleaseLockCallable(uuid))
  }

  case class ReleaseLockCallable(uuid: UUID) extends Runnable {
    def run() {
      if (currentLock != null) {
        if (currentLock._1.compareTo(uuid) == 0) {
          currentLock = null
          futurWatchDog.cancel(true)
          futurWatchDog = null
          lockWatchDog.shutdownNow()
          lockWatchDog = null
        }
      }
    }
  }

  private def lockTimeout() {
    scheduler.submit(LockTimeoutCallable())
  }

  case class LockTimeoutCallable() extends Runnable {
    def run() {
      if (currentLock != null) {
        currentLock._2.lockTimeout()
        currentLock = null
        lockWatchDog.shutdownNow()
        lockWatchDog = null
        futurWatchDog = null
      }
    }
  }

  def checkModel(tModel: ContainerRoot): Boolean = {
    val checkResult = modelChecker.check(model)
    if (checkResult.isEmpty) {
      modelListeners.preUpdate(model, modelCloner.clone(tModel,true))
    } else {
      false
    }
  }

  def updateModel(model: ContainerRoot, callback: ModelUpdateCallback) {
    scheduler.submit(UpdateModelCallable(model, callback))
  }

  def compareAndSwapModel(previousModel: UUIDModel, targetModel: ContainerRoot, callback: ModelUpdateCallback) {
    scheduler.submit(UpdateModelCallable(model, callback)).get()
    lastDate
  }
}
