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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.kevoree.framework.annotation.processor.visitor.sub

import org.kevoree.KevoreeFactory
import org.kevoree.TypeDefinition
import javax.lang.model.element.TypeElement


trait DictionaryProcessor {

  def processDictionary(typeDef: TypeDefinition, classdef: TypeElement) = {

    /* CHECK DICTIONARY */
    if (classdef.getAnnotation(classOf[org.kevoree.annotation.DictionaryType]) != null) {
      classdef.getAnnotation(classOf[org.kevoree.annotation.DictionaryType]).value.foreach {
        dictionaryAtt =>

        //CASE NO DICTIONARY
          if (typeDef.getDictionaryType.isEmpty) {
            val newdictionary = KevoreeFactory.eINSTANCE.createDictionaryType
            typeDef.setDictionaryType(Some(newdictionary))
          }

          //CASE NO ATT ALREADY CREATED WITH NAME
          val processDictionaryAtt = typeDef.getDictionaryType.get.getAttributes.find(eAtt => eAtt.getName == dictionaryAtt.name) match {
            case None => {
              val newAtt = KevoreeFactory.eINSTANCE.createDictionaryAttribute
              newAtt.setName(dictionaryAtt.name)
              typeDef.getDictionaryType.get.addAttributes(newAtt)
              newAtt.setFragmentDependant(dictionaryAtt.fragmentDependant());
              newAtt
            }
            case Some(att) => att
          }

          //INIT DEFAULT VALUE
          processDictionaryAtt.setOptional(dictionaryAtt.optional)

          //INIT DEF VALUE
          //TODO ALLOW MORE TYPE THAN STRING
          if (dictionaryAtt.defaultValue != "defaultKevoreeNonSetValue") {
            typeDef.getDictionaryType.get.getDefaultValues.find(defV => defV.getAttribute == processDictionaryAtt) match {
              case None => {
                val newVal = KevoreeFactory.eINSTANCE.createDictionaryValue
                newVal.setAttribute(processDictionaryAtt)
                newVal.setValue(dictionaryAtt.defaultValue)
                typeDef.getDictionaryType.get.addDefaultValues(newVal)
              }
              case Some(edefV) => edefV.setValue(dictionaryAtt.defaultValue.toString)
            }
          }
          if (!dictionaryAtt.vals().isEmpty) {
            processDictionaryAtt.setDatatype("enum=" + dictionaryAtt.vals().mkString(","))
          }

          var dataTypeName : String = ""
          try {
            dictionaryAtt.dataType()
          } catch {
            case e: javax.lang.model.`type`.MirroredTypeException =>
              dataTypeName = e.getTypeMirror.toString
          }

          if (dataTypeName != "java.lang.Void") {
            processDictionaryAtt.setDatatype("raw=" + dataTypeName)
          }
          processDictionaryAtt
      }
    }
  }

}
