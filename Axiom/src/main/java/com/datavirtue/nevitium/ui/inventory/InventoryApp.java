/*
 * InventoryDialog.java
 *
 * Created on June 23, 2006, 9:29 AM
 *
 */
/**
 *
 * @author  Sean K Anderson - Data Virtue
 * @rights Copyright Data Virtue 2006, 2007, 2008, 2009, 2010, 2011, 2022 All
 * Rights Reserved.
 */
package com.datavirtue.nevitium.ui.inventory;

import com.datavirtue.nevitium.ui.util.LimitedDocument;

import com.datavirtue.nevitium.ui.util.JTextFieldFilter;
import com.datavirtue.nevitium.models.inventory.InventoryTableModel;
import com.datavirtue.nevitium.ui.StatusDialog;
import com.datavirtue.nevitium.services.DiService;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Date;
import com.datavirtue.nevitium.models.inventory.Inventory;
import com.datavirtue.nevitium.models.inventory.InventoryImage;
import com.datavirtue.nevitium.models.settings.AppSettings;
import com.datavirtue.nevitium.services.InventoryService;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import com.datavirtue.nevitium.services.AppSettingsService;
import com.datavirtue.nevitium.services.ExceptionService;
import com.datavirtue.nevitium.services.InventoryImageService;
import com.datavirtue.nevitium.services.LocalSettingsService;
import com.datavirtue.nevitium.services.UserService;
import com.datavirtue.nevitium.services.util.CurrencyUtil;
import com.datavirtue.nevitium.services.util.DV;
import com.datavirtue.nevitium.ui.shared.CollectionMappedListModel;
import com.datavirtue.nevitium.ui.shared.DroppedFilesHandler;
import com.datavirtue.nevitium.ui.shared.ImageFileTransferHandler;
import com.datavirtue.nevitium.ui.shared.InventoryImageCellRenderer;
import com.datavirtue.nevitium.ui.util.AutoCompleteDocument;
import com.datavirtue.nevitium.ui.util.DecimalCellRenderer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;

public class InventoryApp extends javax.swing.JDialog {

    private InventoryService inventoryService;
    private InventoryImageService inventoryImageService;
    private AppSettingsService appSettingsService;
    private AppSettings appSettings;
    private Inventory currentItem = new Inventory();

    private java.util.List<Inventory> returnValue;
    private long lastRecvDate;
    private long lastSaleDate;
    private java.awt.Image winIcon;
    private java.awt.Frame parentWin;
    private boolean selectMode;
    private TableModel tm;

    private CompletableFuture<Void> imageLoader;

