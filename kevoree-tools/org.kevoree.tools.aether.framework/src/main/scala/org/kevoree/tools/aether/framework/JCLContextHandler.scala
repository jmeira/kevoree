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
/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
package org.kevoree.tools.aether.framework

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

import java.io.File
import org.slf4j.LoggerFactory
import org.kevoree.DeployUnit
import scala.collection.JavaConversions._
import org.kevoree.api.service.core.classloading.{DeployUnitResolver, KevoreeClassLoaderHandler}
import org.kevoree.kcl.KevoreeJarClassLoader
import scala.Predef._
import java.util.concurrent.{ThreadFactory, Executors, Callable}
import java.util

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 14:29
 */

class JCLContextHandler extends KevoreeClassLoaderHandler {

  protected val kcl_cache = new java.util.HashMap[String, KevoreeJarClassLoader]()
  protected val kcl_cache_file = new java.util.HashMap[String, File]()
  protected var lockedDu: List[String] = List()
  protected val logger = LoggerFactory.getLogger(this.getClass)

  case class DUMP() extends Runnable {
    def run() {
      printDumpInternals()
    }
  }

  case class INSTALL_DEPLOYUNIT_FILE(du: DeployUnit, file: File) extends Callable[KevoreeJarClassLoader] {
    def call() = installDeployUnitInternals(du, file)
  }

  case class INSTALL_DEPLOYUNIT(du: DeployUnit) extends Callable[KevoreeJarClassLoader] {
    def call() = installDeployUnitNoFileInternals(du)
  }

  case class REMOVE_DEPLOYUNIT(du: DeployUnit) extends Runnable {
    def run() {
      removeDeployUnitInternals(du)
    }
  }

  case class GET_KCL(du: DeployUnit) extends Callable[KevoreeJarClassLoader] {
    def call() = getKCLInternals(du)
  }

  case class MANUALLY_ADD_TO_CACHE(du: DeployUnit, kcl: KevoreeJarClassLoader, toLock : Boolean) extends Runnable {
    def run() {
      manuallyAddToCacheInternals(du, kcl,toLock)
    }
  }

  case class CLEAR() extends Runnable {
    def run() {
      clearInternals()
    }
  }

  protected val resolvers = new util.ArrayList[DeployUnitResolver]

  def registerDeployUnitResolver(dur: DeployUnitResolver) {
     pool.submit(Add_Resolver(dur))
  }
  case class Add_Resolver(dur: DeployUnitResolver) extends Runnable {
    def run() {
      resolvers.add(dur)
    }
  }
  def unregisterDeployUnitResolver(dur: DeployUnitResolver) {
    pool.submit(Remove_Resolver(dur))
  }
  case class Remove_Resolver(dur: DeployUnitResolver) extends Runnable {
    def run() {
      resolvers.remove(dur)
    }
  }

  //case class KILLActor()

  def stop() {
    //TODO REMOVE ALL BEFORE
    pool.shutdownNow()
    resolvers.clear()
  }

