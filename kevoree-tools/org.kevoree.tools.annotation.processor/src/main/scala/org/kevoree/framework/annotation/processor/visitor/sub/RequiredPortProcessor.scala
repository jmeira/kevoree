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

import org.kevoree.ComponentType
import org.kevoree.KevoreeFactory
import org.kevoree.framework.annotation.processor.LocalUtility
import org.kevoree.framework.annotation.processor.visitor.ServicePortTypeVisitor
import javax.lang.model.element.TypeElement
import javax.annotation.processing.ProcessingEnvironment
import javax.tools.Diagnostic.Kind
import org.kevoree.annotation.MessageTypes
import org.kevoree.tools.annotation.generator.ThreadingMapping

trait RequiredPortProcessor {
  def processRequiredPort(componentType: ComponentType, classdef: TypeElement, env: ProcessingEnvironment) = {

    //Collects all RequidedPort annotations and creates a list
    var requiredPortAnnotations: List[org.kevoree.annotation.RequiredPort] = List()

    val annotationRequired = classdef.getAnnotation(classOf[org.kevoree.annotation.RequiredPort])
    if (annotationRequired != null) {
      requiredPortAnnotations = requiredPortAnnotations ++ List(annotationRequired)
    }

    val annotationRequires = classdef.getAnnotation(classOf[org.kevoree.annotation.Requires])
    if (annotationRequires != null) {
      requiredPortAnnotations = requiredPortAnnotations ++ annotationRequires.value.toList
    }

    //For each annotation in the list
    requiredPortAnnotations.foreach {
      requiredPort =>


      //Check if a port with the same name exist in the component scope
        val portAll: List[org.kevoree.PortTypeRef] = componentType.getRequired.toList ++ componentType.getProvided.toList
        portAll.find(existingPort => existingPort.getName == requiredPort.name) match {

          case None => {
            val portTypeRef = KevoreeFactory.eINSTANCE.createPortTypeRef
            portTypeRef.setName(requiredPort.name)
            portTypeRef.setOptional(requiredPort.optional)
            /*
           we replace the annotation parameter "noDependency" by "needCheckDependency but we do not replace noDependency on the model
           noDependency = true (equivalent to needCheckDependency = false) means the required port is not use during critical start and stop operations of the container component
           and so we do not need to take it into account when we try to schedule starts and stops to avoid some deadlocks.
           noDependency = false (equivalent to needCheckDependency = true) means that the required port is used on start or stop operations of the container component
           and so we need to check dependency to avoid some deadlocks
            */
            portTypeRef.setNoDependency(!requiredPort.needCheckDependency())

            //sets the reference to the type of the port
            portTypeRef.setRef(LocalUtility.getOraddPortType(requiredPort.`type` match {
              case org.kevoree.annotation.PortType.SERVICE => {
                val visitor = new ServicePortTypeVisitor
                try {
                  requiredPort.className
                } catch {
                  case e: javax.lang.model.`type`.MirroredTypeException =>

                    //Checks the kind of the className attribute of the annotation
                    if (!e.getTypeMirror.toString.equals("java.lang.Void")) {
                      e.getTypeMirror.accept(visitor, e.getTypeMirror)
                    } else {
                      env.getMessager.printMessage(Kind.ERROR, "The className attribute of a Required ServicePort declaration is mandatory, and must be a Class or an Interface.\n"
                        + "Have a check on RequiredPort[name=" + requiredPort.name + "] of " + componentType.getBean + "\n"
                        + "TypeMirror of " + requiredPort.name + ", typeMirror : " + e.getTypeMirror + ",  qualifiedName : " + e.getTypeMirror + ", typeMirrorClass : " + e.getTypeMirror.getClass + "\n")
                    }

                }
                visitor.getDataType
              }
              case org.kevoree.annotation.PortType.MESSAGE => {
                val mpt = KevoreeFactory.eINSTANCE.createMessagePortType
                mpt.setName("org.kevoree.framework.MessagePort")

                if (requiredPort.messageType() != "untyped" ) {
                  mpt.setName(mpt.getName+System.currentTimeMillis()) //ensure uniqueness
                }

                if (requiredPort.messageType() != "untyped") {
                  if (classdef.getAnnotation(classOf[MessageTypes]) != null) {
                    classdef.getAnnotation(classOf[MessageTypes]).value().find(msgType => msgType.name() == requiredPort.messageType()) match {
                      case Some(foundMessageType) => {
                        val dicoType = KevoreeFactory.eINSTANCE.createDictionaryType
                        foundMessageType.elems().foreach {
                          elem =>
                            val dicAtt = KevoreeFactory.eINSTANCE.createDictionaryAttribute
                            dicAtt.setName(elem.name())
                          //  mpt.setName(mpt.getName+elem.name()) //WORKAROUND
                            try {
                              elem.className()
                            } catch {
                              case e: javax.lang.model.`type`.MirroredTypeException =>
                                dicAtt.setDatatype(e.getTypeMirror.toString)
                            }
                            dicAtt.setOptional(elem.optional())
                            dicoType.addAttributes(dicAtt)
                        }
                        mpt.setDictionaryType(Some(dicoType))
                      }
                      case None => env.getMessager.printMessage(Kind.ERROR, "Can't find message type for name " + requiredPort.messageType() + " for port " + requiredPort.name())
                    }
                  } else {
                    env.getMessager.printMessage(Kind.ERROR, "Can't find message type for name " + requiredPort.messageType() + " for port " + requiredPort.name())
                  }
                }
                mpt
              }
              case _ => null
            }))
            componentType.addRequired(portTypeRef)
          }
          case Some(e) => {
            env.getMessager.printMessage(Kind.ERROR, "Port name duplicated in " + componentType.getName + " Scope => " + requiredPort.name)
          }
        }
        ThreadingMapping.getMappings.put(Tuple2(componentType.getName,requiredPort.name),requiredPort.theadStrategy())

    }
  }
}
