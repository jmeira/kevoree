/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.kevoree.library.frascatiNodeTypes;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.stp.sca.ScaPackage;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.Library;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.library.defaultNodeTypes.JavaSENode;
import org.kevoreeAdaptation.AdaptationModel;
import org.kevoreeAdaptation.AdaptationPrimitive;
import org.ow2.frascati.FraSCAti;
import org.ow2.frascati.util.FrascatiClassLoader;
import org.ow2.frascati.util.FrascatiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author obarais
 */
@Library(name = "Frascati")
@NodeType
public class FrascatiNode extends JavaSENode {
	private static final Logger logger = LoggerFactory
			.getLogger(FrascatiNode.class);

	FraSCAti frascati;
	Thread t;
	Thread current;

	@Start
	@Override
	public void startNode() {
		super.startNode();
		current = Thread.currentThread();

		if (t == null) {

			t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						System.out.println("test");
						System.out.println(FraSCAti.class.getClassLoader());

						System.err.println("toto "
								+ EPackage.Registry.INSTANCE
										.getEPackage(ScaPackage.eNS_URI));
						System.err.println("titi" + EPackage.class.hashCode());
						System.err.println("titi" + EPackage.Registry.class.hashCode());

						Thread.currentThread().setContextClassLoader(
								FraSCAti.class.getClassLoader());
						System.err.println("toto "
								+ EPackage.Registry.INSTANCE
										.getEPackage(ScaPackage.eNS_URI));
						;

						System.err.println("titi" + EPackage.class.hashCode());
						System.err.println("titi" + EPackage.Registry.class.hashCode());

						
						frascati = FraSCAti.newFraSCAti();

						System.err.println("toto "
								+ EPackage.Registry.INSTANCE
										.getEPackage(ScaPackage.eNS_URI));

						System.err.println("titi" + EPackage.class.hashCode());
						System.err.println("titi" + EPackage.Registry.class.hashCode());
						System.err.println("titi "
								+ EPackage.Registry.INSTANCE
										);

						org.ow2.frascati.util.FrascatiClassLoader f = new FrascatiClassLoader(
								FraSCAti.class.getClassLoader());
						frascati.setClassLoader(f);
						System.err.println("toto "
								+ EPackage.Registry.INSTANCE
										.getEPackage(ScaPackage.eNS_URI));

						System.err.println("titi" + EPackage.class.hashCode());
						System.err.println("titi" + EPackage.Registry.class.hashCode());
						System.err.println("titi "
								+ EPackage.Registry.INSTANCE
										);
						
						System.err.println(EPackage.Registry.INSTANCE.keySet().size());
						for (String s : EPackage.Registry.INSTANCE.keySet()){
							System.err.println(s);
							
						}
						
					} catch (FrascatiException e) {
						e.printStackTrace();
					}

				}
			});
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Stop
	@Override
	public void stopNode() {
		super.stopNode();
		try {
			System.err.println("STOP NODE FRASCATI");
			frascati.close(frascati.getComposite("org.ow2.frascati.FraSCAti"));
			frascati = null;
			t.interrupt();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public AdaptationModel kompare(ContainerRoot current, ContainerRoot target) {

		return super.kompare(current, target);

	}

	public org.kevoree.api.PrimitiveCommand getSuperPrimitive(
			AdaptationPrimitive adaptationPrimitive) {
		return super.getPrimitive(adaptationPrimitive);

	}

	@Override
	public org.kevoree.api.PrimitiveCommand getPrimitive(
			AdaptationPrimitive adaptationPrimitive) {
		org.kevoree.library.frascatiNodeTypes.primitives.AdaptatationPrimitiveFactory
				.setFrascati(frascati);
		return org.kevoree.library.frascatiNodeTypes.primitives.AdaptatationPrimitiveFactory
				.getPrimitive(adaptationPrimitive, this, this,
					(org.kevoree.extra.jcl.KevoreeJarClassLoader)FraSCAti.class.getClassLoader());

	}

}
