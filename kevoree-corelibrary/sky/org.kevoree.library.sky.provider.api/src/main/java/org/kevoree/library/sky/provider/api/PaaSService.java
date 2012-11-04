package org.kevoree.library.sky.provider.api;

import org.kevoree.ContainerRoot;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 09/10/12
 * Time: 18:20
 *
 * @author Erwan Daubert
 * @version 1.0
 */
public interface PaaSService {

	public void add(ContainerRoot model) throws SubmissionException;

	public void remove(ContainerRoot model) throws SubmissionException;

	public void merge(ContainerRoot model) throws SubmissionException;

}
