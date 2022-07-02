package com.datavirtue.axiom.ui.shared;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

/**
 *
 * @author SeanAnderson May 21, 2022
 */
public class ImageFileTransferHandler extends TransferHandler {

        private static final DataFlavor[] supportedDataFlavors = {DataFlavor.javaFileListFlavor};

        private final JList previewList;
        private DroppedFilesHandler callback;
        
        public ImageFileTransferHandler(JList previewList, DroppedFilesHandler handler) {
            this.callback = handler;
            this.previewList = previewList;
        }

        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY;
        }

        protected Transferable createTransferable(JComponent c) {
                                                
            return new Transferable() {
                public DataFlavor[] getTransferDataFlavors() {
                    return supportedDataFlavors;
                }

                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    
                    for(var supported : supportedDataFlavors) {
                        if (flavor.equals(supported)) {
                            return true;
                        }
                    }
                    
                    return false;
                }

                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    return previewList.getSelectedValue(); //TODO: populate Object with image
                }
            };
        }

        public boolean canImport(TransferSupport support) {
            
            if (!isTransferSupported(support)) {
                return false;
            }

            JList.DropLocation dl = (JList.DropLocation)support.getDropLocation();
            if (dl.getIndex() == -1) {
                System.out.println("item dropped");                
                return false;
            } else {
                return true;
            }
        }
        
        private boolean isTransferSupported(TransferSupport support) {
            for(var flavor : supportedDataFlavors) {
                if (support.isDataFlavorSupported(flavor)) {
                    return true;
                }
            }
            return false;
        }
        
        private DataFlavor getDataFlavour(TransferSupport support) {
            for(var flavor : supportedDataFlavors) {
                if (support.isDataFlavorSupported(flavor)) {
                    return flavor;
                }
            }
            return null;
        }

        public boolean importData(TransferSupport support) {
                        
            if (!canImport(support)) {
                return false;
            }

            Transferable transferable = support.getTransferable();
            try {
                
                var transferData = transferable.getTransferData(getDataFlavour(support)); 
                
                if (!(transferData instanceof List)) {
                    System.out.println("Why is this getting called?!");
                    return true;
                }
                
                var fileList = (List) transferData;             
                var draggedFiles = Arrays.asList(fileList.get(0));                
                var files = new ArrayList<File>();
                for(var file : draggedFiles) {
                    files.add((File)file);                    
                }
                callback.handleDroppedFiles(files);
                return true;
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void exportDone(JComponent source, Transferable data, int action) {
            super.exportDone(source, data, action);            
        }
    }