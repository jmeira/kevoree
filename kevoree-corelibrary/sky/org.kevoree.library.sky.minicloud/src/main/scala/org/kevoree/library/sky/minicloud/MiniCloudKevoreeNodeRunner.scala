package org.kevoree.library.sky.minicloud

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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io._
import java.lang.Thread
import util.matching.Regex
import org.kevoree.library.sky.api.nodeType.IaaSNode
import org.kevoree.{ContainerRoot, KevoreeFactory}
import org.kevoree.framework.KevoreeXmiHelper
import org.kevoree.library.sky.api.{ProcessStreamFileLogger, KevoreeNodeRunner}

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 20/09/11
 * Time: 11:46
 *
 * @author Erwan Daubert
 * @version 1.0
 */
class MiniCloudKevoreeNodeRunner (nodeName: String, iaasNode: IaaSNode) extends KevoreeNodeRunner(nodeName) {
  private val logger: Logger = LoggerFactory.getLogger(classOf[MiniCloudKevoreeNodeRunner])
  private var nodePlatformProcess: Process = null

  val backupRegex = new Regex(".*<saveRes(.*)/>.*")
  val deployRegex = new Regex(".*<deployRes(.*)/>.*")
  val errorRegex = new Regex(".*Error while update.*")

  case class DeployResult (uuid: String)

  case class BackupResult (uuid: String)

  case class ErrorResult ()

  def startNode (iaasModel: ContainerRoot, jailBootStrapModel: ContainerRoot): Boolean = {
    try {
      logger.debug("Start " + nodeName)

      // find the classpath of the current node
      // if classpath is null then we download the jar with aether
      // else we start the child node with the same classpath as its parent.
      // main class  = org.kevoree.platform.standalone.app.Main
      logger.debug(System.getProperty("java.class.path"))
      if (System.getProperty("java.class.path") != null) {
        logger.debug("trying to start child node with parent's classpath")
        if (!startWithClassPath(jailBootStrapModel)) {
          logger.debug("Unable to start child node with parent's classpath, try to use aether bootstrap")
          startWithAether(jailBootStrapModel)
        } else {
          true
        }
      } else {
        logger.debug("trying to start child node with aether bootstrap")
        startWithAether(jailBootStrapModel)
      }


      /*val platformFile = iaasNode.getBootStrapperService.resolveKevoreeArtifact("org.kevoree.platform.standalone", "org.kevoree.platform", KevoreeFactory.getVersion)
      val java: String = getJava
      if (platformFile != null) {

        val tempFile = File.createTempFile("bootModel" + nodeName, ".kev")
        KevoreeXmiHelper.save(tempFile.getAbsolutePath, jailBootStrapModel)
        // FIXME java memory properties must define as Node properties
        nodePlatformProcess = Runtime.getRuntime
          .exec(Array[String](java, "-Dnode.headless=true", "-Dnode.bootstrap=" + tempFile.getAbsolutePath, "-Dnode.name=" + nodeName, "-jar", platformFile.getAbsolutePath))

        val logFile = System.getProperty("java.io.tmpdir") + File.separator + nodeName + ".log"
        outFile = new File(logFile + ".out")
        logger.debug("writing logs about {} on {}", nodeName, outFile.getAbsolutePath)
        new Thread(new ProcessStreamFileLogger(nodePlatformProcess.getInputStream, outFile)).start()
        errFile = new File(logFile + ".err")
        logger.debug("writing logs about {} on {}", nodeName, errFile.getAbsolutePath)
        new Thread(new ProcessStreamFileLogger(nodePlatformProcess.getErrorStream, errFile)).start()
        nodePlatformProcess.exitValue
        false
      } else {
        logger.error("Unable to start node because the platform jar file is not available")
        false
      }*/
    } catch {
      case e: IOException => {
        logger.error("Unexpected error while trying to start " + nodeName, e)
        false
      }
      case e: IllegalThreadStateException => {
        logger.debug("platform " + nodeName + " is started")
        true
      }
    }
  }

  def stopNode (): Boolean = {
    logger.debug("Kill " + nodeName)
    try {
      nodePlatformProcess.destroy()
      true
    }
    catch {
      case _@e => {
        logger.debug(nodeName + " cannot be killed. Try to force kill...")
        nodePlatformProcess.destroy()
        logger.debug(nodeName + " has been forcibly killed")
        true
      }
    }
  }

  /*
  def updateNode (model: String): Boolean = {
    val uuid = UUID.randomUUID()
    actor.manage(DeployResult(uuid.toString))
    nodePlatformProcess.getOutputStream.write(("sendModel " + model + " " + uuid.toString + "\n").getBytes)
    nodePlatformProcess.getOutputStream.flush()

    actor.waitFor()
  }*/

  private def getJava: String = {
    val java_home: String = System.getProperty("java.home")
    java_home + File.separator + "bin" + File.separator + "java"
  }

  private def startWithClassPath (jailBootStrapModel: ContainerRoot): Boolean = {
    try {
      val java: String = getJava
      val tempFile = File.createTempFile("bootModel" + nodeName, ".kev")
      KevoreeXmiHelper.save(tempFile.getAbsolutePath, jailBootStrapModel)

      // FIXME java memory properties must define as Node properties
      nodePlatformProcess = Runtime.getRuntime
        .exec(Array[String](java, "-Dnode.headless=true", "-Dnode.bootstrap=" + tempFile.getAbsolutePath, "-Dnode.name=" + nodeName, "-classpath", System.getProperty("java.class.path"),
                             "org.kevoree.platform.standalone.gui.App"))

      configureLogFile()

      nodePlatformProcess.exitValue
      false
    } catch {
      case e: IllegalThreadStateException => {
        logger.debug("platform " + nodeName + " is started")
        true
      }
    }
  }

  private def startWithAether (jailBootStrapModel: ContainerRoot): Boolean = {
    try {
      val platformFile = iaasNode.getBootStrapperService.resolveKevoreeArtifact("org.kevoree.platform.standalone", "org.kevoree.platform", KevoreeFactory.getVersion)
      val java: String = getJava
      if (platformFile != null) {

        val tempFile = File.createTempFile("bootModel" + nodeName, ".kev")
        KevoreeXmiHelper.save(tempFile.getAbsolutePath, jailBootStrapModel)

        // FIXME java memory properties must define as Node properties
        nodePlatformProcess = Runtime.getRuntime
          .exec(Array[String](java, "-Dnode.headless=true", "-Dnode.bootstrap=" + tempFile.getAbsolutePath, "-Dnode.name=" + nodeName, "-jar", platformFile.getAbsolutePath))
        configureLogFile()

        nodePlatformProcess.exitValue
        false
      } else {
        logger.error("Unable to start node because the platform jar file is not available")
        false
      }
    } catch {
      case e: IllegalThreadStateException => {
        logger.debug("platform " + nodeName + " is started")
        true
      }
    }
  }

  private def configureLogFile () {
    val logFile = System.getProperty("java.io.tmpdir") + File.separator + nodeName + ".log"
    outFile = new File(logFile + ".out")
    logger.debug("writing logs about {} on {}", nodeName, outFile.getAbsolutePath)
    new Thread(new ProcessStreamFileLogger(nodePlatformProcess.getInputStream, outFile)).start()
    errFile = new File(logFile + ".err")
    logger.debug("writing logs about {} on {}", nodeName, errFile.getAbsolutePath)
    new Thread(new ProcessStreamFileLogger(nodePlatformProcess.getErrorStream, errFile)).start()
  }
}

