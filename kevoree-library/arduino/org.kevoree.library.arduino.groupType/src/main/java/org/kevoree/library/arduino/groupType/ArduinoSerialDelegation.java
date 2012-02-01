package org.kevoree.library.arduino.groupType;

import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.service.core.handler.KevoreeModelHandlerService;
import org.kevoree.framework.AbstractGroupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: ffouquet
 * Date: 10/08/11
 * Time: 11:07
 */
@Library(name = "Arduino")
@GroupType
@DictionaryType({
        @DictionaryAttribute(name = "serialport", fragmentDependant = true)
})
public class ArduinoSerialDelegation extends AbstractGroupType {

    protected KevoreeModelHandlerService modelHandlerService = null;
    protected Logger logger = LoggerFactory.getLogger(ArduinoSerialDelegation.class);

    ArduinoDelegationPush delegationPush = null;

    private boolean isStarted = false;

    @Start
    public void startGroupDelegation() {
        delegationPush = new ArduinoDelegationPush(modelHandlerService, this.getName());
        //triggerModelUpdate();
    }

    @Stop
    public void stopGroupDelegation() {
        if (modelHandlerService != null) {
            modelHandlerService = null;
        }
    }

    @Override
    public void triggerModelUpdate() {
        new Thread() {
            @Override
            public void run() {
                delegationPush.setModel(null);//INVALIDATE MODEl
                delegationPush.deployAll();
            }
        }.start();
    }

    @Override
    public void push(ContainerRoot model, String targetNodeName) {
        if (!isStarted) {
            startGroupDelegation();
        }
        delegationPush.setModel(model);
        delegationPush.deployNode(targetNodeName);
    }

    @Override
    public ContainerRoot pull(String targetNodeName) {
        return null;  //TODO
    }
}