    /**
     * Creates new form InventoryDialog
     */
    public InventoryApp(java.awt.Frame parent, boolean modal, boolean select) {

        super(parent, modal);

        initComponents();
        var injector = DiService.getInjector();
        inventoryService = injector.getInstance(InventoryService.class);
        inventoryImageService = injector.getInstance(InventoryImageService.class);
        appSettingsService = injector.getInstance(AppSettingsService.class);
        appSettingsService.setObjectType(AppSettings.class);

        Toolkit tools = Toolkit.getDefaultToolkit();
        winIcon = tools.getImage(InventoryApp.class.getResource("/Orange.png"));
        this.setIconImage(winIcon);

        //Adjust references, to this context, for the needed objects 
        selectMode = select;

        if (selectMode) {
            receiveModeBox.setVisible(false);
        }

        /* Limit field characters */
        qtyTextField.setDocument(new JTextFieldFilter(JTextFieldFilter.FLOAT));
        costTextField.setDocument(new JTextFieldFilter(JTextFieldFilter.FLOAT));
        priceTextField.setDocument(new JTextFieldFilter(JTextFieldFilter.FLOAT));

        /* Allow the price field to accept negative numbers */
        JTextFieldFilter tf = (JTextFieldFilter) priceTextField.getDocument();
        tf.setNegativeAccepted(true);

        weightTextField.setDocument(new JTextFieldFilter(JTextFieldFilter.FLOAT));

        upcTextField.setDocument(new LimitedDocument(14));
        codeTextField.setDocument(new LimitedDocument(16));
        descTextField.setDocument(new LimitedDocument(50));
        sizeTextField.setDocument(new LimitedDocument(15));
        weightTextField.setDocument(new LimitedDocument(15));
        catTextField.setDocument(new LimitedDocument(20));

        /* Set the spinner  */
 /*                                              val min max  incr      */
        SpinnerNumberModel model = new SpinnerNumberModel(0, 0, null, 1);
        reorderSpinnerControl.setModel(model);

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {

                try {
                    recordWindowSizeAndPosition();
                } catch (BackingStoreException ex) {
                    ExceptionService.showErrorDialog(e.getComponent(), ex, "Error saving local screen preferences");
                }

            }
        });

        /* Close dialog on escape */
        ActionMap am = getRootPane().getActionMap();
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object windowCloseKey = new Object();
        KeyStroke windowCloseStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        Action windowCloseAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                setVisible(false);
                dispose();
            }
        };
        im.put(windowCloseStroke, windowCloseKey);
        am.put(windowCloseKey, windowCloseAction);
        /**/

        if (select) {

            savePanel.setVisible(false);

        }

    }//end constructor

    SwingWorker worker = new SwingWorker() {

        public Object doInBackground() {
            StatusDialog sd = new StatusDialog(parentWin, false, "Please Wait", false);
            sd.changeMessage("Initializing Inventory");
            sd.addStatus("Building inventory table...");
            //tm = db.createTableModel("inventory", iTable);//time consuming
            try {
                var allInventory = inventoryService.getAll();
                tm = new InventoryTableModel(allInventory);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            sd.addStatus("Inventory table Complete.");
            sd.addStatus("Configuring inventory table...");

            return sd;
        }

        public void done() {
            inventoryTable.setModel(tm);

            inventoryTable.getColumnModel().getColumn(2).setCellRenderer(new DecimalCellRenderer(18, 2, SwingConstants.RIGHT));
            inventoryTable.getColumnModel().getColumn(3).setCellRenderer(new DecimalCellRenderer(18, 2, SwingConstants.RIGHT));

            StatusDialog d = null;
            try {
                d = (StatusDialog) get();
            } catch (InterruptedException ex) {
                Logger.getLogger(InventoryApp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(InventoryApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            d.addStatus("<<< Complete >>>");
            d.dispose();
        }
    };

    /* My initializer method */
    public void display() throws SQLException, BackingStoreException {

        appSettings = appSettingsService.getObject();
        var user = UserService.getCurrentUser();

        if (!user.isAdmin() && user.getInventory() < 300) {
            JOptionPane.showMessageDialog(this, "Please see the admin about permissions.", "Access denied", JOptionPane.OK_OPTION);
            this.dispose();
            return;
        }

        this.setTitle(appSettings.getCompany().getCompanyName() + " Inventory Manager");

        taxCheckBox.setText(appSettings.getInvoice().getTax1Name());
        tax2CheckBox.setText(appSettings.getInvoice().getTax2Name());

        if (selectMode) {
            /* Setup Select Mode */
            saveButton.setVisible(true);
            saveButton.setEnabled(false);
            selectButton.setVisible(true);
            voidButton.setVisible(true);
            setFieldsEnabled(false);

        } else {
            saveButton.setVisible(true);
            saveButton.setEnabled(false);
            selectButton.setVisible(false);
            voidButton.setVisible(false);
            setFieldsEnabled(false);
        }

        searchFieldCombo.setSelectedItem(appSettings.getInventory().getDefaultInventorySearchField());

        taxCheckBox.setToolTipText(appSettings.getInvoice().getTax1Name());
        tax2CheckBox.setToolTipText(appSettings.getInvoice().getTax2Name());

        findTextField.requestFocus();

        worker.execute();

        this.populateCategoryList();

        this.restoreSavedWindowSizeAndPosition();

        this.setVisible(true);

    }

    private void recordWindowSizeAndPosition() throws BackingStoreException {
        var localSettings = LocalSettingsService.getLocalAppSettings();
        var screenSettings = localSettings.getScreenSettings();
        var sizeAndPosition = LocalSettingsService.getWindowSizeAndPosition(this);
        screenSettings.setInventory(sizeAndPosition);
        LocalSettingsService.saveLocalAppSettings(localSettings);
    }

    private void restoreSavedWindowSizeAndPosition() throws BackingStoreException {
        var localSettings = LocalSettingsService.getLocalAppSettings();
        var screenSettings = localSettings.getScreenSettings().getInventory();
        LocalSettingsService.applyScreenSizeAndPosition(screenSettings, this);
    }

    private void populateCategoryList() {
        try {
            var categories = inventoryService.getInventoryCategories();
            var autoComplete = new AutoCompleteDocument(catTextField, new ArrayList<String>(Arrays.asList(categories)));
            catTextField.setDocument(autoComplete);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void calculateMarkup() {
        if (StringUtils.isEmpty(priceTextField.getText().trim())) {
            try {
                double cost = Double.parseDouble(costTextField.getText());
                var price = inventoryService.calculateMarkup(cost);
                priceTextField.setText(CurrencyUtil.money(price));
            } catch (NumberFormatException numberFormatException) {
                ExceptionService.showErrorDialog(this, numberFormatException, "Error parsing calculated markup number");
            } catch (SQLException sqlException) {
                ExceptionService.showErrorDialog(this, sqlException, "Error fetching default markup value settings");
            }

        }

    }

    private void setFieldsEnabled(boolean enabled) {

        upcTextField.setEnabled(enabled);
        codeTextField.setEnabled(enabled);
        descTextField.setEnabled(enabled);
        sizeTextField.setEnabled(enabled);
        weightTextField.setEnabled(enabled);
        qtyTextField.setEnabled(enabled);
        costTextField.setEnabled(enabled);
        priceTextField.setEnabled(enabled);
        catTextField.setEnabled(enabled);
        serviceBox.setEnabled(enabled);
        reorderSpinnerControl.setEnabled(enabled);

        picField.setEnabled(enabled);

        taxCheckBox.setEnabled(enabled);
        tax2CheckBox.setEnabled(enabled);
        partialBox.setEnabled(enabled);
        availableCheckBox.setEnabled(enabled);

        picButton.setEnabled(enabled);

        if (currentItem != null && currentItem.getId() != null) {

            receiveButton.setEnabled(enabled);

        } else {

            receiveButton.setEnabled(false);
        }

        if (enabled) {

            helpBox.setText("Remember to click 'Save' after making changes.");

        } else {

            helpBox.setText("Click the UPC field to start a new record.");

        }
        noteButton.setEnabled(enabled);

    }

    private void clearFields() {

        upcTextField.setText("");
        codeTextField.setText("");
        descTextField.setText("");
        sizeTextField.setText("");
        weightTextField.setText("");
        qtyTextField.setText("0.000");
        costTextField.setText("0.00");
        priceTextField.setText("0.00");

        catTextField.setText("");

        saleField.setText("");
        recvdField.setText("");

        reorderSpinnerControl.setValue(0);

        picField.setText("");

        taxCheckBox.setSelected(false);
        tax2CheckBox.setSelected(false);
        availableCheckBox.setSelected(false);
        costTotalLabel.setText("0.00");
        retailTotalLabel.setText("0.00");

        viewButton.setEnabled(false);

        findTextField.requestFocus();

        serviceBox.setSelected(false);

    }

    private void populateImageListModel() {
        imageList.setModel(new CollectionMappedListModel<InventoryImage>((ArrayList<InventoryImage>) new ArrayList(this.currentItem.getImages().stream().toList())));
    }

    private void createImageList() {

        var handler = new DroppedFilesHandler() {
            @Override
            public void handleDroppedFiles(ArrayList<File> files) {
                for (var file : files) {
                    try {
                        var image = Files.readAllBytes(file.toPath());
                        var inventoryImage = new InventoryImage();
                        inventoryImage.setImage(image);
                        inventoryImage.setCaption(file.getName());
                        saveImage(inventoryImage);
                    } catch (IOException ex) {
                        ExceptionService.showErrorDialog(parentWin, ex, "Error reading the image file");
                    } catch (SQLException ex) {
                        ExceptionService.showErrorDialog(parentWin, ex, "Error saving image to database");
                    }
                }
                populateImageListModel();
            }
        };

        imageList.setCellRenderer(new InventoryImageCellRenderer());
        imageList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        imageList.setVisibleRowCount(-1);
        imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //imageList.setFixedCellWidth(240);
        //imageList.setFixedCellHeight(120);
        //imageList.setDragEnabled(true);
        //imageList.setDropMode(DropMode.INSERT);
        imageList.setTransferHandler(new ImageFileTransferHandler(imageList, handler));
    }

    /**
     * Will find ALL occurances of matching records and selects these multiples
     * in the table view.
     */
    private void search() {

        boolean match = false;

        if (findTextField.getText().trim().equals("")) {
            refreshTable();
            clearFields();
            setFieldsEnabled(false);
            saveButton.setEnabled(false);
            findTextField.requestFocus();
            return;
        }

        /* get text from findTextField.getText(); */
        String text = findTextField.getText();

        /* build search from button status  */
        int col = searchFieldCombo.getSelectedIndex() + 1;

        if (col == 4) { //size

            java.util.List<Inventory> searchResults = null;

            try {
                searchResults = inventoryService.getAllInventoryBySize(findTextField.getText());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (searchResults == null || searchResults.size() == 0) {
                refreshTable();
                findTextField.requestFocus();
                findTextField.selectAll();
            } else {
                inventoryTable.setModel(new InventoryTableModel(searchResults));
                match = true;
                findTextField.selectAll();
                return;
            }

        }

        if (col == 5) { //weight

            java.util.List<Inventory> searchResults = null;

            try {
                searchResults = inventoryService.getAllInventoryByWeight(findTextField.getText());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (searchResults == null || searchResults.size() == 0) {
                refreshTable();
                findTextField.requestFocus();
                findTextField.selectAll();
            } else {
                inventoryTable.setModel(new InventoryTableModel(searchResults));
                match = true;
                findTextField.selectAll();
                return;
            }

        }

        if (col == 6) { //category

            java.util.List<Inventory> searchResults = null;

            try {
                searchResults = inventoryService.getAllInventoryByCategory(findTextField.getText());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (searchResults == null || searchResults.size() == 0) {
                refreshTable();
                findTextField.requestFocus();
                findTextField.selectAll();
            } else {
                inventoryTable.setModel(new InventoryTableModel(searchResults));
                match = true;
                findTextField.selectAll();
                return;
            }
        }

        if (col == 3) { //search for desc

            java.util.List<Inventory> searchResults = null;

            try {
                searchResults = inventoryService.getAllInventoryByDecription(findTextField.getText());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (searchResults == null || searchResults.size() == 0) {
                refreshTable();
                findTextField.requestFocus();
                findTextField.selectAll();
            } else {
                inventoryTable.setModel(new InventoryTableModel(searchResults));
                match = true;
                findTextField.selectAll();
                return;
            }
        }

        if (col == 1) { //UPC

            java.util.List row = DV.searchTableMulti(inventoryTable.getModel(), 1, text);

            if (row != null) {

                inventoryTable.clearSelection();

                for (int r = 0; r < row.size(); r++) {
                    //iTable.changeSelection((Integer) row.get(r), 0, true, true);
                    inventoryTable.addRowSelectionInterval((Integer) row.get(r), (Integer) row.get(r));
                }
                populateFields();
                match = true;
            } else {

                if (receiveModeBox.isSelected()) {
                    int a
                            = javax.swing.JOptionPane.showConfirmDialog(null,
                                    "No record for this UPC was found would you like to add a new one?", "No Match", JOptionPane.YES_NO_OPTION);
                    if (a == 0) {
                        clearFields();
                        setFieldsEnabled(true);
                        upcTextField.setText(findTextField.getText());
                        upcTextField.requestFocus();
                        saveButton.setEnabled(true);
                        return;
                    }
                }
            }

            findTextField.selectAll();
        }

        if (col == 2) {  //Code

            java.util.List row = DV.searchTableMulti(inventoryTable.getModel(), 0, text);

            if (row != null) {
                inventoryTable.clearSelection();
                for (int r = 0; r < row.size(); r++) {
                    inventoryTable.addRowSelectionInterval((Integer) row.get(r), (Integer) row.get(r));
                }
                populateFields();
                match = true;
            } else {

                if (receiveModeBox.isSelected()) {
                    int a
                            = javax.swing.JOptionPane.showConfirmDialog(null,
                                    "No record for this CODE was found would you like to add a new one?", "No Match", JOptionPane.YES_NO_OPTION);
                    if (a == 0) {
                        currentItem = new Inventory();
                        clearFields();
                        setFieldsEnabled(true);
                        saveButton.setEnabled(true);
                        codeTextField.setText(findTextField.getText());
                        upcTextField.requestFocus();
                        return;
                    }
                }
            }
            findTextField.selectAll();
        }

        if (catTextField.isEnabled()) {
            saveButton.setEnabled(true);
        }

        if (!match) {
            clearFields();
            this.setFieldsEnabled(false);
            this.saveButton.setEnabled(false);
        } else {
            if (receiveModeBox.isSelected()) {
                this.receive();
            }
        }
    }

    private void refreshTable() {
        try {
            var allInventory = inventoryService.getAll();
            inventoryTable.setModel(new InventoryTableModel(allInventory));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called to populate form JTextFields when a user clicks a row on the
     * JTable
     */
    private void populateFields() {

        try {
            this.populateForm();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error retrieving images for inventory item: " + this.currentItem.getDescription());
        }
    }

    private void populateForm() throws SQLException {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        var tableModel = (InventoryTableModel) inventoryTable.getModel();

        this.currentItem = (Inventory) tableModel.getValueAt(selectedRow);

        upcTextField.setText(currentItem.getUpc());
        codeTextField.setText(currentItem.getCode());
        descTextField.setText(currentItem.getDescription());
        sizeTextField.setText(currentItem.getSize());
        weightTextField.setText(currentItem.getWeight());
        qtyTextField.setText(Double.toString(currentItem.getQuantity()));

        if (UserService.getCurrentUser().isAdmin()
                || UserService.getCurrentUser().getInventory() >= 300) {
            costTextField.setText(CurrencyUtil.money(currentItem.getCost()));
        }
        priceTextField.setText(CurrencyUtil.money(currentItem.getPrice()));

        catTextField.setText(currentItem.getCategory());

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);

        lastSaleDate = currentItem.getLastSale() != null ? currentItem.getLastSale().getTime() : null;
        lastRecvDate = currentItem.getLastReceived() != null ? currentItem.getLastReceived().getTime() : null;

        String lastSale = df.format(lastSaleDate);
        String recvDate = df.format(lastRecvDate);

        saleField.setText(lastSale);
        recvdField.setText(recvDate);

        reorderSpinnerControl.setValue(currentItem.getReorderCutoff());
        taxCheckBox.setSelected(currentItem.isTax1());
        tax2CheckBox.setSelected(currentItem.isTax2());
        partialBox.setSelected(currentItem.isPartialSaleAllowed());
        availableCheckBox.setSelected(currentItem.isAvailable());

        computePrices();

        loadImages();

        this.createImageList();
        this.populateImageListModel();

        setFieldsEnabled(true);

        if (currentItem.getCategory() != null && currentItem.getCategory().equalsIgnoreCase("Service")) {
            serviceBox.setSelected(true);
        } else {
            serviceBox.setSelected(false);
        }
        checkService();
        noteButton.setEnabled(true);
    }

    private void loadImages() {
        if (imageLoader != null) {
            imageLoader.complete(null);
        }
        imageLoader = CompletableFuture.runAsync(() -> {

            if (currentItem.getImages() == null) {
                try {
                    // TODO: make cancellable...                    
                    var images = inventoryImageService.getAllImagesForInventory(currentItem.getId());
                    currentItem.setImages(images);
                } catch (SQLException ex) {
                    ExceptionService.showErrorDialog(parentWin, ex, "Error retrieving images from database");
                }
            }

        });

    }

    private void getNote(int invKey) {

    }

    private void computePrices() {

        if (!qtyTextField.getText().equals("")
                && !costTextField.getText().equals("")
                && !priceTextField.getText().equals("")) {

            //Version 1.5                   
            float qty = Float.parseFloat(qtyTextField.getText());

            if (qty > 0) {

                float cost = Float.parseFloat(costTextField.getText());

                float price = Float.parseFloat(priceTextField.getText());  //error

                costTotalLabel.setText(CurrencyUtil.money(cost * qty));
                retailTotalLabel.setText(CurrencyUtil.money(price * qty));

            } else {

                costTotalLabel.setText("0.00");
                retailTotalLabel.setText("0.00");

            }
        }
    }

    private void receive() {

        if (catTextField.getText().equalsIgnoreCase("Service")) {
            return;
        }
        if (!upcTextField.isEnabled()) {
            return;
        }

        InventoryReceivePromptDialog rpd
                = new InventoryReceivePromptDialog(null, true, descTextField.getText());

        boolean negZero = rpd.isNegZero();

        if (rpd.getInAmount() == 0) {
            return;
        }

        float current = 0f;
        String _q = qtyTextField.getText();

        if (DV.validFloatString(_q)) {
            current = Float.parseFloat(_q);
        }

        /*  */
        if (current < 0) {

            if (negZero) {
                current = 0.00f;
            }

        }

        float received = rpd.getInAmount();

        rpd = null;

        current += received;

        qtyTextField.setText(CurrencyUtil.money(current));

        //Version 1.5
        lastRecvDate = new java.util.Date().getTime();
        DateFormat df = new SimpleDateFormat();
        recvdField.setText(df.format(new Date(lastRecvDate)));

        try {
            saveRecord();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error Saving", JOptionPane.OK_OPTION);
        }

        findTextField.selectAll();
        findTextField.requestFocus();
    }

    /* Public method to grab the needed value in Select Mode */
    public java.util.List<Inventory> getReturnValue() {

        return returnValue;

    }

    /* NetBeans-generated actionPerformance methods */
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jSplitPane1 = new javax.swing.JSplitPane();
        tablePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        inventoryTable = new javax.swing.JTable();
        jToolBar1 = new javax.swing.JToolBar();
        viewButton = new javax.swing.JButton();
        groupButton = new javax.swing.JButton();
        labelButton = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        deleteButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        selectButton = new javax.swing.JButton();
        voidButton = new javax.swing.JButton();
        searchFieldCombo = new javax.swing.JComboBox();
        findTextField = new javax.swing.JTextField();
        receiveModeBox = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        detailTabPane = new javax.swing.JTabbedPane();
        detailPanel = new javax.swing.JPanel();
        assField = new javax.swing.JLabel();
        costTotalLabel = new javax.swing.JLabel();
        retailTotalLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        sizeTextField = new javax.swing.JTextField();
        catTextField = new javax.swing.JTextField();
        codeTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        costTextField = new javax.swing.JTextField();
        descTextField = new javax.swing.JTextField();
        receiveButton = new javax.swing.JButton();
        serviceBox = new javax.swing.JCheckBox();
        qtyTextField = new javax.swing.JTextField();
        priceTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        upcTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        weightTextField = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        partialBox = new javax.swing.JCheckBox();
        taxCheckBox = new javax.swing.JCheckBox();
        tax2CheckBox = new javax.swing.JCheckBox();
        statusPanel = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        saleField = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        recvdField = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        reorderSpinnerControl = new javax.swing.JSpinner();
        availableCheckBox = new javax.swing.JCheckBox();
        notesScrollPane = new javax.swing.JScrollPane();
        notesPane = new javax.swing.JTextPane();
        noteButton = new javax.swing.JButton();
        picturesPanel = new javax.swing.JPanel();
        picField = new javax.swing.JTextField();
        picButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        imageList = new javax.swing.JList<>();
        savePanel = new javax.swing.JPanel();
        helpBox = new javax.swing.JTextField();
        jToolBar2 = new javax.swing.JToolBar();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        clearButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Inventory Manager");
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                formKeyTyped(evt);
            }
        });

        jSplitPane1.setMinimumSize(new java.awt.Dimension(10, 10));
        jSplitPane1.setOneTouchExpandable(true);

        tablePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jScrollPane1.setMinimumSize(new java.awt.Dimension(14, 14));
        jScrollPane1.setRequestFocusEnabled(false);

        inventoryTable.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        inventoryTable.setToolTipText("Click a row and press F9 to receive.");
        inventoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                inventoryTableMousePressed(evt);
            }
        });
        inventoryTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                inventoryTableKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                inventoryTableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(inventoryTable);

        jToolBar1.setRollover(true);

        viewButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/image_gallery_32px.png"))); // NOI18N
        viewButton.setText("View Pic (F12)");
        viewButton.setEnabled(false);
        viewButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        viewButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        viewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(viewButton);

        groupButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/hierarchy_32px.png"))); // NOI18N
        groupButton.setText("Groups");
        groupButton.setToolTipText("Highlight Inventory Items and Hit this Button to Create Product Groups");
        groupButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        groupButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        groupButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                groupButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(groupButton);

        labelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/barcode_32px.png"))); // NOI18N
        labelButton.setText("Labels");
        labelButton.setToolTipText("Select rows above and press this button to create labels.");
        labelButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        labelButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        labelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                labelButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(labelButton);
        jToolBar1.add(filler2);

        deleteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/Delete_32px.png"))); // NOI18N
        deleteButton.setText("Delete");
        deleteButton.setToolTipText("Deleting In-Use Inventory Items is Not Recommended");
        deleteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(deleteButton);

        selectButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        selectButton.setText("Select");
        selectButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectButtonActionPerformed(evt);
            }
        });

        voidButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        voidButton.setText("None");
        voidButton.setMargin(new java.awt.Insets(2, 10, 2, 10));
        voidButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                voidButtonActionPerformed(evt);
            }
        });

        searchFieldCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "UPC", "Code", "Description", "Size", "Weight", "Category" }));
        searchFieldCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchFieldComboActionPerformed(evt);
            }
        });

        findTextField.setBackground(new java.awt.Color(0, 0, 0));
        findTextField.setFont(new java.awt.Font("Courier New", 1, 13)); // NOI18N
        findTextField.setForeground(new java.awt.Color(0, 255, 0));
        findTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        findTextField.setToolTipText("Press F9 to Toggle Receive Mode");
        findTextField.setCaretColor(new java.awt.Color(51, 255, 255));
        findTextField.setNextFocusableComponent(inventoryTable);
        findTextField.setSelectionColor(new java.awt.Color(0, 255, 51));
        findTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                findTextFieldFocusGained(evt);
            }
        });
        findTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                findTextFieldKeyPressed(evt);
            }
        });

        receiveModeBox.setText("Receive Mode");
        receiveModeBox.setToolTipText("Allows You to Quickly Update Inventory Quantities for Scanned Items.");
        receiveModeBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        receiveModeBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                receiveModeBoxMouseClicked(evt);
            }
        });
        receiveModeBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                receiveModeBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(selectButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(voidButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(searchFieldCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 136, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(findTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 170, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(137, 137, 137))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(receiveModeBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 96, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(selectButton)
                    .add(voidButton)
                    .add(searchFieldCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(findTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(receiveModeBox)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout tablePanelLayout = new org.jdesktop.layout.GroupLayout(tablePanel);
        tablePanel.setLayout(tablePanelLayout);
        tablePanelLayout.setHorizontalGroup(
            tablePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(tablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(tablePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(tablePanelLayout.createSequentialGroup()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 486, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 59, Short.MAX_VALUE)))
                .addContainerGap())
        );
        tablePanelLayout.setVerticalGroup(
            tablePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, tablePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(tablePanel);

        detailTabPane.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        detailPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        assField.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        assField.setText("(EAN/UPC/GTIN Barcode)");

        costTotalLabel.setFont(new java.awt.Font("OCR B MT", 0, 12)); // NOI18N
        costTotalLabel.setText("0.00");

        retailTotalLabel.setFont(new java.awt.Font("OCR B MT", 0, 12)); // NOI18N
        retailTotalLabel.setText("0.00");

        jLabel4.setText("Size");

        sizeTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        sizeTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        sizeTextField.setToolTipText("[15 Char] ");
        sizeTextField.setNextFocusableComponent(weightTextField);
        sizeTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                sizeTextFieldFocusGained(evt);
            }
        });

        catTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        catTextField.setToolTipText("Use Standard Categories to Help Sort Products");
        catTextField.setNextFocusableComponent(taxCheckBox);
        catTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                catTextFieldFocusGained(evt);
            }
        });
        catTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                catTextFieldKeyPressed(evt);
            }
        });

        codeTextField.setColumns(16);
        codeTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        codeTextField.setToolTipText("[16 Char] In-House Code System  [Code or UPC REQUIRED]");
        codeTextField.setNextFocusableComponent(descTextField);
        codeTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                codeTextFieldFocusGained(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel8.setText("Price");

        costTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        costTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        costTextField.setToolTipText("Item Cost");
        costTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                costTextFieldFocusGained(evt);
            }
        });

        descTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        descTextField.setToolTipText("[50 Char] Appears on the Invoices and Quotes  [REQUIRED FIELD]");
        descTextField.setNextFocusableComponent(sizeTextField);

        receiveButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        receiveButton.setText("Receive");
        receiveButton.setToolTipText("This button updates the Quanity and saves the record");
        receiveButton.setEnabled(false);
        receiveButton.setMargin(new java.awt.Insets(1, 6, 1, 4));
        receiveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                receiveButtonActionPerformed(evt);
            }
        });

        serviceBox.setText("Service");
        serviceBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        serviceBox.setEnabled(false);
        serviceBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serviceBoxActionPerformed(evt);
            }
        });

        qtyTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        qtyTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        qtyTextField.setToolTipText("Quantity On Hand");
        qtyTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                qtyTextFieldActionPerformed(evt);
            }
        });

        priceTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        priceTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        priceTextField.setToolTipText("Selling Price");
        priceTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                priceTextFieldFocusGained(evt);
            }
        });

        jLabel7.setText("Cost");

        upcTextField.setColumns(14);
        upcTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        upcTextField.setToolTipText("Click Here to Create a New Item [14 Char] [Code or UPC REQUIRED]");
        upcTextField.setNextFocusableComponent(codeTextField);
        upcTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                upcTextFieldMouseClicked(evt);
            }
        });
        upcTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                upcTextFieldFocusGained(evt);
            }
        });

        jLabel1.setText("UPC");

        jLabel2.setText("Code");

        jLabel3.setText("Desc.");

        jLabel9.setText("Category");

        weightTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        weightTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        weightTextField.setToolTipText("[15 Char] Input a Raw Number to Allow Weight Calculations on the Invoices");
        weightTextField.setNextFocusableComponent(qtyTextField);
        weightTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                weightTextFieldFocusGained(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jLabel16.setText("=  ");

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel6.setText("Quantity");

        jLabel14.setFont(new java.awt.Font("Arial Black", 1, 12)); // NOI18N
        jLabel14.setText("=  ");

        jLabel5.setText("Weight");

        jLabel17.setForeground(new java.awt.Color(255, 0, 0));
        jLabel17.setText("*");

        partialBox.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        partialBox.setLabel("Allow partial quantity sale");
        partialBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                partialBoxActionPerformed(evt);
            }
        });

        taxCheckBox.setLabel("Tax 1");

        tax2CheckBox.setLabel("Tax 2");

        org.jdesktop.layout.GroupLayout detailPanelLayout = new org.jdesktop.layout.GroupLayout(detailPanel);
        detailPanel.setLayout(detailPanelLayout);
        detailPanelLayout.setHorizontalGroup(
            detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(detailPanelLayout.createSequentialGroup()
                .add(12, 12, 12)
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel4)
                    .add(jLabel3)
                    .add(jLabel5)
                    .add(jLabel6)
                    .add(jLabel7)
                    .add(jLabel8)
                    .add(jLabel9)
                    .add(jLabel2)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(detailPanelLayout.createSequentialGroup()
                        .add(upcTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 105, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(assField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(detailPanelLayout.createSequentialGroup()
                        .add(catTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 203, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(serviceBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(tax2CheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(detailPanelLayout.createSequentialGroup()
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(partialBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, codeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 153, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, detailPanelLayout.createSequentialGroup()
                                .add(descTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 293, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel17))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, detailPanelLayout.createSequentialGroup()
                                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, sizeTextField)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, weightTextField)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, qtyTextField)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, costTextField)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, priceTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                        .add(detailPanelLayout.createSequentialGroup()
                                            .add(jLabel16)
                                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                            .add(retailTotalLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, detailPanelLayout.createSequentialGroup()
                                            .add(jLabel14)
                                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                            .add(costTotalLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .add(receiveButton)))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, taxCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        detailPanelLayout.setVerticalGroup(
            detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(detailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(upcTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(assField))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(codeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(descTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(detailPanelLayout.createSequentialGroup()
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(sizeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel4))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(weightTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel5))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel6)
                            .add(qtyTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(receiveButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel7)
                            .add(costTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel8)
                            .add(priceTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(detailPanelLayout.createSequentialGroup()
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel14)
                            .add(costTotalLabel))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel16)
                            .add(retailTotalLabel))))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(catTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(serviceBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(partialBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(taxCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(tax2CheckBox)
                .addContainerGap(151, Short.MAX_VALUE))
        );

        detailTabPane.addTab("Detail", detailPanel);

        statusPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jLabel15.setText("Last Sale");

        saleField.setEditable(false);
        saleField.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        saleField.setToolTipText("Last Time this Item was Sold on an Invoice ");

        jLabel19.setText("Last Received");

        recvdField.setEditable(false);
        recvdField.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        recvdField.setToolTipText("The Last Time Inventory was Added with the Recieve Function");

        jLabel20.setText("Reorder @");

        reorderSpinnerControl.setToolTipText("Lowest Amount of Available Stock Before Reorder");

        availableCheckBox.setText("Unavailable");
        availableCheckBox.setToolTipText("Unavailable items fire a warning when you try to sell them");
        availableCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        availableCheckBox.setNextFocusableComponent(picButton);

        notesPane.setEditable(false);
        notesPane.setBackground(new java.awt.Color(219, 216, 216));
        notesPane.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        notesPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                notesPaneMouseClicked(evt);
            }
        });
        notesScrollPane.setViewportView(notesPane);

        noteButton.setText("Edit Notes");
        noteButton.setToolTipText("Keep Small Notes About Each Item");
        noteButton.setEnabled(false);
        noteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noteButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusPanelLayout.createSequentialGroup()
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(notesScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                    .add(statusPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(jLabel19)
                                    .add(jLabel15)
                                    .add(jLabel20))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(saleField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                                    .add(recvdField)
                                    .add(reorderSpinnerControl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 124, Short.MAX_VALUE)
                                .add(availableCheckBox))
                            .add(statusPanelLayout.createSequentialGroup()
                                .add(0, 257, Short.MAX_VALUE)
                                .add(noteButton)))))
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(saleField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel15))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel19)
                    .add(recvdField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel20)
                    .add(reorderSpinnerControl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(availableCheckBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(notesScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(noteButton)
                .addContainerGap())
        );

        detailTabPane.addTab("Status", statusPanel);

        picturesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        picField.setColumns(50);
        picField.setFont(new java.awt.Font("Courier New", 0, 11)); // NOI18N
        picField.setToolTipText("Picture Reference");
        picField.setNextFocusableComponent(saveButton);

        picButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        picButton.setText("Picture");
        picButton.setToolTipText("Browse to a Picture");
        picButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        picButton.setNextFocusableComponent(picField);
        picButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                picButtonActionPerformed(evt);
            }
        });

        imageList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        imageList.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        imageList.setDropMode(javax.swing.DropMode.INSERT);
        jScrollPane2.setViewportView(imageList);

        org.jdesktop.layout.GroupLayout picturesPanelLayout = new org.jdesktop.layout.GroupLayout(picturesPanel);
        picturesPanel.setLayout(picturesPanelLayout);
        picturesPanelLayout.setHorizontalGroup(
            picturesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(picturesPanelLayout.createSequentialGroup()
                .add(picturesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(picturesPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(picButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(picField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE))
                    .add(jScrollPane2))
                .addContainerGap())
        );
        picturesPanelLayout.setVerticalGroup(
            picturesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(picturesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(picturesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(picButton)
                    .add(picField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                .addContainerGap())
        );

        detailTabPane.addTab("Pictures", picturesPanel);

        savePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        savePanel.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                savePanelKeyPressed(evt);
            }
        });

        helpBox.setEditable(false);
        helpBox.setText("Click the UPC field to start a new record.");

        jToolBar2.setRollover(true);
        jToolBar2.add(filler1);

        clearButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/close_window_32px.png"))); // NOI18N
        clearButton.setToolTipText("Clear/Cancel");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(clearButton);

        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/save_32px.png"))); // NOI18N
        saveButton.setToolTipText("Save changes");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(saveButton);

        org.jdesktop.layout.GroupLayout savePanelLayout = new org.jdesktop.layout.GroupLayout(savePanel);
        savePanel.setLayout(savePanelLayout);
        savePanelLayout.setHorizontalGroup(
            savePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(savePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(savePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, helpBox)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jToolBar2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        savePanelLayout.setVerticalGroup(
            savePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, savePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jToolBar2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(helpBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(detailTabPane)
                    .add(savePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(detailTabPane)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(savePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(jPanel2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void serviceBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serviceBoxActionPerformed
        if (serviceBox.isSelected()) {
            catTextField.setText("Service");
        } else {
            catTextField.setText("");
        }
        checkService();
    }//GEN-LAST:event_serviceBoxActionPerformed

    private void checkService() {

        boolean enabled = true;

        if (serviceBox.isSelected()) {
            enabled = false;
        }

        if (!enabled) {
            sizeTextField.setText("0");
            weightTextField.setText("0");
        }
        catTextField.setEnabled(enabled);

        qtyTextField.setEnabled(enabled);

        sizeTextField.setEnabled(enabled);

        weightTextField.setEnabled(enabled);

        receiveButton.setEnabled(enabled);

    }


    private void receiveModeBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_receiveModeBoxActionPerformed

        findTextField.requestFocus();

    }//GEN-LAST:event_receiveModeBoxActionPerformed

    private void receiveModeBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_receiveModeBoxMouseClicked

        findTextField.requestFocus();

    }//GEN-LAST:event_receiveModeBoxMouseClicked

    private void noteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noteButtonActionPerformed


    }//GEN-LAST:event_noteButtonActionPerformed

    private void calculateTotalInventoryCost() {

        int rows = tm.getRowCount();
        if (rows < 1) {
            return;
        }
        float qty_TMP = 0f;
        float cost_TMP;
        float total = 0f;

        for (int r = 0; r < rows; r++) {
            qty_TMP = (Float) tm.getValueAt(r, 6);//qty
            cost_TMP = (Float) tm.getValueAt(r, 7);//cost per
            if (qty_TMP > 0) {
                total += (qty_TMP * cost_TMP);
            }
        }
    }

    private void groupButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_groupButtonActionPerformed

        int[] a = inventoryTable.getSelectedRows();

        int[] v = new int[a.length];

        for (int i = 0; i < v.length; i++) {

            v[i] = (Integer) inventoryTable.getModel().getValueAt(a[i], 0);

        }
        //new InventoryGroupDialog(null, true, v, workingPath).setVisible(true);


    }//GEN-LAST:event_groupButtonActionPerformed

    private void catTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_catTextFieldKeyPressed

        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {

            picField.requestFocus();

        }


    }//GEN-LAST:event_catTextFieldKeyPressed

    private void savePanelKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_savePanelKeyPressed

        if (inventoryTable.getSelectedRow() > -1) {

            viewPic();

        }

    }//GEN-LAST:event_savePanelKeyPressed

    private void saveImage(InventoryImage image) throws SQLException {

        var isNewInventory = currentItem.getId() == null;

        if (isNewInventory) {
            validateFormAndSave();
        }

        if (!this.currentItem.getImages().contains(image)) {
            this.currentItem.getImages().add(image);
        }

        inventoryImageService.saveIfNewAndlinkImageToInventory(currentItem, image);

        if (isNewInventory && this.currentItem.getId() != null) {
            // inventory was updated
        }

        populateImageListModel();

    }


    private void picButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_picButtonActionPerformed

        try {

            java.io.File file;

            file = new java.io.File(picField.getText());
            JFileChooser fileChooser = new JFileChooser(file);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnVal = fileChooser.showOpenDialog(null);
            java.io.File curFile = fileChooser.getSelectedFile();

            if (returnVal == JFileChooser.CANCEL_OPTION) {
                return;
            }

            if (curFile == null) {
                return;
            }

            var image = Files.readAllBytes(curFile.toPath());
            var inventoryImage = new InventoryImage();
            inventoryImage.setImage(image);
            this.saveImage(inventoryImage);
            picField.setText(DV.verifyPath(picField.getText()));

        } catch (Exception e) {

            javax.swing.JOptionPane.showMessageDialog(null, "There was a problem with the file system.");

        }


    }//GEN-LAST:event_picButtonActionPerformed

    private void viewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewButtonActionPerformed

        if (inventoryTable.getSelectedRow() > -1 && !picField.getText().equals("")) {

            viewPic();

        }

    }//GEN-LAST:event_viewButtonActionPerformed

    private void viewPic() {

        new PictureDialog(null, true, picField.getText(), true);

        inventoryTable.requestFocus();

    }

    private void labelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_labelButtonActionPerformed

        if (inventoryTable.getSelectedRow() > -1) {

            //new InventoryLabelsDialog(null, true, iTable.getModel(), iTable.getSelectedRows(), workingPath, props);
        } else {

            javax.swing.JOptionPane.showMessageDialog(null, "Select rows from the Inventory table to create labels.");

        }


    }//GEN-LAST:event_labelButtonActionPerformed

    private void inventoryTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inventoryTableKeyReleased

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F12) {

            if (!viewButton.isEnabled()) {
                return;
            }

            viewPic();
            return;
        }

        if (inventoryTable.getSelectedRow() > -1) {

            populateFields();

            saveButton.setEnabled(true);

        }


    }//GEN-LAST:event_inventoryTableKeyReleased

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F9 && receiveButton.isEnabled()) {

            receive();

        }
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F11) {

            togglePanels();

        }


    }//GEN-LAST:event_formKeyPressed

    private void receiveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_receiveButtonActionPerformed

        receive();

    }//GEN-LAST:event_receiveButtonActionPerformed

    private void findTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_findTextFieldFocusGained
        findTextField.selectAll();
    }//GEN-LAST:event_findTextFieldFocusGained

    private void togglePanels() {
        if (savePanel.isVisible()) {

            savePanel.setVisible(false);

        } else {

            savePanel.setVisible(true);

        }
        findTextField.requestFocus();
    }

    private void inventoryTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inventoryTableKeyPressed

        if (selectMode) {

            if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                if (inventoryTable.getSelectedRow() > -1 && selectMode) {

                    setReturnValue();

                    this.setVisible(false);
                }
            }
        }

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F9 && receiveButton.isEnabled()) {

            receive();
        }

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_HOME) {

            //new InventoryNoteDialog(null, true, db, (Integer) dataOut[0], descTextField.getText());
        }

    }//GEN-LAST:event_inventoryTableKeyPressed

    private void upcTextFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_upcTextFieldMouseClicked

        if (!upcTextField.isEnabled()) {

//            if (!accessKey.checkInventory(200)) {
//                accessKey.showMessage("Create");
//                return;
//            }

            /* A new record has begun */
            currentItem = new Inventory();
            noteButton.setEnabled(false);
            clearFields();
            setFieldsEnabled(true);
            serviceBox.setEnabled(true);
            saveButton.setEnabled(true);
            upcTextField.requestFocus();

        }
    }//GEN-LAST:event_upcTextFieldMouseClicked

    private void findTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_findTextFieldKeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F9) {

            boolean state = receiveModeBox.isSelected();

            if (state) {

                receiveModeBox.setSelected(false);

            } else {

                receiveModeBox.setSelected(true);

            }

            findTextField.requestFocus();
            return;

        }

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            search();
        }
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F11) {
            togglePanels();
        }
    }//GEN-LAST:event_findTextFieldKeyPressed

    private void voidButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_voidButtonActionPerformed

        returnValue = null;
        setVisible(false);

    }//GEN-LAST:event_voidButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed

