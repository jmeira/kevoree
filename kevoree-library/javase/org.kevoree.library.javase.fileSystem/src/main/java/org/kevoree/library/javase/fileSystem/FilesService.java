package org.kevoree.library.javase.fileSystem;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 22/11/11
 * Time: 20:03
 * To change this template use File | Settings | File Templates.
 */
public interface FilesService {

    public Set<String> getFilesPath();

    public byte[] getFileContent(String relativePath);
	
	public String getAbsolutePath(String relativePath);

}