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
package org.kevoree.tools.marShell.interpreter.sub

import org.kevoree.tools.marShell.ast.{AddRepoStatment, MergeStatement}
import org.kevoree.tools.marShell.interpreter.{KevsInterpreterContext, KevsAbstractInterpreter}
import org.kevoree.merger.KevoreeMergerComponent
import org.slf4j.LoggerFactory
import org.kevoree.framework.KevoreeXmiHelper
import java.util.jar.{JarEntry, JarFile}
import java.io.File
import java.net.URL
import org.kevoree.KevoreeFactory

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 12/12/11
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */

case class KevsAddRepoInterpreter (addRepo: AddRepoStatment) extends KevsAbstractInterpreter {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def interpret (context: KevsInterpreterContext): Boolean = {
    if (addRepo.url == ""){
      logger.error("Empty Repository URL not allowed")
      false
    } else {
      context.model.getRepositories.find(r => r.getUrl == addRepo.url) match {
        case Some(_)=>
        case None => {
          val repo = KevoreeFactory.eINSTANCE.createRepository
          repo.setUrl(addRepo.url)
          context.model.addRepositories(repo)
        }
      }
      true
    }
  }

}