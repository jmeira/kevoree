package org.kevoree.library.javase.fileSystemGit;

import com.sun.xml.internal.rngom.ast.builder.BuildException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.kevoree.annotation.*;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.library.javase.fileSystem.LockFilesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 12/12/11
 * Time: 22:39
 * To change this template use File | Settings | File Templates.
 */
@Library(name = "JavaSE")
@Provides({
        @ProvidedPort(name = "files", type = PortType.SERVICE, className = LockFilesService.class)
})
@MessageTypes({
        @MessageType(name = "saveFile", elems = {@MsgElem(name = "path", className = String.class), @MsgElem(name = "data", className = Byte[].class)})
})
@DictionaryType({
        @DictionaryAttribute(name = "url", optional = false),
        @DictionaryAttribute(name = "login", optional = false),
        @DictionaryAttribute(name = "pass", optional = false)
})
@ComponentType
public class GitFileSystem extends AbstractComponentType implements LockFilesService {

    private File baseClone = null;
    private Logger logger = LoggerFactory.getLogger(GitFileSystem.class);

    @Start
    public void start() {
        try {
            baseClone = File.createTempFile("kevoreessh", "temp");
            baseClone.delete();
            baseClone.mkdir();
            CloneCommand clone = Git.cloneRepository();
            clone.setURI(this.getDictionary().get("url").toString()).setDirectory(baseClone).setBranch(org.eclipse.jgit.lib.Constants.HEAD);
            clone.call();

        } catch (Exception e) {
            logger.debug("Could not clone repository: ", e);
            throw new BuildException(e);
        }
    }

    @Stop
    public void stop() {
        //DELETE FILE
    }

    @Update
    public void update() {
        stop();
        start();
    }

    private Set<String> getFlatFiles(File base, String relativePath, boolean root, Set<String> extensions) {
        Set<String> files = new HashSet<String>();
        if (base.exists() && !base.getName().startsWith(".")) {
            if (base.isDirectory()) {
                File[] childs = base.listFiles();
                for (int i = 0; i < childs.length; i++) {
                    if (root) {
                        files.addAll(getFlatFiles(childs[i], relativePath, false, extensions));
                    } else {
                        files.addAll(getFlatFiles(childs[i], relativePath + "/" + base.getName(), false, extensions));
                    }
                }
            } else {

                boolean filtered = false;
                if (extensions != null) {
                    filtered = true;
                    logger.debug("Look for extension for " + base.getName());
                    for (String filter : extensions) {
                        if (base.getName().endsWith(filter)) {
                            filtered = false;
                        }
                    }
                }
                if (!root && !filtered) {
                    files.add(relativePath + "/" + base.getName());
                }
            }
        }
        return files;
    }


