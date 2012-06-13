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
package org.kevoree.tools.marShell.ast


object AstHelper {

  def createBlockFromStatement(s : Statment) : Block = {
    TransactionalBloc(List(s))
  }

  def toStringDictionary(dico : java.util.Properties) : String = {
    val buffer = new StringBuffer()
    buffer.append("{")
    import scala.collection.JavaConversions._
    var isFirst = true
    dico.keys().foreach(k => {
      if (!isFirst){
        buffer.append(",")
      }
      buffer.append(k+"='"+dico.get(k)+"'")
      isFirst = false
    })
    buffer.append("}")
    buffer.toString
  }

}