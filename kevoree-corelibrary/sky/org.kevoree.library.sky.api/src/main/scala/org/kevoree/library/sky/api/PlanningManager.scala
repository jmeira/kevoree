package org.kevoree.library.sky.api

import command.{AddNodeCommand, RemoveNodeCommand}
import nodeType.{HostNode, AbstractHostNode, AbstractIaaSNode, IaaSNode}
import org.kevoreeAdaptation.{AdaptationPrimitive, ParallelStep, KevoreeAdaptationFactory, AdaptationModel}
import org.slf4j.{LoggerFactory, Logger}
import org.kevoree._
import api.PrimitiveCommand


/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 13/12/11
 * Time: 09:19
 *
 * @author Erwan Daubert
 * @version 1.0
 */

object PlanningManager {
  private val logger: Logger = LoggerFactory.getLogger(PlanningManager.getClass)

  def kompare(current: ContainerRoot, target: ContainerRoot, skyNode: AbstractHostNode): AdaptationModel = {
    logger.debug("starting kompare...")
    val adaptationModel: AdaptationModel = KevoreeAdaptationFactory.eINSTANCE.createAdaptationModel
    var step: ParallelStep = KevoreeAdaptationFactory.eINSTANCE.createParallelStep
    adaptationModel.setOrderedPrimitiveSet(new Some[ParallelStep](step))
    if (skyNode.isHost) {
      var removeNodeType: AdaptationPrimitiveType = null
      var addNodeType: AdaptationPrimitiveType = null
      current.getAdaptationPrimitiveTypes.foreach {
        primitiveType =>
        //        for (primitiveType <- current.getAdaptationPrimitiveTypesForJ) {
          if (primitiveType.getName == HostNode.REMOVE_NODE) {
            removeNodeType = primitiveType
          }
          else if (primitiveType.getName == HostNode.ADD_NODE) {
            addNodeType = primitiveType
          }
      }
      if (removeNodeType == null || addNodeType == null) {
        target.getAdaptationPrimitiveTypes.foreach {
          primitiveType =>
          //          for (primitiveType <- target.getAdaptationPrimitiveTypesForJ) {
            if (primitiveType.getName == HostNode.REMOVE_NODE) {
              removeNodeType = primitiveType
            }
            else if (primitiveType.getName == HostNode.ADD_NODE) {
              addNodeType = primitiveType
            }
        }
      }
      if (removeNodeType == null) {
        logger.warn("there is no adaptation primitive for " + HostNode.REMOVE_NODE)
      }
      if (addNodeType == null) {
        logger.warn("there is no adaptation primitive for " + HostNode.ADD_NODE)
      }
      //        for (node <- current.getNodesForJ) {
      current.getNodes.find(node => node.getName == skyNode.getNodeName) match {
        case Some(node) => {
          target.getNodes.find(node1 => node1.getName == skyNode.getNodeName) match {
            case Some(node1) => {
              node.getHosts.foreach {
                subNode =>
                  node1.getHosts.find(subNode1 => subNode.getName == subNode1.getName) match {
                    case None => {
                      logger.debug("add a " + HostNode.REMOVE_NODE + " adaptation primitive with " + subNode.getName +
                        " as parameter")
                      val command: AdaptationPrimitive = KevoreeAdaptationFactory.eINSTANCE.createAdaptationPrimitive
                      command.setPrimitiveType(removeNodeType)
                      command.setRef(subNode)
                      val subStep: ParallelStep = KevoreeAdaptationFactory.eINSTANCE.createParallelStep
                      subStep.addAdaptations(command)
                      adaptationModel.addAdaptations(command)
                      step.setNextStep(new Some[ParallelStep](subStep))
                      step = subStep
                    }
                    case Some(subNode1) =>
                  }
              }
            }
            case None =>
          }
        }
        case None =>
      }

      target.getNodes.find(node => node.getName == skyNode.getNodeName) match {
        case Some(node) => {
          current.getNodes.find(node1 => node1.getName == skyNode.getNodeName) match {
            case Some(node1) => {
              node.getHosts.foreach {
                subNode =>
                  node1.getHosts.find(subNode1 => subNode.getName == subNode1.getName) match {
                    case None => {
                      logger.debug("add a " + HostNode.ADD_NODE + " adaptation primitive with " + subNode.getName +
                        " as parameter")
                      val command: AdaptationPrimitive = KevoreeAdaptationFactory.eINSTANCE.createAdaptationPrimitive
                      command.setPrimitiveType(addNodeType)
                      command.setRef(subNode)
                      val subStep: ParallelStep = KevoreeAdaptationFactory.eINSTANCE.createParallelStep
                      subStep.addAdaptations(command)
                      adaptationModel.addAdaptations(command)
                      step.setNextStep(new Some[ParallelStep](subStep))
                      step = subStep
                    }
                    case Some(subNode1) =>
                  }
              }
            }
            case None =>
          }
        }
        case None =>
      }
    }
    val superModel: AdaptationModel = skyNode.superKompare(current, target)
    if (!skyNode.isContainer && isContaining(superModel.getOrderedPrimitiveSet)) {
      throw new Exception("This node is not a container (see \"role\" attribute)")
    }
    adaptationModel.addAllAdaptations(superModel.getAdaptations)
    step.setNextStep(superModel.getOrderedPrimitiveSet)
    logger.debug("Kompare model contain " + adaptationModel.getAdaptations.size + " primitives")

    adaptationModel
  }

