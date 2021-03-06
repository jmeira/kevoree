package org.kevoree.library.sky.libvirt;

import org.kevoree.annotation.*;
import org.kevoree.library.sky.api.KevoreeNodeRunner;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 15/09/11
 * Time: 16:26
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@Library(name = "SKY")
@NodeType
@DictionaryType({
        @DictionaryAttribute(name = "default_DISK", optional = false),
        @DictionaryAttribute(name = "default_COPY_MODE", vals = {"base", "clone", "as_is"}, optional = false),
        @DictionaryAttribute(name = "defaultdomain", defaultValue = "debian", optional = false)
})
public class LibVirtKvmNode extends LibVirtNode /*implements IaaSNodeWithDefaultValue*/ {
	private static final Logger logger = LoggerFactory.getLogger(LibVirtKvmNode.class);

	@Override
	public void startNode () {
		try {
			connection = new Connect("qemu:///system", false);
		} catch (LibvirtException e) {
			logger.error("Unable to find the hypervisor!", e);
		}
		super.startNode();
	}

	@Start

	@Override
	public KevoreeNodeRunner createKevoreeNodeRunner (String nodeName) {
		return new LibVirtKvmKevoreeNodeRunner(nodeName, this, connection);
	}
}