//        if (!accessKey.checkInventory(500)) {
//
//            accessKey.showMessage("Delete");
//            return;
//        }
        if (inventoryTable.getSelectedRow() > -1) {

            int a = JOptionPane.showConfirmDialog(this, "Delete Selected Record?", "DELETE", JOptionPane.YES_NO_OPTION);

            if (a == 0) {

                try {
                    inventoryService.deleteInventory(currentItem);
                } catch (SQLException e) {
                    JOptionPane.showConfirmDialog(this, e.getMessage(), "DELETE ERROR", JOptionPane.OK_OPTION);
                }

                refreshTable();
                clearFields();
                this.setFieldsEnabled(false);
                this.saveButton.setEnabled(false);
            }
        }
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        currentItem = new Inventory();
        saveButton.setEnabled(false);
        clearFields();
        serviceBox.setEnabled(false);
        setFieldsEnabled(false);

    }//GEN-LAST:event_clearButtonActionPerformed

    private void validateFormAndSave() {
        try {
            if (validateForm()) {
                saveRecord();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error Saving", JOptionPane.OK_OPTION);
        }

    }

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed

        validateFormAndSave();

    }//GEN-LAST:event_saveButtonActionPerformed

    private void saveRecord() throws SQLException {

        boolean isNewItem = currentItem.getId() == null;

        if (currentItem.getId() == null) {
//            if (!accessKey.checkInventory(200)) {
//                accessKey.showMessage("Create");
//                clearFields();
//                return;
//            }
        }

        if (currentItem.getId() != null) {
//            if (!accessKey.checkInventory(400)) {
//                accessKey.showMessage("Edit");
//                clearFields();
//                return;
//            }
        }

        //call validateForm() before this method
        if (upcTextField.getText().trim().equals("") && codeTextField.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this,
                    "You must enter a UPC or Code. Code is displayed on invoices.",
                    "Need Reference Data", JOptionPane.OK_OPTION);
            return;
        }

        //Make sure the qty, cost & price fields get set to some number if none are entered
        if (!DV.validFloatString(qtyTextField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Make sure QTY is a number.", "Form Problem!", JOptionPane.OK_OPTION);
            return;
        }

        if (!DV.validFloatString(costTextField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Make sure COST is a number.", "Form Problem!", JOptionPane.OK_OPTION);
            return;
        }

        if (!DV.validFloatString(priceTextField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Make sure PRICE is a number.", "Form Problem!", JOptionPane.OK_OPTION);
            return;
        }

        currentItem.setUpc(upcTextField.getText().trim());
        currentItem.setCode(codeTextField.getText().trim());
        currentItem.setDescription(descTextField.getText().trim());
        currentItem.setSize(sizeTextField.getText().trim());
        currentItem.setWeight(weightTextField.getText().trim());
        currentItem.setQuantity(new BigDecimal(qtyTextField.getText().trim()).doubleValue());
        currentItem.setCost(new BigDecimal(costTextField.getText().trim()).doubleValue());
        currentItem.setPrice(new BigDecimal(priceTextField.getText().trim()).doubleValue());
        currentItem.setTax1(taxCheckBox.isSelected());
        currentItem.setTax2(tax2CheckBox.isSelected());
        currentItem.setPartialSaleAllowed(partialBox.isSelected());
        currentItem.setAvailable(availableCheckBox.isSelected());
        currentItem.setLastSale(new Date());
        currentItem.setLastReceived(new Date());
        currentItem.setReorderCutoff((Integer) reorderSpinnerControl.getValue());
        inventoryService.save(currentItem);

        // TODO: is there anything to do for images here? any impact from saving the inventory item early when adding images?
        // reflect changes for the user and cleanup UI
        updateTableModel(currentItem, isNewItem);
        saveButton.setEnabled(false);
        clearFields();
        setFieldsEnabled(false);
        changeSelectionTo(currentItem);
        currentItem = new Inventory();
    }

    private void changeSelectionTo(Inventory inventory) {
        var tableModel = (InventoryTableModel) inventoryTable.getModel();
        int row = tableModel.getCollection().indexOf(inventory);
        inventoryTable.changeSelection(row, 0, false, false);
    }

    private void updateTableModel(Inventory inventory, boolean isNewItem) {
        int changeRow = inventoryTable.getSelectedRow();
        var tableModel = (InventoryTableModel) inventoryTable.getModel();
        if (isNewItem) {
            tableModel.setValueAt(inventory);
        } else {
            tableModel.setValueAt(changeRow, inventory);
        }
    }

    private boolean validateForm() throws SQLException {

        if (DV.validFloatString(costTextField.getText().trim())); else {
            JOptionPane.showMessageDialog(this, "Make sure Cost is a valid number (0.00). ", "Form Problem!", JOptionPane.OK_OPTION);
            return false;
        }
        if (DV.validFloatString(priceTextField.getText().trim())); else {
            JOptionPane.showMessageDialog(this, "Make sure Price is a valid number (0.00). ", "Form Problem!", JOptionPane.OK_OPTION);
            return false;
        }

        if (codeTextField.getText().trim().equalsIgnoreCase("return")) {

            JOptionPane.showMessageDialog(this, "The word 'return' cannot be used for a Code!", "Invalid Code", JOptionPane.OK_OPTION);
            return false;

        }
        var withDescription = inventoryService.getAllInventoryByDecription(descTextField.getText());

        if (currentItem.getId() != null && withDescription.size() > 0) {
            var descWithDiffId
                    = withDescription.stream().filter(x -> !x.getId().equals(currentItem.getId())).count();
            if (descWithDiffId > 0) {
                JOptionPane.showMessageDialog(this, "A record with this DESC. already exsists.", "No Duplicates", JOptionPane.OK_OPTION);
                return false;
            }
        }

        var withCode = inventoryService.getAllInventoryByCode(codeTextField.getText());

        if (withCode.size() > 0) {
            if (withCode.size() > 1) {
                JOptionPane.showMessageDialog(this, "Multiple records with this CODE already exsist. (2 Maximum)", "No More Duplicates", JOptionPane.OK_OPTION);
                return false;
            }

            var codeWithDiffId
                    = withCode.stream().filter(x -> !x.getId().equals(currentItem.getId())).count();
            if (codeWithDiffId > 0) {
                int a = JOptionPane.showConfirmDialog(this, "A record with this CODE already exists, is this OK ?", "WARNING", JOptionPane.YES_NO_OPTION);
                if (a == 0); else {
                    return false;
                }
            }
        }

        /* Look for duplicate UPC */
        var withUpc = inventoryService.getAllInventoryByUpc(upcTextField.getText());
        if (withUpc.size() > 0) {
            if (withUpc.size() > 1) {
                JOptionPane.showMessageDialog(this, "Multiple records with this UPC already exsist. (2 Maximum)", "No More Duplicates", JOptionPane.OK_OPTION);
                return false;
            }

            var upcWithDiffId
                    = withUpc.stream().filter(x -> !x.getId().equals(currentItem.getId())).count();
            if (upcWithDiffId > 0) {
                int a = JOptionPane.showConfirmDialog(this, "A record with this UPC already exists, is this OK ?", "WARNING", JOptionPane.YES_NO_OPTION);
                if (a == 0); else {
                    return false;
                }
            }
        }
        return true;
    }


    private void selectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectButtonActionPerformed

        if (inventoryTable.getSelectedRow() > -1) {
            setReturnValue();
            this.setVisible(false);
        }

    }//GEN-LAST:event_selectButtonActionPerformed

    private void notesPaneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesPaneMouseClicked

        if (evt.getClickCount() == 2) {
            int tableRow = inventoryTable.getSelectedRow();
            if (tableRow > 0) {
                this.populateFields();
                int k = (Integer) inventoryTable.getModel().getValueAt(tableRow, 0);
                //new InventoryNoteDialog(null, true, db, (Integer) dataOut[0], descTextField.getText());
                this.getNote(k);
            }
        }
    }//GEN-LAST:event_notesPaneMouseClicked

    private void priceTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_priceTextFieldFocusGained
        calculateMarkup();
        selectAll(evt);
    }//GEN-LAST:event_priceTextFieldFocusGained

    private void formKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyTyped

    }//GEN-LAST:event_formKeyTyped

    private void upcTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_upcTextFieldFocusGained
        selectAll(evt);
    }//GEN-LAST:event_upcTextFieldFocusGained

    private void codeTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_codeTextFieldFocusGained
        selectAll(evt);
    }//GEN-LAST:event_codeTextFieldFocusGained

    private void sizeTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_sizeTextFieldFocusGained
        selectAll(evt);
    }//GEN-LAST:event_sizeTextFieldFocusGained

    private void weightTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_weightTextFieldFocusGained
        selectAll(evt);
    }//GEN-LAST:event_weightTextFieldFocusGained

    private void costTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_costTextFieldFocusGained
        selectAll(evt);
    }//GEN-LAST:event_costTextFieldFocusGained

    private void catTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_catTextFieldFocusGained
        selectAll(evt);
    }//GEN-LAST:event_catTextFieldFocusGained

    private void searchFieldComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchFieldComboActionPerformed
        findTextField.requestFocus();
    }//GEN-LAST:event_searchFieldComboActionPerformed

    private void qtyTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_qtyTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_qtyTextFieldActionPerformed

    private void partialBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_partialBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_partialBoxActionPerformed

    private void inventoryTableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_inventoryTableMousePressed
        int mouseButton = evt.getButton();
        if (mouseButton == evt.BUTTON2 || mouseButton == evt.BUTTON3) {
            return;
        }

        if (evt.getClickCount() == 1) { // TODO: make sure all click handlers are only populating on the first click
            populateFields();
            saveButton.setEnabled(true);
        }

        if (evt.getClickCount() == 2) {

            if (selectMode) {

                int row = inventoryTable.rowAtPoint(new Point(evt.getX(), evt.getY()));

                if (inventoryTable.getSelectedRow() > -1) {
                    returnValue = new ArrayList();
                    var tableModel = (InventoryTableModel) this.inventoryTable.getModel();
                    returnValue.add((Inventory) tableModel.getValueAt(row));
                    this.setVisible(false);
                }

            }

        }

    }//GEN-LAST:event_inventoryTableMousePressed

    private void selectAll(java.awt.event.FocusEvent evt) {
        JTextField tf = (JTextField) evt.getSource();
        tf.selectAll();
    }

    private void setReturnValue() {

        var selectedRowIndexes = inventoryTable.getSelectedRows();
        var tableModel = (InventoryTableModel) inventoryTable.getModel();
        returnValue = new ArrayList();

        for (int i = 0; i < selectedRowIndexes.length; i++) {
            returnValue.add((Inventory) tableModel.getValueAt(selectedRowIndexes[i]));
        }
    }

    /* NetBeans generated privates */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel assField;
    private javax.swing.JCheckBox availableCheckBox;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextField catTextField;
    private javax.swing.JButton clearButton;
    private javax.swing.JTextField codeTextField;
    private javax.swing.JTextField costTextField;
    private javax.swing.JLabel costTotalLabel;
    private javax.swing.JButton deleteButton;
    private javax.swing.JTextField descTextField;
    private javax.swing.JPanel detailPanel;
    private javax.swing.JTabbedPane detailTabPane;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JTextField findTextField;
    private javax.swing.JButton groupButton;
    private javax.swing.JTextField helpBox;
    private javax.swing.JList<InventoryImage> imageList;
    private javax.swing.JTable inventoryTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JButton labelButton;
    private javax.swing.JButton noteButton;
    private javax.swing.JTextPane notesPane;
    private javax.swing.JScrollPane notesScrollPane;
    private javax.swing.JCheckBox partialBox;
    private javax.swing.JButton picButton;
    private javax.swing.JTextField picField;
    private javax.swing.JPanel picturesPanel;
    private javax.swing.JTextField priceTextField;
    private javax.swing.JTextField qtyTextField;
    private javax.swing.JButton receiveButton;
    private javax.swing.JCheckBox receiveModeBox;
    private javax.swing.JTextField recvdField;
    private javax.swing.JSpinner reorderSpinnerControl;
    private javax.swing.JLabel retailTotalLabel;
    private javax.swing.JTextField saleField;
    private javax.swing.JButton saveButton;
    private javax.swing.JPanel savePanel;
    private javax.swing.JComboBox searchFieldCombo;
    private javax.swing.JButton selectButton;
    private javax.swing.JCheckBox serviceBox;
    private javax.swing.JTextField sizeTextField;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JPanel tablePanel;
    private javax.swing.JCheckBox tax2CheckBox;
    private javax.swing.JCheckBox taxCheckBox;
    private javax.swing.JTextField upcTextField;
    private javax.swing.JButton viewButton;
    private javax.swing.JButton voidButton;
    private javax.swing.JTextField weightTextField;
    // End of variables declaration//GEN-END:variables

}
