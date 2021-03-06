package org.kevoree.library.javase.basicGossiper.group;

import jexxus.common.Connection;
import jexxus.common.Delivery;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.Group;
import org.kevoree.KevoreeFactory;
import org.kevoree.annotation.*;
import org.kevoree.framework.KevoreePropertyHelper;
import org.kevoree.framework.NetworkHelper;
import org.kevoree.library.BasicGroup;
import org.kevoree.library.basicGossiper.protocol.gossip.Gossip;
import org.kevoree.library.basicGossiper.protocol.message.KevoreeMessage;
import org.kevoree.library.javase.basicGossiper.*;
import org.kevoree.library.javase.conflictSolver.AlreadyPassedPrioritySolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Erwan Daubert
 */
@Library(name = "JavaSE", names = "Android")
@GroupType
@DictionaryType({
        @DictionaryAttribute(name = "interval", defaultValue = "20000", optional = true)
})
public class BasicGossiperGroup extends BasicGroup implements GossiperComponent {

    protected DataManagerForGroup dataManager;
    protected GossiperPeriodic actor;
    protected GroupScorePeerSelector selector;
    private GossiperProcess processValue;
    protected Logger logger = LoggerFactory.getLogger(BasicGossiperGroup.class);
    private boolean starting;

    @Start
    public void startGossiperGroup() throws IOException {
        udp = true;
        Long timeoutLong = Long.parseLong((String) this.getDictionary().get("interval"));
        Serializer serializer = new GroupSerializer(this.getModelService());
        dataManager = new DataManagerForGroup(this.getName(), this.getNodeName(), this.getModelService(), new AlreadyPassedPrioritySolver(getKevScriptEngineFactory()));
        processValue = new GossiperProcess(this, dataManager, serializer, false);
        selector = new GroupScorePeerSelector(timeoutLong, this.currentCacheModel, this.getNodeName());
        logger.debug("{}: initialize GossiperActor", this.getName());
        actor = new GossiperPeriodic(this, timeoutLong, selector, processValue);
        processValue.start();
        actor.start();
        starting = true;
        super.startRestGroup();
    }

    protected void externalProcess(byte[] data, Connection from) {
        try {
            ByteArrayInputStream stin = new ByteArrayInputStream(data);
            stin.read();
            KevoreeMessage.Message msg = KevoreeMessage.Message.parseFrom(stin);
            logger.debug("Rec Some MSG {}" + msg.getContentClass() + "->" + msg.getDestName() + "->" + msg.getDestNodeName());
            processValue.receiveRequest(msg);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Stop
    public void stopGossiperGroup() {
        super.stopRestGroup();
        if (actor != null) {
            actor.stop();
            actor = null;
        }
        if (selector != null) {
            selector = null;
        }
        if (processValue != null) {
            processValue.stop();
            processValue = null;
        }
        if (dataManager != null) {
            dataManager = null;
        }
    }

    @Update
    public void updateGossiperGroup() throws IOException {
        logger.info("try to update configuration of {}", this.getName());
        stopGossiperGroup();
        startGossiperGroup();
    }

    protected AtomicReference<ContainerRoot> currentCacheModel = new AtomicReference<ContainerRoot>(KevoreeFactory.createContainerRoot());

    @Override
    public boolean afterLocalUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        currentCacheModel.set(proposedModel);
        return super.afterLocalUpdate(currentModel, proposedModel);
    }

    @Override
    public String getAddress(String remoteNodeName) {
        Option<String> ipOption = NetworkHelper.getAccessibleIP(KevoreePropertyHelper
                .getStringNetworkProperties(currentCacheModel.get(), remoteNodeName, org.kevoree.framework.Constants.KEVOREE_PLATFORM_REMOTE_NODE_IP()));
        if (ipOption.isDefined()) {
            return ipOption.get();
        } else {
            return "127.0.0.1";
        }
    }

    @Override
    public int parsePortNumber(String nodeName) {
        Option<Integer> portOption = KevoreePropertyHelper.getIntPropertyForGroup(currentCacheModel.get(), this.getName(), "port", true, nodeName);
        if (portOption.isDefined()) {
            return portOption.get();
        } else {
            return 8000;
        }
    }


    @Override
    public void localNotification(Object data) {
        // NO OP
    }


    private void notifyPeersInternal(List<String> l) {
        KevoreeMessage.Message.Builder messageBuilder = KevoreeMessage.Message.newBuilder().setDestName(getName()).setDestNodeName(getNodeName());
        messageBuilder.setContentClass(Gossip.UpdatedValueNotification.class.getName()).setContent(Gossip.UpdatedValueNotification.newBuilder().build().toByteString());
        for (String peer : l) {
            if (!peer.equals(getNodeName())) {
                String address = getAddress(peer);
                processValue.netSender().sendMessage/*Unreliable*/(messageBuilder.build(), new InetSocketAddress(address, parsePortNumber(peer)));
            }
        }
    }

    @Override
    protected void locaUpdateModel(final ContainerRoot modelOption) {
        new Thread() {
            public void run() {
                getModelService().atomicUpdateModel(modelOption);
            }
        }.start();
    }

    @Override
    public void triggerModelUpdate() {
        if (starting) {
            final ContainerRoot modelOption = org.kevoree.library.NodeNetworkHelper.updateModelWithNetworkProperty(this);
            if (modelOption != null) {
                getModelService().unregisterModelListener(this);
                getModelService().atomicUpdateModel(modelOption);
                getModelService().registerModelListener(this);
            }
            starting = false;
        }
        for (Object o : currentCacheModel.get().getGroupsForJ()) {
            Group g = (Group) o;
            if (g.getName().equals(this.getName())) {
                List<String> peers = new ArrayList<String>(g.getSubNodes().size());
                for (ContainerNode node : g.getSubNodesForJ()) {
                    peers.add(node.getName());
                }
                notifyPeersInternal(peers);
            }
        }
    }


}
