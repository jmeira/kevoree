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

import org.kevoree.tools.marShell.ast.CreateDictionaryTypeStatment
import org.kevoree.tools.marShell.interpreter.{KevsInterpreterContext, KevsAbstractInterpreter}
import org.slf4j.LoggerFactory
import org.kevoree.KevoreeFactory

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 18/10/12
 * Time: 12:33
 */
case class KevsCreateDictionaryTypeInterpreter(stmt: CreateDictionaryTypeStatment) extends KevsAbstractInterpreter {

  val logger = LoggerFactory.getLogger(this.getClass)

  def interpret(context: KevsInterpreterContext) = {

    context.model.getTypeDefinitions.find(td => td.getName == stmt.typeName) match {
      case Some(td) => {
        if (td.getDictionaryType.isEmpty) {
          val newdictionary = KevoreeFactory.eINSTANCE.createDictionaryType
          td.setDictionaryType(Some(newdictionary))
        }

        stmt.elems.foreach {
          tdElem => {
            val processDictionaryAtt = td.getDictionaryType.get.getAttributes.find(eAtt => eAtt.getName == tdElem.id) match {
              case None => {
                val newAtt = KevoreeFactory.eINSTANCE.createDictionaryAttribute
                newAtt.setName(tdElem.id)
                td.getDictionaryType.get.addAttributes(newAtt)
                newAtt.setFragmentDependant(tdElem.fragDep)
                newAtt
              }
              case Some(att) => att
            }

            processDictionaryAtt.setFragmentDependant(tdElem.fragDep)
            processDictionaryAtt.setOptional(tdElem.optional)
            //processDictionaryAtt.setDatatype()

          }
        }
        true
      }
      case None => {
        logger.error("Type definition not found {}", stmt.typeName)
        false
      }
    }
  }

}