    @Port(name = "files", method = "getFilesPath")
    public Set<String> getFilesPath() {
        return getFlatFiles(baseClone, "", true, null);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Port(name = "files", method = "getFilteredFilesPath")
    public Set<String> getFilteredFilesPath(Set<String> extensions) {
        return getFlatFiles(baseClone, "", true, extensions);
    }


    private long lastRevisionCheck = -1;

    @Override
    public byte[] getFileContent(String relativePath, Boolean lock) {

        //UPDATE PHASE
        try {
            long newRevision = repository.getLatestRevision();
            if (newRevision != lastRevisionCheck) {
                File f = new File(baseClone.getAbsolutePath() + relativePath);
                if (f.exists()) {
                    lastRevisionCheck = client.doUpdate(f, SVNRevision.HEAD, false, false);
                }
            }
        } catch (SVNException e) {
            logger.error("Error while getRevision");
        }
        if (lock) {
            final String[] relatvePathClean = {relativePath};
            if (relativePath.startsWith("/")) {
                relatvePathClean[0] = relativePath.substring(relativePath.indexOf("/") + 1);
            }
            if (!lockedFile.contains(relatvePathClean)) {
                lockedFile.add(relatvePathClean[0]);
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        Map<String, Long> locks = new HashMap<String, Long>();
                        locks.put(relatvePathClean[0], lastRevisionCheck);
                        try {
                            repository.lock(locks, "AutoLock Kevoree Editor", false, null);
                            lockedFile.add(relatvePathClean[0]);
                        } catch (SVNException e) {
                            logger.error("Error while acquire lock ", e);
                        }
                    }
                };
                t.start();
            }

        }
        return getFileContent(relativePath);
    }

    @Port(name = "files", method = "getFileContent")
    public byte[] getFileContent(String relativePath) {
        File f = new File(baseClone.getAbsolutePath() + relativePath);
        if (f.exists()) {
            try {
                FileInputStream fs = new FileInputStream(f);
                byte[] result = convertStream(fs);
                fs.close();

                return result;
            } catch (Exception e) {
                logger.error("Error while getting file ", e);
            }
        } else {
            logger.debug("No file exist = {}", baseClone.getAbsolutePath() + relativePath);
            return new byte[0];
        }
        return new byte[0];
    }

    @Port(name = "files", method = "getAbsolutePath")
    public String getAbsolutePath(String relativePath) {
        if (new File(baseClone.getAbsolutePath() + relativePath).exists()) {
            return new File(baseClone.getAbsolutePath() + relativePath).getAbsolutePath();
        } else {
            return null;
        }
    }

    @Override
    public boolean saveFile(String relativePath, byte[] data, Boolean unlock) {
        boolean result = saveFile(relativePath, data);

        File f = new File(baseClone.getAbsolutePath() + relativePath);
        if (f.exists()) {
            SVNCommitClient clientCommit = new SVNCommitClient(authManager, SVNWCUtil.createDefaultOptions(true));
            File[] paths = {f};
            if (unlock) {
                Map<String, Long> unlocks = new HashMap<String, Long>();
                String relatvePathClean = relativePath;
                if (relativePath.startsWith("/")) {
                    relatvePathClean = relativePath.substring(relativePath.indexOf("/") + 1);
                }
                unlocks.put(relatvePathClean, null);
                try {
                    repository.unlock(unlocks, false, null);
                    clientCommit.doCommit(paths, true, "AutoCommit Kevoree Editor", false, false);
                    lockedFile.remove(relatvePathClean);
                } catch (SVNException e) {
                    logger.error("error while unkock and commit svn ", e);
                }
            }
        }
        return result;
    }

    @Port(name = "files", method = "unlock")
    public void unlock(String relativePath) {
        final String[] relatvePathClean = {relativePath};
        if (relativePath.startsWith("/")) {
            relatvePathClean[0] = relativePath.substring(relativePath.indexOf("/") + 1);
        }
        Map<String, Long> locks = new HashMap<String, Long>();
        locks.put(relatvePathClean[0], lastRevisionCheck);
        try {
            repository.lock(locks, "AutoLock Kevoree Editor", false, null);
            lockedFile.add(relatvePathClean[0]);
        } catch (SVNException e) {
            logger.error("Error while acquire lock ", e);
        }
    }

    @Port(name = "files", method = "saveFile")
    public boolean saveFile(String relativePath, byte[] data) {
        File f = new File(baseClone.getAbsolutePath() + relativePath);
        if (f.exists()) {
            try {
                FileOutputStream fw = new FileOutputStream(f);
                fw.write(data);
                fw.flush();
                fw.close();
                return true;
            } catch (Exception e) {
                logger.error("Error while getting file ", e);
                return false;
            }
        } else {
            logger.debug("No file exist = {}", baseClone.getAbsolutePath() + relativePath);
            return false;
        }
    }

    public static byte[] convertStream(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int l;
        do {
            l = (in.read(buffer));
            if (l > 0)
                out.write(buffer, 0, l);
        } while (l > 0);
        return out.toByteArray();
    }
}