  val pool = Executors.newSingleThreadExecutor(new ThreadFactory() {
    val s = System.getSecurityManager
    val group = if (s != null) {
      s.getThreadGroup
    } else {
      Thread.currentThread().getThreadGroup
    }

    def newThread(p1: Runnable) = {
      val t = new Thread(group, p1, "Kevoree_KCLHandler_Scheduler_" + hashCode())
      if (t.isDaemon) {
        t.setDaemon(false)
      }
      if (t.getPriority != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY)
      }
      t
    }
  })

  case class GET_CACHE_FILE(du: DeployUnit) extends Callable[File] {
    def call(): File = {
      getCacheFileInternals(du)
    }
  }

  /*
def act() {
loop {
 react {
   // case GET_CACHE_FILE(du) => reply(getCacheFileInternals(du))
   //case INSTALL_DEPLOYUNIT_FILE(du, file) => reply(installDeployUnitInternals(du, file))
   case INSTALL_DEPLOYUNIT(du) => reply(installDeployUnitNoFileInternals(du))
  // case GET_KCL(du) => reply(getKCLInternals(du))
   case REMOVE_DEPLOYUNIT(du) => removeDeployUnitInternals(du)
   //case MANUALLY_ADD_TO_CACHE(du, kcl) => manuallyAddToCacheInternals(du, kcl)
   //case DUMP() => printDumpInternals()
  // case CLEAR() => clearInternals(); reply()
   //case KILLActor() => exit()
 }
}
}     */

  def clear() {
    pool.submit(CLEAR()).get()
  }

  def getCacheFile(du: DeployUnit): File = {
    pool.submit(GET_CACHE_FILE(du)).get()
  }

  def manuallyAddToCache(du: DeployUnit, kcl: KevoreeJarClassLoader) {
    pool.submit(MANUALLY_ADD_TO_CACHE(du, kcl,toLock = true))
  }

  def attachKCL(du: DeployUnit, kcl: KevoreeJarClassLoader) {
    pool.submit(MANUALLY_ADD_TO_CACHE(du, kcl,toLock = false)).get()
  }

  def printDump() {
    pool.submit(DUMP())
  }

  protected def clearInternals() {
    logger.debug("Clear Internal")
    kcl_cache.keySet().toList.foreach {
      key =>
        if (!lockedDu.contains(key)) {
          if (kcl_cache.containsKey(key)) {
            logger.debug("Remove KCL for {}", key)
            kcl_cache.get(key).unload()
            kcl_cache.remove(key)
          }
          if (kcl_cache_file.containsKey(key)) {
            kcl_cache_file.remove(key)
          }
        }
    }
    failedLinks.clear()
    /*
    if (logger.isDebugEnabled) {
      logger.debug("-----------------------------DUMP after clear-------------------------")
      printDumpInternals()
      logger.debug("-----------------------------END DUMP after clear-------------------------")
    }*/
  }

  protected def getCacheFileInternals(du: DeployUnit): File = {
    kcl_cache_file.get(buildKEY(du))
  }

  private def manuallyAddToCacheInternals(du: DeployUnit, kcl: KevoreeJarClassLoader, toLock : Boolean) {
    kcl_cache.put(buildKEY(du), kcl)
    if(toLock){
      lockedDu = lockedDu ++ List(buildKEY(du))
    }
    // kcl_cache_file.put(buildKEY(du), f)
  }

  protected def printDumpInternals() {
    logger.debug("------------------ KCL Dump -----------------------")
    kcl_cache.foreach {
      k =>
        logger.debug("Dump = {}", k._1)
        k._2.printDump()
    }
    logger.debug("================== End KCL Dump ===================")
  }


  /* Temp Zone for temporary unresolved KCL links */
  protected val failedLinks = new java.util.HashMap[String, KevoreeJarClassLoader]()

  def clearFailedLinks() {
    failedLinks.clear()
  }

  protected def installDeployUnitInternals(du: DeployUnit, file: File): KevoreeJarClassLoader = {
    val previousKCL = getKCLInternals(du)
    val res = if (previousKCL != null) {
      logger.debug("Take already installed {}", buildKEY(du))
      previousKCL
    } else {
      logger.debug("Install {} , file {}", buildKEY(du), file)
      val newcl = new KevoreeJarClassLoader

      //if (du.getVersion.contains("SNAPSHOT")) {

      if (System.getProperty("kcl.lazy") != null && "true".equals(System.getProperty("kcl.lazy"))) {
        newcl.setLazyLoad(true)
      } else {
        newcl.setLazyLoad(false)
      }

      // }

      newcl.add(file.getAbsolutePath)
      kcl_cache.put(buildKEY(du), newcl)
      kcl_cache_file.put(buildKEY(du), file)
      logger.debug("Add KCL for {}->{}", du.getUnitName, buildKEY(du))

      //TRY TO RECOVER FAILED LINK
      if (failedLinks.containsKey(buildKEY(du))) {
        failedLinks.get(buildKEY(du)).addSubClassLoader(newcl)
        newcl.addWeakClassLoader(failedLinks.get(buildKEY(du)))
        failedLinks.remove(buildKEY(du))
        logger.debug("Failed Link {} remain size : {}", du.getUnitName, failedLinks.size())
      }

      du.getRequiredLibs.foreach {
        rLib =>
          val kcl = getKCLInternals(rLib)
          if (kcl != null) {
            logger.debug("Link KCL for {}->{}", du.getUnitName, rLib.getUnitName)
            newcl.addSubClassLoader(kcl)
            kcl.addWeakClassLoader(newcl)

            du.getRequiredLibs.filter(rLibIn => rLib != rLibIn).foreach(rLibIn => {
              val kcl2 = getKCLInternals(rLibIn)
              if (kcl2 != null) {
                kcl.addWeakClassLoader(kcl2)
                // logger.debug("Link Weak for {}->{}", rLib.getUnitName, rLibIn.getUnitName)
              }
            })
          } else {
            logger.debug("Fail link ! Warning ")
            failedLinks.put(buildKEY(du), newcl)
          }
      }
      newcl
    }
    res
  }

  protected def getKCLInternals(du: DeployUnit): KevoreeJarClassLoader = {
    kcl_cache.get(buildKEY(du))
  }

  protected def removeDeployUnitInternals(du: DeployUnit) {

    val key = buildKEY(du)
    if (failedLinks.containsKey(key)) {
      failedLinks.remove(key)
    }
    if (!lockedDu.contains(key)) {
      val kcl_to_remove = kcl_cache.get(key)
      failedLinks.filter(fl => fl._2 == kcl_to_remove).toList.foreach {
        k =>
          failedLinks.remove(k._1)
      }
      if (!lockedDu.contains(key)) {
        if (kcl_cache.containsKey(key)) {
          logger.debug("Try to remove KCL for {}->{}", du.getUnitName, buildKEY(du))
          logger.debug("Cache To cleanuip size" + kcl_cache.values().size() + "-" + kcl_cache.size() + "-" + kcl_cache.keySet().size())
          kcl_cache.values().foreach {
            vals => {
              if (vals.getSubClassLoaders().contains(kcl_to_remove)) {
                failedLinks.put(key, vals)
                logger.debug("Pending Fail link " + key)
              }
              vals.cleanupLinks(kcl_to_remove)
              logger.debug("Cleanup {} from {}", vals.toString(), du.getUnitName)
            }
          }

          val toRemoveKCL = kcl_cache.get(key)
          try {
            var rootGroup = Thread.currentThread().getThreadGroup
            var parentGroup: ThreadGroup = null
            while ( {
              parentGroup = rootGroup.getParent
              parentGroup
            } != null) {
              rootGroup = parentGroup
            }
            val numThreads = rootGroup.activeCount()
            val listOfThreads = new Array[Thread](numThreads)
            rootGroup.enumerate(listOfThreads)
            for (i <- 0 until numThreads) {
              val tloop = listOfThreads(i)
              if (tloop != null && tloop.getContextClassLoader == toRemoveKCL) {
                logger.debug("Change Thread " + tloop.getName + " currentClassLoader to avoid memory leak")
                tloop.setContextClassLoader(getClass.getClassLoader)
              }
            }
          } catch {
            case _ =>
          }
          toRemoveKCL.unload()
          kcl_cache.remove(key)
        }
        if (kcl_cache_file.containsKey(key)) {
          logger.debug("Cleanup Cache File" + kcl_cache_file.get(key).getAbsolutePath)
          kcl_cache_file.get(key).delete()
          kcl_cache_file.remove(key)
          logger.debug("Remove File Cache " + key)
        }
      }
    }
  }


  protected def buildKEY(du: DeployUnit): String = {
    du.getName + "/" + buildQuery(du, None)
  }

  private def buildQuery(du: DeployUnit, repoUrl: Option[String]): String = {
    val query = new StringBuilder
    query.append("mvn:")
    repoUrl match {
      case Some(r) => query.append(r); query.append("!")
      case None =>
    }
    query.append(du.getGroupName)
    query.append("/")
    query.append(du.getUnitName)
    du.getVersion match {
      case "default" =>
      case "" =>
      case _ => query.append("/"); query.append(du.getVersion)
    }
    query.toString()
  }


  def installDeployUnit(du: DeployUnit, file: File): KevoreeJarClassLoader = {
    pool.submit(INSTALL_DEPLOYUNIT_FILE(du, file)).get()
  }

  def getKevoreeClassLoader(du: DeployUnit): KevoreeJarClassLoader = {
    pool.submit(GET_KCL(du)).get()
  }

  def removeDeployUnitClassLoader(du: DeployUnit) {
    pool.submit(REMOVE_DEPLOYUNIT(du)).get()
  }


  def installDeployUnitNoFileInternals(du: DeployUnit): KevoreeJarClassLoader = {

    var resolvedFile : File = null
    resolvers.exists{ res =>
        try {
          resolvedFile = res.resolve(du)
          true
        } catch {
          case _ @ e => false
        }
    }
    if (resolvedFile == null) {
      resolvedFile = AetherUtil.resolveDeployUnit(du)
    }
    if (resolvedFile != null) {
      installDeployUnitInternals(du, resolvedFile)
    } else {
      logger.error("Error while resolving deploy unit " + du.getUnitName)
      null
    }
  }

  def installDeployUnit(du: DeployUnit): KevoreeJarClassLoader = {
    pool.submit(INSTALL_DEPLOYUNIT(du)).get()
  }

  def getKCLDump: String = {
    val buffer = new StringBuffer
    kcl_cache.foreach {
      k =>
        buffer.append("KCL KEY name=" + k._1 + "\n")
        buffer.append(k._2.getKCLDump + "\n")
    }

    buffer.toString
  }

}