package org.kevoree.library.ui.fileExplorer;

import com.explodingpixels.macwidgets.*;
import org.kevoree.framework.MessagePort;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 07/11/11
 * Time: 21:38
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryExplorerPanel extends Panel {

    DirectoryExplorer dirExplorer = null;
    HashMap<SourceListItem, File> files = new HashMap<SourceListItem, File>();

    public DirectoryExplorerPanel(DirectoryExplorer explorer) {
        dirExplorer = explorer;
        this.setLayout(new BorderLayout());
    }

    public void refresh(String path) {
        files.clear();
        SourceListModel model = new SourceListModel();
        SourceListCategory category = new SourceListCategory("Files");
        model.addCategory(category);
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory() && dir.canRead() && dir.getName() != "." && dir.getName() != "..") {
            //
        } else {
            JFileChooser filechooser = new JFileChooser();
            filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            filechooser.setDialogTitle("Select base directory for Kevoree File Explorer ");
            int returnVal = filechooser.showOpenDialog(null);
            if (filechooser.getSelectedFile() != null && returnVal == JFileChooser.APPROVE_OPTION) {
                dir = filechooser.getSelectedFile();
            }
        }
        SourceListItem firstChild = new SourceListItem(dir.getName());
        files.put(firstChild, dir);
        model.addItemToCategory(firstChild, category);
        populateDir(model, firstChild, dir, true);
        SourceList sourceList = new SourceList(model);
        sourceList.setColorScheme(new SourceListDarkColorScheme());
        sourceList.setExpanded(category, false);
        sourceList.addSourceListSelectionListener(new SourceListSelectionListener() {
            @Override
            public void sourceListItemSelected(SourceListItem sourceListItem) {
                if (dirExplorer.getPortByName("fileurl") != null) {
                    dirExplorer.getPortByName("fileurl", MessagePort.class).process(files.get(sourceListItem));
                }
            }
        });
        this.removeAll();
        this.add(sourceList.getComponent(), BorderLayout.CENTER);

    }

    public void populateDir(SourceListModel model, SourceListItem parent, File dir, boolean first) {
        if (dir.exists() && dir.isDirectory() && dir.canRead() && dir.getName() != "." && dir.getName() != "..") {
            if (!first) {
                SourceListItem item = new SourceListItem(dir.getName());
                model.addItemToItem(item, parent);
                files.put(item, dir);
                File[] childs = dir.listFiles();
                for (int i = 0; i < childs.length; i++) {
                    populateDir(model, item, childs[i], false);
                }
            } else {
                File[] childs = dir.listFiles();
                for (int i = 0; i < childs.length; i++) {
                    populateDir(model, parent, childs[i], false);
                }
            }


        }
    }


}
