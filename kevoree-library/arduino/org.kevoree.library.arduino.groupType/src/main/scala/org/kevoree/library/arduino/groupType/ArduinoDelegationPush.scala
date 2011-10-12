package org.kevoree.library.arduino.groupType

import org.kevoree.api.service.core.handler.KevoreeModelHandlerService
import org.slf4j.{Logger, LoggerFactory}
import org.kevoree.tools.aether.framework.NodeTypeBootstrapHelper
import org.osgi.framework.Bundle
import reflect.BeanProperty
import org.kevoree.{KevoreeFactory, ContainerRoot}

/**
 * User: ffouquet
 * Date: 10/08/11
 * Time: 13:55
 */

class ArduinoDelegationPush(handler: KevoreeModelHandlerService, groupName: String, bundle: Bundle) {
  protected var logger: Logger = LoggerFactory.getLogger(this.getClass)
  @BeanProperty
  var model: ContainerRoot = _

  def deployAll() {

    if (model == null) {
      model = handler.getLastModel
    }
    model.getGroups.find(g => g.getName == groupName) match {
      case Some(group) => {
        group.getSubNodes.filter(sub => sub.getTypeDefinition.getName.toLowerCase.contains("arduino")).foreach {
          subNode =>
            deployNode(subNode.getName)
        }
      }
      case None => logger.warn("Group Not found")
    }
  }

  def deployNode(targetNodeName: String) {
    if (model == null) {
      model = handler.getLastModel
    }
    try {
      val nodeTypeHelper = new NodeTypeBootstrapHelper
      val nodeType = nodeTypeHelper.bootstrapNodeType(model, targetNodeName, bundle.getBundleContext)
      nodeType match {
        case Some(gNodeType) => {
          model.getGroups.find(g => g.getName == groupName) match {
            case Some(group) => {
              val dictionary = group.getDictionary.getOrElse({
                val newdic = KevoreeFactory.createDictionary; group.setDictionary(Some(newdic)); newdic
              })
              val att = dictionary.getValues.find(value => value.getAttribute.getName == "serialport" && value.getTargetNode == targetNodeName).getOrElse(null)
              gNodeType.getClass.getMethods.find(method => method.getName == "push") match {
                case Some(method) => {
                  val port = if (att != null) {
                    att.getValue
                  } else {
                    ""
                  }
                  method.invoke(gNodeType, targetNodeName, model, port)
                }
                case None => logger.error("No push method in group for name " + groupName)
              }


            }
            case None => logger.error("Group not found for name " + groupName)
          }
        }
        case None => logger.warn("Can't bootstrap NodeType")
      }
    } catch {
      case _@e => logger.error("Can deploy model to node " + targetNodeName, e)
    }
  }


}