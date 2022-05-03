package com.datavirtue.nevitium.ui.shared;

import com.datavirtue.nevitium.models.inventory.Inventory;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

/**
 *
 * @author SeanAnderson
 */
public class ImageTransferHandler extends TransferHandler {

        private static final DataFlavor DATA_FLAVOUR = new DataFlavor(Inventory.class, "Images");

        private final JList previewList;
        private boolean inDrag;

        public ImageTransferHandler(JList previewList) {
            this.previewList = previewList;
        }

        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        protected Transferable createTransferable(JComponent c) {
            inDrag = true;
            return new Transferable() {
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] {DATA_FLAVOUR};
                }

                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return flavor.equals(DATA_FLAVOUR);
                }

                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    return previewList.getSelectedValue();
                }
            };
        }

        public boolean canImport(TransferSupport support) {
            if (!inDrag || !support.isDataFlavorSupported(DATA_FLAVOUR)) {
                return false;
            }

            JList.DropLocation dl = (JList.DropLocation)support.getDropLocation();
            if (dl.getIndex() == -1) {
                return false;
            } else {
                return true;
            }
        }

        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            Transferable transferable = support.getTransferable();
            try {
                Object draggedImage = transferable.getTransferData(DATA_FLAVOUR);

                JList.DropLocation dl = (JList.DropLocation)support.getDropLocation();
                DefaultListModel model = (DefaultListModel)previewList.getModel();
                int dropIndex = dl.getIndex();
                if (model.indexOf(draggedImage) < dropIndex) {
                    dropIndex--;
                }
                model.removeElement(draggedImage);
                model.add(dropIndex, draggedImage);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void exportDone(JComponent source, Transferable data, int action) {
            super.exportDone(source, data, action);
            inDrag = false;
        }
    }