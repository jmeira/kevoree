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
package org.kevoree.tools.aether.framework

import java.io.File
import java.util.zip.ZipFile
import org.slf4j.LoggerFactory
import org.sonatype.aether.RepositorySystemSession

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 27/04/12
 * Time: 17:16
 */

trait CorruptedFileChecker {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def clearRepoCacheFile(repoSession : RepositorySystemSession,arteFact : org.sonatype.aether.artifact.Artifact){
    val fileM2REPO = repoSession.getLocalRepository.getBasedir
    val cacheFile = new File(fileM2REPO.getAbsolutePath+File.separator+arteFact.getGroupId.replace(".",File.separator)+File.separator+arteFact.getArtifactId+File.separator+arteFact.getVersion+File.separator+"_maven.repositories")
    if (cacheFile.exists()){
      cacheFile.delete()
      logger.info("Delete Maven Repository potentially corrupted cache to force resolution")
    }
  }


  def checkFile(f: File): Boolean = {
    if (f.exists()) {
      if (f.getName.endsWith(".zip") || f.getName.endsWith(".jar") || f.getName.endsWith("war")) {
        try {
          new ZipFile(f)
          true
        } catch {
          case _@e => {
            try {
              f.delete()
            } catch {
              case _ => //NOTHING
            }
            logger.error("Corrupted File has ben deleted, not a zip file {}",f)
            false
          }
        }
      } else {
        true
      }
    } else {
      logger.error("Corrupted File , does not exist {}",f)
      false
    }
  }


}
