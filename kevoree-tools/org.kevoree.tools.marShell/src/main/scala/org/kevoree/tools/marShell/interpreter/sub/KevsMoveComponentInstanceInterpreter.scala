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

import org.kevoree.tools.marShell.interpreter.{KevsInterpreterContext, KevsAbstractInterpreter}
import org.kevoree.tools.marShell.ast.MoveComponentInstanceStatment

import org.slf4j.LoggerFactory
import org.kevoree._
import tools.marShell.ast.MoveComponentInstanceStatment
import tools.marShell.interpreter.KevsInterpreterContext
import scala.Some

case class KevsMoveComponentInstanceInterpreter (moveComponent: MoveComponentInstanceStatment) extends KevsAbstractInterpreter {
	var logger = LoggerFactory.getLogger(this.getClass)

	def interpret (context: KevsInterpreterContext): Boolean = {

		moveComponent.cid.nodeName match {
			case Some(nodeID) => {
				context.model.getNodes.find(node => node.getName == nodeID) match {
					case Some(sourceNode) => {
						//SEARCH COMPONENT
						sourceNode.getComponents.find(c => c.getName == moveComponent.cid.componentInstanceName) match {
							case Some(targetComponent) => {
								context.model.getNodes.find(node => node.getName == moveComponent.targetNodeName) match {
									case Some(targetNode) => {

										// look at all ports to get all channels and check attributes that are fragment dependent:
										// look at the fragment for sourceNode
										// if another component is bound to the channel we do nothing
										// else we need to remove the fragment
										// look at the fragment for targetNode
										// if another component is already bound to the channel we do nothing
										// else we need to copy the previous fragment
										var fragments = List[(Channel, Port, DictionaryValue)]()
										(targetComponent.getProvided ++ targetComponent.getRequired).foreach(p => p.getBindings.foreach(mb => {
											if (mb.getHub.getDictionary.isDefined) {
												mb.getHub.getDictionary.get.getValues.foreach(dv => {
													if (dv.getAttribute.getFragmentDependant && dv.getTargetNode.isDefined && dv.getTargetNode.get.getName == nodeID) {
														fragments = fragments ++ List[(Channel, Port, DictionaryValue)]((mb.getHub, mb.getPort, dv))
													}
												})
											}
										}))
										if (fragments.size > 0) {
											fragments.foreach {
												tuple =>
													val sourceFragmentMustBeRemoved = !tuple._1.getBindings.exists(mb =>
														mb.getPort.eContainer.eContainer.asInstanceOf[ContainerNode].getName == tuple._2.eContainer.eContainer.asInstanceOf[ContainerNode].getName &&
																mb.getPort.eContainer.asInstanceOf[ComponentInstance].getName != tuple._2.eContainer.asInstanceOf[ComponentInstance].getName)

													val targetFragmentMustBeAdded = !tuple._1.getDictionary.get.getValues
														.exists(odv => odv.getTargetNode.isDefined && odv.getTargetNode.get.getName == moveComponent.targetNodeName && odv.getAttribute.getName == tuple._3.getAttribute.getName)

//													println("sourceFragmentMustBeRemoved && targetFragmentMustBeAdded: " + sourceFragmentMustBeRemoved + "&&" + targetFragmentMustBeAdded)
													if (sourceFragmentMustBeRemoved && targetFragmentMustBeAdded) {
														tuple._3.setTargetNode(Some(targetNode))
													} else if (targetFragmentMustBeAdded) {
														val value = KevoreeFactory.createDictionaryValue
														value.setAttribute(tuple._3.getAttribute)
														value.setTargetNode(Some(targetNode))
														value.setValue(tuple._3.getValue)
														tuple._1.getDictionary.get.addValues(value)
													} else if (sourceFragmentMustBeRemoved) {
														tuple._1.getDictionary.get.removeValues(tuple._3)
													}
											}
										}
										sourceNode.removeComponents(targetComponent)
										targetNode.addComponents(targetComponent)
										true
									}
									case None => {
										logger.error("Target node not found " + moveComponent.cid.componentInstanceName)
										false
									}
								}
							}
							case None => {
								logger.error("Component not found " + moveComponent.cid.componentInstanceName)
								false
							}
						}
					}
					case None => {
						logger.error("Source Node not found " + nodeID)
						false
					}
				}
			}
			case None => false //TODO solve ambiguity
		}

	}
}