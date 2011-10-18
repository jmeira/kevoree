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
package org.kevoree.tools.ui.editor.command

import java.net.URL
import org.kevoree.framework.KevoreeXmiHelper
import java.io.{File, BufferedReader, InputStreamReader, OutputStreamWriter}
import org.kevoree.tools.ui.editor.{PositionedEMFHelper, KevoreeUIKernel}
import org.slf4j.LoggerFactory
import org.kevoree.tools.aether.framework.AetherUtil

class MergeDefaultLibrary extends Command {

  var logger = LoggerFactory.getLogger(this.getClass)

  var kernel: KevoreeUIKernel = null

  def setKernel(k: KevoreeUIKernel) = kernel = k

  var snapshot : Boolean = false

  def setSnapshot(p : Boolean) = { snapshot = p }

  def execute(p: Object) {

    try {

      var file : File = null
      if(snapshot){
          AetherUtil.resolveMavenArtifact("org.kevoree.library.model.all","org.kevoree.library.model","1.4.0-SNAPSHOT",)
      } else {
         url = new URL("http://dist.kevoree.org/KevoreeLibraryStable.php");
      }

      // Get the response




      val newmodel = KevoreeXmiHelper.loadStream()

      if (newmodel != null) {
        kernel.getModelHandler().merge(newmodel);

        //CREATE TEMP FILE FROM ACTUAL MODEL
        val tempFile = File.createTempFile("kevoreeEditorTemp", ".kev");
        PositionedEMFHelper.updateModelUIMetaData(kernel);
        KevoreeXmiHelper.save(tempFile.getAbsolutePath, kernel.getModelHandler.getActualModel);

        //LOAD MODEL
        val loadCmd = new LoadModelCommand();
        loadCmd.setKernel(kernel);
        loadCmd.execute(tempFile.getAbsolutePath);


      } else {
        logger.error("Error while loading model");
      }


    } catch {

      case _@e => logger.error("Could not load default lib ! => "+e.getMessage ); e.printStackTrace()
    }


  }


}