  private def isContaining(step: Option[ParallelStep]): Boolean = {
    if (step.isDefined) {
      step.get.getAdaptations.forall(adaptation => adaptation.getPrimitiveType.getName == "UpdateType" || adaptation.getPrimitiveType.getName == "UpdateDeployUnit" ||
        adaptation.getPrimitiveType.getName == "AddType" || adaptation.getPrimitiveType.getName == "AddDeployUnit" || adaptation.getPrimitiveType.getName == "AddThirdParty" ||
        adaptation.getPrimitiveType.getName == "RemoveType" || adaptation.getPrimitiveType.getName == "RemoveDeployUnit" || adaptation.getPrimitiveType.getName == "UpdateDictionaryInstance" ||
        adaptation.getPrimitiveType.getName == "StartThirdParty" || (adaptation.getPrimitiveType.getName == "AddInstance" && adaptation.getRef.isInstanceOf[Group]) ||
        (adaptation.getPrimitiveType.getName == "UpdateInstance" && adaptation.getRef.isInstanceOf[Group]) ||
        (adaptation.getPrimitiveType.getName == "RemoveInstance" && adaptation.getRef.isInstanceOf[Group]) ||
        (adaptation.getPrimitiveType.getName == "StartInstance" && adaptation.getRef.isInstanceOf[Group]) ||
        (adaptation.getPrimitiveType.getName == "StopInstance" && adaptation.getRef.isInstanceOf[Group])) && isContaining(step.get.getNextStep)
    } else {
      false
    }
  }

  def getPrimitive(adaptationPrimitive: AdaptationPrimitive, skyNode: AbstractHostNode): PrimitiveCommand = {
    logger.debug("ask for primitiveCommand corresponding to " + adaptationPrimitive.getPrimitiveType.getName)
    var command: PrimitiveCommand = null
    if (adaptationPrimitive.getPrimitiveType.getName == HostNode.REMOVE_NODE) {
      logger.debug("add REMOVE_NODE command on " + (adaptationPrimitive.getRef.asInstanceOf[ContainerNode]).getName)

      val targetNode = adaptationPrimitive.getRef.asInstanceOf[ContainerNode]
      val targetNodeRoot = adaptationPrimitive.getRef.asInstanceOf[ContainerNode].eContainer.asInstanceOf[ContainerRoot]
      command = new RemoveNodeCommand(targetNodeRoot,targetNode.getName,skyNode)
    }
    else if (adaptationPrimitive.getPrimitiveType.getName == HostNode.ADD_NODE) {
      logger.debug("add ADD_NODE command on " + (adaptationPrimitive.getRef.asInstanceOf[ContainerNode]).getName)

      val targetNode = adaptationPrimitive.getRef.asInstanceOf[ContainerNode]
      val targetNodeRoot = adaptationPrimitive.getRef.asInstanceOf[ContainerNode].eContainer.asInstanceOf[ContainerRoot]
      command = new AddNodeCommand(targetNodeRoot,targetNode.getName,skyNode)
    }
    if (command == null) {
      command = skyNode.superGetPrimitive(adaptationPrimitive)
    }
    command
  }
}