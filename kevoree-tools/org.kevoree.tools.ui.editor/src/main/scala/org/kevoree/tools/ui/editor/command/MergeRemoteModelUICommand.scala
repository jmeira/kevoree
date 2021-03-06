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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.kevoree.tools.ui.editor.command

import java.net.URL
import javax.swing.JOptionPane
import org.kevoree.framework.KevoreeXmiHelper
import org.kevoree.tools.ui.editor.{PositionedEMFHelper, KevoreeUIKernel}
import org.slf4j.LoggerFactory
import java.util.concurrent.{TimeUnit, Exchanger}
import org.kevoree.ContainerRoot
import jexxus.client.ClientConnection
import jexxus.common.{Delivery, Connection, ConnectionListener}
import java.io.ByteArrayInputStream
import jexxus.server.ServerConnection

object MergeRemoteModelUICommand {
  var lastRemoteNodeAddress: String = "localhost:8000"
}

class MergeRemoteModelUICommand extends Command {

  var kernel: KevoreeUIKernel = null

  def setKernel(k: KevoreeUIKernel) = kernel = k

  private val lcommand = new MergeModelCommand();

  var logger = LoggerFactory.getLogger(this.getClass)

  def tryRemoteLoad(ip: String, port: String, zip: Boolean): Boolean = {
    try {
      //CALL POST REMOTE URL
      var loadedModel : ContainerRoot = null
      if (zip) {
        val url = new URL("http://" + ip + ":" + port + "/model/current/zip");
        val conn = url.openConnection()
        conn.setConnectTimeout(2000)
        val inputStream = conn.getInputStream
        loadedModel = KevoreeXmiHelper.loadCompressedStream(inputStream)
        logger.debug("Load model from zip stream")
      } else {
        val url = new URL("http://" + ip + ":" + port + "/model/current");
        val conn = url.openConnection()
        conn.setConnectTimeout(2000)
        val inputStream = conn.getInputStream
        loadedModel = KevoreeXmiHelper.loadStream(inputStream)
        logger.debug("Load model from xml stream")
      }
      PositionedEMFHelper.updateModelUIMetaData(kernel)
      lcommand.setKernel(kernel)
      lcommand.execute(loadedModel)
      true
    } catch {
      case _@e => {
        logger.debug("Pull failed to "+ip+":"+port+" , zip activated="+zip)
        false
      }
    }
  }


  def tryRemoteJexxus(ip: String, port: String) : Boolean = {
    try {
    val exchanger = new Exchanger[ContainerRoot]();
    var conns :Tuple1[ClientConnection] = null
    conns = Tuple1(new ClientConnection(new ConnectionListener() {
      def connectionBroken(broken: Connection, forced: Boolean) {
        exchanger.exchange(null)
      }
      def receive(data: Array[Byte], from: Connection) {
        val inputStream = new ByteArrayInputStream(data)
        val root = KevoreeXmiHelper.loadCompressedStream(inputStream)
        try {
          exchanger.exchange(root);
        } catch {
          case _ @ e => //logger.error("",e)
        } finally {
          conns._1.close()
        }
      }
      def clientConnected(conn: ServerConnection) {}
    }, ip, Integer.parseInt(port), false))
    conns._1.connect(5000)
    conns._1.send(Array(Byte.box(0)),Delivery.RELIABLE)
    val root = exchanger.exchange(null,5000,TimeUnit.MILLISECONDS)
    //kernel.getModelHandler.merge(root)
    PositionedEMFHelper.updateModelUIMetaData(kernel)
    lcommand.setKernel(kernel)
    lcommand.execute(root)
    true
    } catch {
      case _@e => {
        logger.debug("Pull failed to "+ip+":"+port)
        false
      }
    }
  }


  def execute(p: Object) = {

    try {
      val result = JOptionPane.showInputDialog("Remote target node <ip:port>", MergeRemoteModelUICommand.lastRemoteNodeAddress)
      if (result != null && result != "") {
        MergeRemoteModelUICommand.lastRemoteNodeAddress = result
        val results = result.split(":").toList
        if (results.size >= 2) {
          val ip = results(0)
          val port = results(1)
          if(!tryRemoteJexxus(ip, port)){
            if (!tryRemoteLoad(ip, port, true)) {
              if (!tryRemoteLoad(ip, port, false)) {
                logger.error("Can't load model from node")
              }
            }
          }
        }
        true
      }
    } catch {
      case _@e => {
        logger.error("Bad Input , ip@port needed")
        false
      }
    }


  }

}
