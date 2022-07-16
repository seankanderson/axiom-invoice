/*
 * invDialog.java
 *
 * Created on June 25, 2006, 11:16 AM
 **
 */
package com.datavirtue.axiom.ui.invoices;

import com.datavirtue.axiom.services.PosPrinterService;
import com.datavirtue.axiom.ui.util.Tools;
import com.datavirtue.axiom.database.reports.ReportModel;
import com.datavirtue.axiom.ui.util.JTextFieldFilter;
import com.datavirtue.axiom.ui.contacts.ContactsApp;
import com.datavirtue.axiom.ui.contacts.ContactShippingDialog;
import com.datavirtue.axiom.ui.util.NewEmail;
import com.datavirtue.axiom.ui.VATCalculator;
import com.datavirtue.axiom.ui.inventory.InventoryApp;
import java.beans.PropertyVetoException;
import javax.swing.JTextField;
import javax.swing.table.*;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Date;
import java.awt.event.*;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import com.datavirtue.axiom.models.contacts.Contact;
import com.datavirtue.axiom.models.contacts.ContactAddressInterface;
import com.datavirtue.axiom.models.inventory.Inventory;
import com.datavirtue.axiom.models.invoices.Invoice;
import com.datavirtue.axiom.models.invoices.InvoiceItem;
import com.datavirtue.axiom.models.invoices.InvoiceItemsTableModel;
import com.datavirtue.axiom.models.settings.AppSettings;
import com.datavirtue.axiom.services.AppSettingsService;
import com.datavirtue.axiom.services.BarcodeService;
import com.datavirtue.axiom.services.ContactService;
import com.datavirtue.axiom.services.DiService;
import com.datavirtue.axiom.services.ExceptionService;
import com.datavirtue.axiom.services.ImageService;
import com.datavirtue.axiom.services.InventoryService;
import com.datavirtue.axiom.services.InvoiceItemService;
import com.datavirtue.axiom.services.InvoiceService;
import com.datavirtue.axiom.services.LocalSettingsService;
import com.datavirtue.axiom.services.UserService;
import com.datavirtue.axiom.services.util.CurrencyUtil;
import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.services.util.LinePrinter;
import com.datavirtue.axiom.ui.AxiomApp;
import com.datavirtue.axiom.ui.EnhancedTableCellRenderer;
import com.datavirtue.axiom.ui.util.DecimalCellRenderer;
import com.google.zxing.WriterException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.sql.SQLException;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.stream.Collectors;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import freemarker.template.*;
import java.awt.Desktop;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import net.sourceforge.barbecue.BarcodeException;
import net.sourceforge.barbecue.output.OutputException;

/**
 *
 * @author Sean K Anderson - Data Virtue
 * @rights Copyright Data Virtue 2006, 2007, 2008, 2009, 2022 All Rights
 * Reserved.
 */
public class InvoiceApp extends javax.swing.JDialog implements AxiomApp {

    private boolean addCategoryInfo = false;
    private Invoice currentInvoice = new Invoice();
    private Contact customer;
    private AppSettingsService settingsService;
    private AppSettings appSettings;
    private InvoiceService invoiceService;
    private InventoryService inventoryService;
    private InvoiceItemService itemService;
    private ContactService contactService;
    private ImageService imageService;
    private BarcodeService barcodeService;

    private String invoiceMessage = "Thank You!";
    private javax.swing.table.DefaultTableModel tm;
    private java.awt.Frame parentWin;
    private String nl = System.getProperty("line.separator");
    private boolean viewPrint = false;

    private java.awt.Image winIcon;

    public InvoiceApp(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        parentWin = parent;
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {

                try {
                    recordWindowSizeAndPosition();
                } catch (BackingStoreException ex) {
                    ExceptionService.showErrorDialog(e.getComponent(), ex, "Error saving local screen preferences");
                }
            }
        });
        initComponents();
    }

    /**
     *
     * Pass in invoice before calling displayApp() to load an existing invoice.
     *
     * @param invoice
     */
    public void setInvoice(Invoice invoice) {
        this.currentInvoice = null;
        this.currentInvoice = invoice;
    }

    public void displayApp() {

        var injector = DiService.getInjector();
        invoiceService = injector.getInstance(InvoiceService.class);
        inventoryService = injector.getInstance(InventoryService.class);
        itemService = injector.getInstance(InvoiceItemService.class);
        settingsService = injector.getInstance(AppSettingsService.class);
        settingsService.setObjectType(AppSettings.class);
        imageService = injector.getInstance(ImageService.class);
        barcodeService = injector.getInstance(BarcodeService.class);
        contactService = injector.getInstance(ContactService.class);
        
        var user = UserService.getCurrentUser();

        if (!user.isAdmin() && user.getInvoices() < 300) {
            JOptionPane.showMessageDialog(this, "Please see the admin about permissions.", "Access denied", JOptionPane.OK_OPTION);
            this.dispose();
            return;
        }

        try {
            this.initCommon();
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching local settings");
        }

        if (currentInvoice.getId() == null) {
            this.initForNewInvoice();
        } else {
            if (currentInvoice.isQuote()) {
                this.initForViewQuote();
            } else {
                this.initForViewInvoice();
            }
            this.modelToView();
        }

        try {
            restoreSavedWindowSizeAndPosition();
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching local screen settings");
        }
        this.qtyTextField.setText("1.00");
        this.setVisible(true);

    }

    private void recordWindowSizeAndPosition() throws BackingStoreException {
        var localSettings = LocalSettingsService.getLocalAppSettings();
        var screenSettings = localSettings.getScreenSettings();
        var sizeAndPosition = LocalSettingsService.getWindowSizeAndPosition(this);
        screenSettings.setInvoices(sizeAndPosition);
        LocalSettingsService.saveLocalAppSettings(localSettings);
    }

    private void restoreSavedWindowSizeAndPosition() throws BackingStoreException {
        var localSettings = LocalSettingsService.getLocalAppSettings();
        var screenSettings = localSettings.getScreenSettings().getInvoices();
        LocalSettingsService.applyScreenSizeAndPosition(screenSettings, this);
    }

    /**
     * All required setup for creating / viewing invoices and quotes.
     */
    private void initCommon() throws BackingStoreException {

        if (this.currentInvoice.getItems() == null) {
            this.currentInvoice.setItems(new ArrayList<InvoiceItem>());
        }
        var tableModel = new InvoiceItemsTableModel(new ArrayList(this.currentInvoice.getItems()));
        this.invoiceItemsTable.setModel(tableModel);
        
        try {
            appSettings = this.settingsService.getObject();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching app settings from database");
        }

        if (currentInvoice == null) {
            currentInvoice = new Invoice();
        }
        optionsToolbar.setLayout(new FlowLayout());

        Toolkit tools = Toolkit.getDefaultToolkit();
        winIcon = tools.getImage(getClass().getResource("/Orange.png"));

        qtyTextField.setDocument(new JTextFieldFilter(JTextFieldFilter.FLOAT));
        addCategoryInfo = appSettings.getInventory().isAddCategoryLineToInvoiceItems();
        custButton.setText(appSettings.getInvoice().getBillToLabel());

        paymentCheckBox.setSelected(appSettings.getInvoice().isProcessPaymentOnPosting());

        /* Set currency field alignment justification */
        itemTotalField.setHorizontalAlignment(JTextField.RIGHT);
        t1Field.setHorizontalAlignment(JTextField.RIGHT);
        t2Field.setHorizontalAlignment(JTextField.RIGHT);
        grandTotalField.setHorizontalAlignment(JTextField.RIGHT);

        if (!appSettings.getInvoice().isShowTax2()) {
            t2Field.setVisible(false);
            t2Label.setVisible(false);
        }

        this.VATButton.setVisible(appSettings.getInvoice().getTax1Name().equalsIgnoreCase("vat"));

        String scanField = appSettings.getInvoice().getDefaultBarcodeScanField();
        if (scanField.equalsIgnoreCase("UPC")) {
            upcCombo.setSelectedItem("UPC");
        }
        if (scanField.equalsIgnoreCase("CODE")) {
            upcCombo.setSelectedItem("Code");
        }
        if (scanField.equalsIgnoreCase("DESC")) {
            upcCombo.setSelectedItem("Desc");
        }

        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        datePicker1.setDateFormat(df);

        this.invoiceItemsTable.setDefaultRenderer(Object.class, new EnhancedTableCellRenderer());

        setDefaultMessage();
        computePrices();
        this.customizeView();
    }

    /**
     * Required setup for only viewing a saved quote
     */
    private void initForViewQuote() {
        toolBar.setVisible(false);
        printReceiptButton.setVisible(false);
        //currentInvoice.setCustomer("S A L E");
        invoiceNumberEditCheckBox.setEnabled(false);
        convertButton.setVisible(true);
        postButton.setEnabled(false);
        closeButton.setText("CLOSE");
        clearFields();
        viewReturnsButton.setVisible(false);
    }

    /**
     * Required setup for new invoice that may become a quote
     */
    private void initForNewInvoice() {
        /* Close dialog on escape */
        javax.swing.ActionMap am = getRootPane().getActionMap();
        javax.swing.InputMap imap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object windowCloseKey = new Object();
        KeyStroke windowCloseStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        Action windowCloseAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                int a = javax.swing.JOptionPane.showConfirmDialog(null, "Discard this invoice?", "Exit", JOptionPane.YES_NO_OPTION);

                if (a == 0) {
                    setVisible(false);
                    dispose();
                }
                return;  //cancels the exit

            }
        };
        imap.put(windowCloseStroke, windowCloseKey);
        am.put(windowCloseKey, windowCloseAction);
        /**/

        if (appSettings.getInvoice().isPointOfSaleMode()) {
            receiptCheckBox.setSelected(true);
            paymentCheckBox.setSelected(true);
            receiptCheckBox.setEnabled(false);
            paymentCheckBox.setEnabled(false);
            jPanel2.setVisible(false);
            packingslipButton.setVisible(false);
            shippingButton.setVisible(false);
            printButton.setVisible(false);
            saveButton.setVisible(false);
            viewReturnsButton.setVisible(false);
        }

        this.setAddressView(currentInvoice.getBillTo(), custTextArea);
        
        convertButton.setVisible(false);
        printButton.setVisible(false);
        setInvoiceNumber(this.currentInvoice);
    }

    /**
     * Required setup for viewing posted/saved invoices
     */
    private void initForViewInvoice() {

        toolBar.add(paymentButton);
        toolBar.add(statementButton);
        toolBar.add(historyButton);

        javax.swing.ActionMap am = getRootPane().getActionMap();
        javax.swing.InputMap imap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object windowCloseKey = new Object();
        KeyStroke windowCloseStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        Action windowCloseAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        imap.put(windowCloseStroke, windowCloseKey);
        am.put(windowCloseKey, windowCloseAction);

        autoInvoiceNumberButton.setEnabled(false);
        invoiceNumberEditCheckBox.setEnabled(false);
        datePicker1.setEnabled(false);
        receiptCheckBox.setEnabled(false);

        saveButton.setEnabled(false);
        postButton.setEnabled(false);
        upcCombo.setEnabled(false);
        upcField.setEnabled(false);
        miscButton.setVisible(false);
        calcButton.setVisible(false);
        removeButton.setVisible(false);
        custButton.setVisible(false);
        discountButton.setVisible(false);
        messageButton.setVisible(false);
        paymentCheckBox.setVisible(false);
        custTextArea.setEditable(false);
        shipToTextArea.setEditable(false);
        shipToButton.setVisible(false);
        clearShipToButton.setVisible(false);
        copyBillToButton.setVisible(false);
        convertButton.setVisible(false);
        qtyTextField.setEditable(false);
        messageButton.setEnabled(false);
        closeButton.setText("CLOSE");

//        if (invoice.getReturnCount() > 0) {
//            viewReturnsButton.setVisible(true);
//        } else {
//            viewReturnsButton.setVisible(false);
//        }
    }

    private void setDefaultMessage() {
        //get default name
        //search for mesage
        //if found set, otherwise leave program default in place.

//        String msg = props.getProp("DEFAULT MSG");
//
//        if (msg != null && msg.length() > 0) {
//            ArrayList al = db.search("messages", 1, msg, false);
//            if (al != null) {
//                Object[] m = db.getRecord("messages", ((Integer) al.get(0)));
//                invoiceMessage = (String) m[2];
//
//            } else {
//
//            }
//
//        }
//
//        messageButton.setToolTipText(invoiceMessage);
    }

    private void modelToView() {

        documentNumberField.setText(currentInvoice.getInvoiceNumber());

        InvoiceItemsTableModel tableModel = new InvoiceItemsTableModel(new ArrayList(currentInvoice.getItems()));
        invoiceItemsTable.setModel(tableModel);

        try {

            datePicker1.setDate(currentInvoice.getInvoiceDate());

        } catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
        
        this.setAddressView(currentInvoice.getBillTo(), custTextArea);
        this.setAddressView(currentInvoice.getShiptTo(), shipToTextArea);

        customizeView();
        computePrices();

    }

    public void viewToModel() {

        this.currentInvoice.setInvoiceNumber(this.documentNumberField.getText());

        this.currentInvoice.setInvoiceDate(datePicker1.getDate());

        this.currentInvoice.setMessage(invoiceMessage);

        this.currentInvoice.setPaid(false);

        if (this.customer != null) {
            this.currentInvoice.setCustomerId(this.customer.getId());
        }

        var tableModel = (InvoiceItemsTableModel) this.invoiceItemsTable.getModel();

        this.currentInvoice.setItems(tableModel.getCollection());
        
    }

    private boolean printReciept() {

        String currency = appSettings.getInvoice().getCurrencySymbol();

        double mm;

        try {

            mm = appSettings.getInvoice().getRecieptPaperWidthInMm();

        } catch (Exception e) {

            mm = 80;  //default
        }

        double widthInInches = mm * PosPrinterService.metricToInchesConversionFactor;

        PosPrinterService posPrinter = new PosPrinterService(new java.awt.Font("Courier", java.awt.Font.BOLD, 8), true, widthInInches);
        var company = appSettings.getCompany();
        posPrinter.newLine();
        posPrinter.newLine();
        posPrinter.addLine(company.getCompanyName());
        posPrinter.addLine(company.getAddress1());
        posPrinter.addLine(company.getAddress2());
        posPrinter.addLine(company.getCity() + "  " + company.getState() + "  " + company.getPostalCode());
        posPrinter.addLine(company.getPhoneNumber());

        posPrinter.newLine();
        posPrinter.newLine();

        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();

        ReportModel rm = new ReportModel(invoiceItemsTable.getModel());

        String spc = " ";
        String bite = "";

        posPrinter.addLine(DV.addSpace("QTY   CODE ", 32, ' ') + "PRICE");
        posPrinter.addLine("ITEM DESCRIPTION");
        posPrinter.addLine(DV.addSpace("", 40, '-'));

        do {

            for (int i = 2; i < 6; i++) {   //start grabbing data after invoice and inventory keys

                spc = " ";

                if (i == 2) {
                    spc = "";
                }

                bite = rm.getValueAt(i);

                if (i == 2 || i == 3 || i == 5) {

                    //add formatting crap
                    if (i == 3) {
                        // Add spaces to line up prices
                        bite = DV.addSpace(bite, 27, ' ');
                        // chop the description down
                        //if (bite.length() > 26) bite = bite.substring(0, 25);
                    }
                    if (i == 2) {
                        bite = bite + ' ';
                    }
                    line1.append(bite);
                }
                if (i == 4) {
                    line2.append(bite);
                }


                /*
                     
                     if (i == 4){
                         // Add spaces to line up prices
                         bite = DV.padString(bite, 26);
                         // chop the description down
                         if (bite.length() > 26) bite = bite.substring(0, 25);
                     }
                     sb.append(spc + bite + spc);*/
                //if col 3 addline
            }

            posPrinter.addLine(line1.toString());
            posPrinter.addLine(line2.toString());
            posPrinter.addLine(" ");
            line1 = new StringBuilder();
            line2 = new StringBuilder();

        } while (rm.next());

        posPrinter.addLine(DV.addSpace("", 40, '-'));
        posPrinter.newLine();
        posPrinter.addLine("Item Total: " + currency + " " + itemTotalField.getText());
        posPrinter.addLine(t1Label.getText() + " Total: " + currency + " " + t1Field.getText());
        posPrinter.addLine(t2Label.getText() + " Total: " + currency + " " + t2Field.getText());
        posPrinter.addLine("GrandTotal: " + currency + " " + grandTotalField.getText());
        posPrinter.newLine();
        posPrinter.newLine();
        posPrinter.addLine("Reference Number: " + documentNumberField.getText());
        Calendar cal = Calendar.getInstance();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,
                DateFormat.MEDIUM);
        posPrinter.addLine(df.format(cal.getTime()));
        posPrinter.go();

        return false;

    }

    private boolean print(boolean q) {

        //hydrateInvoiceAndcomputePrices();
        // testPriiintPdf()
        return true;

    }

    private void customizeView() {

        clearFields();

        invoiceItemsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        // TODO : resize only certain columns such as description

        TableColumn col = invoiceItemsTable.getColumnModel().getColumn(0);
        col.setPreferredWidth(20);

        col = invoiceItemsTable.getColumnModel().getColumn(1);
        col.setPreferredWidth(30);

        col = invoiceItemsTable.getColumnModel().getColumn(2);
        col.setPreferredWidth(300);

        col = invoiceItemsTable.getColumnModel().getColumn(3);
        col.setPreferredWidth(30);

        //tax
        col = invoiceItemsTable.getColumnModel().getColumn(4);
        col.setPreferredWidth(30);

        col = invoiceItemsTable.getColumnModel().getColumn(5);
        col.setPreferredWidth(30);

        col = invoiceItemsTable.getColumnModel().getColumn(6);
        col.setPreferredWidth(30);

        var tax1Name = appSettings.getInvoice().getTax1Name();
        var tax2Name = appSettings.getInvoice().getTax2Name();
        invoiceItemsTable.getColumn(invoiceItemsTable.getColumnName(4)).setHeaderValue(tax1Name);
        invoiceItemsTable.getColumn(invoiceItemsTable.getColumnName(5)).setHeaderValue(tax2Name);

        invoiceItemsTable.getColumnModel().getColumn(0).setCellRenderer(new DecimalCellRenderer(18, 2, SwingConstants.RIGHT));
        invoiceItemsTable.getColumnModel().getColumn(3).setCellRenderer(new DecimalCellRenderer(18, 2, SwingConstants.RIGHT));
        invoiceItemsTable.getColumnModel().getColumn(5).setCellRenderer(new DecimalCellRenderer(18, 2, SwingConstants.RIGHT));

        t1Label.setText(tax1Name);
        t2Label.setText(tax2Name);

        this.invoiceItemsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.invoiceItemsTable.setRowSelectionAllowed(true);
        this.invoiceItemsTable.setColumnSelectionAllowed(false);

        if (!appSettings.getInvoice().isShowTax2()) {
            var columnModel = this.invoiceItemsTable.getColumnModel();
            columnModel.removeColumn(columnModel.getColumn(5));
        }

    }

    private void clearFields() {
        qtyTextField.setText("1.00");
        upcField.setText("");
        upcField.requestFocus();
    }

    private void computePrices() {

        var totals = InvoiceService.calculateInvoiceTotals(currentInvoice);
        t1Field.setText(CurrencyUtil.money(totals.getTax1Total()));
        t2Field.setText(CurrencyUtil.money(totals.getTax2Total()));

        itemTotalField.setText(CurrencyUtil.money(totals.getSubTotal()));

        this.discountTextField.setText(CurrencyUtil.money(totals.getPretaxDiscounts()));
        this.taxableTotalField.setText(CurrencyUtil.money(totals.taxable1Subtotal));
        grandTotalField.setText(CurrencyUtil.money(totals.getGrandTotal()));
    }

    private void miscAction() {

        var miscItemDialog = new MiscItemDialog(null, true, null);

        try {
            miscItemDialog.displayApp();
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error getting local settings");
            return;
        }

        var miscItem = miscItemDialog.getItem();
        miscItem.setQuantity(DV.parseDouble(qtyTextField.getText()));
        miscItem.setTaxable1Rate(appSettings.getInvoice().getTax1Rate());
        miscItem.setTaxable2Rate(appSettings.getInvoice().getTax2Rate());
        if (miscItem != null) {
            this.addItemToInvoiceItemsTable(miscItem);
        }

        miscItemDialog.dispose();
        computePrices();
        upcField.requestFocus();
    }

    /**
     * @param inventory inventory item to check against business logic
     * @param qtyNeeded qty the user wishes to sell
     * @param checkAvailability true
     * @return
     */
    private boolean canInventoryBeSold(Inventory inventory, double qtyNeeded, boolean checkAvailability) throws SQLException {

        if (checkAvailability) {

            if (!inventory.isAvailable()) {

                int a = JOptionPane.showConfirmDialog(this, inventory.getDescription() + " is marked as unavailable. " + nl
                        + "Would you like to sell it anyway?", "Item unavailable", JOptionPane.YES_NO_OPTION);
                if (a == JOptionPane.NO_OPTION) {
                    return false;
                }
            }

            var qtyAvailable = inventoryService.quantityAvailableNow(inventory.getId());
            if (qtyNeeded > qtyAvailable && !inventory.getCategory().equalsIgnoreCase("Service")) {
                if (!appSettings.getInventory().isIgnoreQuantityWarnings()) {
                    int a = JOptionPane.showConfirmDialog(this, "Not enough in stock to complete the sale. Available: " + qtyAvailable + nl
                            + "Would you like to add " + '"' + inventory.getDescription() + '"' + " anyway?", "Inventory level warning", JOptionPane.YES_NO_OPTION);
                    if (a == JOptionPane.NO_OPTION) {
                        return false;
                    }
                }
            }
        }

        if (!appSettings.getInventory().isIgnoreQuantityWarnings()) {

            if (!inventory.isPartialSaleAllowed() && Tools.isDecimal(qtyNeeded)) {
                JOptionPane.showMessageDialog(null,
                        inventory.getDescription() + " cannot be sold in partial quantities.");
                return false;
            }
        }

        return true;
    }

    private boolean canItemBeSold(InvoiceItem item) throws SQLException {

        var inventoryId = item.getSourceInventoryId();
        var inventory = inventoryService.getInventoryById(inventoryId);
        return canInventoryBeSold(inventory, item.getQuantity(), false);

    }

    private void addInvoiceItemFromInventory(double quantityRequired, Inventory inventory) {

        if (Tools.isDecimal(quantityRequired) && !inventory.isPartialSaleAllowed()) {
            javax.swing.JOptionPane.showMessageDialog(null, inventory.getDescription() + " cannot be sold in partial decimal quantities.");
            return;
        }

        if (!appSettings.getInventory().isIgnoreQuantityWarnings()
                && checkAvailability(inventory, quantityRequired)) {
            var invoiceItem = this.mapInventoryToInvoiceItem(quantityRequired, inventory);
            this.addItemToInvoiceItemsTable(invoiceItem);
        }
    }

    private int addItemToInvoiceItemsTable(InvoiceItem newItem) {
        newItem.setInvoice(currentInvoice);
        var items = (List<InvoiceItem>) this.currentInvoice.getItems();

        // update existing item instead of appending if there are existing items
        if (items != null && items.size() > 0) {
            for (int index = 0; index < items.size(); index++) {
                var item = items.get(index);
                if (item.getDescription().equals(newItem.getDescription())) {
                    item.setQuantity(item.getQuantity() + newItem.getQuantity());
                    return index;
                }
            }
        }

        if (items == null) {
            items = new ArrayList<InvoiceItem>();
        }
        items.add(newItem);

        var tableModel = new InvoiceItemsTableModel(items);
        this.invoiceItemsTable.setModel(tableModel);
        this.customizeView();
        
        this.invoiceItemsTable.repaint();

        return (items.size() - 1);
    }

    private InvoiceItem mapInventoryToInvoiceItem(double qty, Inventory inventory) {

        try {
            return itemService.mapInventoryToInvoiceItem(qty, currentInvoice, inventory);
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error getting tax settings from database");
            return null;
        }
    }

    private boolean checkAvailability(Inventory inventory, double quantityRequired) {

        if (!inventory.isAvailable()) {
            int a = JOptionPane.showConfirmDialog(this, inventory.getDescription() + " is marked as unavailable. " + nl
                    + "Would you like to sell it anyway?", "Item unavailable", JOptionPane.YES_NO_OPTION);

            if (a == 1) {
                return false;
            }
        }

        if (quantityRequired > inventory.getQuantity()) {
            int a = JOptionPane.showConfirmDialog(this, "Not enough in stock to complete the sale. Available: " + inventory.getQuantity() + nl
                    + "Would you like to add " + '"' + quantityRequired + '"' + " anyway?", "Inventory warning", JOptionPane.YES_NO_OPTION);
            if (a == 1) {
                return false;
            }
        }

        if ((inventory.getQuantity() - quantityRequired) <= inventory.getReorderCutoff()
                && !(quantityRequired > inventory.getQuantity())) {
            javax.swing.JOptionPane.showMessageDialog(null, inventory.getDescription()
                    + " quantity on hand has met, or has gone below, the reorder cut-off amount defined for it.");
        }

        return true;
    }

    private void addItem(Inventory inventory) throws SQLException {

        double quantity = DV.parseDouble(qtyTextField.getText());

        if (!inventory.getCode().trim().equals("MISC") && !inventory.getCode().trim().equals("DISC")) {
            if (!canInventoryBeSold(inventory, quantity, true)) {
                return;
            }
        }

        var rowIndex = addItemToInvoiceItemsTable(this.mapInventoryToInvoiceItem(quantity, inventory));
        if (rowIndex > 0) {
            invoiceItemsTable.changeSelection(rowIndex, 0, false, false);
        }

        computePrices();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jPanel2 = new javax.swing.JPanel();
        custButton = new javax.swing.JButton();
        documentNumberField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        custTextArea = new javax.swing.JTextArea();
        logoPanel = new javax.swing.JPanel();
        datePicker1 = new com.michaelbaranov.microba.calendar.DatePicker();
        jScrollPane4 = new javax.swing.JScrollPane();
        shipToTextArea = new javax.swing.JTextArea();
        shipToButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        invoiceNumberEditCheckBox = new javax.swing.JCheckBox();
        autoInvoiceNumberButton = new javax.swing.JButton();
        convertButton = new javax.swing.JButton();
        copyBillToButton = new javax.swing.JButton();
        clearShipToButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        invoiceItemsTable = new javax.swing.JTable();
        upcField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        optionsToolbar = new javax.swing.JToolBar();
        messageButton = new javax.swing.JButton();
        viewReturnsButton = new javax.swing.JButton();
        printButton = new javax.swing.JButton();
        printReceiptButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        itemTotalField = new javax.swing.JTextField();
        grandTotalField = new javax.swing.JTextField();
        jToolBar1 = new javax.swing.JToolBar();
        calcButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        postButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        jToolBar3 = new javax.swing.JToolBar();
        receiptCheckBox = new javax.swing.JCheckBox();
        paymentCheckBox = new javax.swing.JCheckBox();
        discountTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        taxableTotalField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        t1Field = new javax.swing.JTextField();
        t2Field = new javax.swing.JTextField();
        t1Label = new javax.swing.JLabel();
        t2Label = new javax.swing.JLabel();
        toolBar = new javax.swing.JToolBar();
        paymentButton = new javax.swing.JButton();
        statementButton = new javax.swing.JButton();
        historyButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        datePicker2 = new com.michaelbaranov.microba.calendar.DatePicker();
        qtyTextField = new javax.swing.JTextField();
        jToolBar4 = new javax.swing.JToolBar();
        VATButton = new javax.swing.JButton();
        shippingButton = new javax.swing.JButton();
        packingslipButton = new javax.swing.JButton();
        jToolBar5 = new javax.swing.JToolBar();
        miscButton = new javax.swing.JButton();
        discountButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        upcCombo = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Invoice / Quote");
        setIconImage(winIcon);

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        custButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Customers.png"))); // NOI18N
        custButton.setText("Bill To:");
        custButton.setToolTipText("Select a customer from My Connections");
        custButton.setIconTextGap(1);
        custButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                custButtonActionPerformed(evt);
            }
        });

        documentNumberField.setEditable(false);
        documentNumberField.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        documentNumberField.setToolTipText("8 Alphanumeric character max");

        custTextArea.setColumns(20);
        custTextArea.setRows(5);
        custTextArea.setToolTipText("Customer Billing Information");
        jScrollPane1.setViewportView(custTextArea);

        org.jdesktop.layout.GroupLayout logoPanelLayout = new org.jdesktop.layout.GroupLayout(logoPanel);
        logoPanel.setLayout(logoPanelLayout);
        logoPanelLayout.setHorizontalGroup(
            logoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );
        logoPanelLayout.setVerticalGroup(
            logoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        datePicker1.setToolTipText("Click the Calendar icon to select a date.");
        datePicker1.setFieldEditable(false);
        datePicker1.setShowNoneButton(false);
        datePicker1.setStripTime(true);
        datePicker1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                datePicker1PropertyChange(evt);
            }
        });

        shipToTextArea.setColumns(20);
        shipToTextArea.setRows(5);
        shipToTextArea.setToolTipText("DO NOT use this as the customer field, it is for Shipping directives only.");
        jScrollPane4.setViewportView(shipToTextArea);

        shipToButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Card index.png"))); // NOI18N
        shipToButton.setText("Ship To:");
        shipToButton.setToolTipText("Select a shipping address from My Connections");
        shipToButton.setMaximumSize(new java.awt.Dimension(39, 23));
        shipToButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shipToButtonActionPerformed(evt);
            }
        });

        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);

        invoiceNumberEditCheckBox.setText("Edit");
        invoiceNumberEditCheckBox.setToolTipText("Select this to Manually Edit the Invoice Number");
        invoiceNumberEditCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        invoiceNumberEditCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoiceNumberEditCheckBoxActionPerformed(evt);
            }
        });

        autoInvoiceNumberButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        autoInvoiceNumberButton.setText("Invoice #");
        autoInvoiceNumberButton.setToolTipText("Get Auto Invoice Number");
        autoInvoiceNumberButton.setEnabled(false);
        autoInvoiceNumberButton.setMargin(new java.awt.Insets(2, 8, 2, 8));
        autoInvoiceNumberButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoInvoiceNumberButtonActionPerformed(evt);
            }
        });

        convertButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Object manager.png"))); // NOI18N
        convertButton.setText("Copy To Invoice");
        convertButton.setToolTipText("Uses this Quote to Build a New Invoice");
        convertButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        convertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                convertButtonActionPerformed(evt);
            }
        });

        copyBillToButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Text file.png"))); // NOI18N
        copyBillToButton.setText("Copy");
        copyBillToButton.setToolTipText("Copies the 'Bill To'  into 'Ship To'");
        copyBillToButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        copyBillToButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyBillToButtonActionPerformed(evt);
            }
        });

        clearShipToButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Erase.png"))); // NOI18N
        clearShipToButton.setText("Clear");
        clearShipToButton.setToolTipText("Clears the 'Ship To' info");
        clearShipToButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearShipToButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 302, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(custButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 200, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(copyBillToButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 297, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(shipToButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 200, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(clearShipToButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(logoPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(autoInvoiceNumberButton)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(documentNumberField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(invoiceNumberEditCheckBox))
                    .add(convertButton)
                    .add(datePicker1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 170, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(copyBillToButton)
                            .add(clearShipToButton)
                            .add(custButton)
                            .add(shipToButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                        .add(logoPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(19, 19, 19))
                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(datePicker1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(convertButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 39, Short.MAX_VALUE)
                        .add(autoInvoiceNumberButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(documentNumberField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(invoiceNumberEditCheckBox)))
                    .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 142, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jScrollPane2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jScrollPane2.setOpaque(false);

        invoiceItemsTable.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        invoiceItemsTable.setInheritsPopupMenu(true);
        invoiceItemsTable.setRowSelectionAllowed(false);
        invoiceItemsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                invoiceItemsTableMouseClicked(evt);
            }
        });
        invoiceItemsTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                invoiceItemsTableKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(invoiceItemsTable);

        upcField.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        upcField.setToolTipText("Double-click or hit ENTER to select from inventory (F12 for Misc)");
        upcField.setNextFocusableComponent(miscButton);
        upcField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                upcFieldFocusGained(evt);
            }
        });
        upcField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                upcFieldMouseClicked(evt);
            }
        });
        upcField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upcFieldActionPerformed(evt);
            }
        });
        upcField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                upcFieldKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                upcFieldKeyReleased(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        optionsToolbar.setOrientation(javax.swing.SwingConstants.VERTICAL);
        optionsToolbar.setRollover(true);

        messageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Get message.png"))); // NOI18N
        messageButton.setText("Invoice Message");
        messageButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        messageButton.setPreferredSize(new java.awt.Dimension(160, 33));
        messageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                messageButtonActionPerformed(evt);
            }
        });
        optionsToolbar.add(messageButton);

        viewReturnsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Notes.png"))); // NOI18N
        viewReturnsButton.setText("View Returns");
        viewReturnsButton.setToolTipText("Product Returns for this Invoice");
        viewReturnsButton.setPreferredSize(new java.awt.Dimension(160, 29));
        viewReturnsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewReturnsButtonActionPerformed(evt);
            }
        });
        optionsToolbar.add(viewReturnsButton);

        printButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Print preview.png"))); // NOI18N
        printButton.setText("Print Preview");
        printButton.setToolTipText("Print");
        printButton.setPreferredSize(new java.awt.Dimension(160, 29));
        printButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printButtonActionPerformed(evt);
            }
        });
        optionsToolbar.add(printButton);

        printReceiptButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Form 3d.png"))); // NOI18N
        printReceiptButton.setText("Print Receipt");
        printReceiptButton.setFocusable(false);
        printReceiptButton.setPreferredSize(new java.awt.Dimension(160, 29));
        printReceiptButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        printReceiptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printReceiptButtonActionPerformed(evt);
            }
        });
        optionsToolbar.add(printReceiptButton);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(optionsToolbar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 220, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(optionsToolbar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel5.setPreferredSize(new java.awt.Dimension(296, 116));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Item Total");

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("Grand Total");

        itemTotalField.setEditable(false);
        itemTotalField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        itemTotalField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        itemTotalField.setToolTipText("Total for All Items (Taxes Not Included)");

        grandTotalField.setEditable(false);
        grandTotalField.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        grandTotalField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jToolBar1.setRollover(true);

        calcButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Grid.png"))); // NOI18N
        calcButton.setText("Prices");
        calcButton.setToolTipText("Tally Invoice Totals");
        calcButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        calcButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calcButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(calcButton);

        saveButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Floppy.png"))); // NOI18N
        saveButton.setText("Quote");
        saveButton.setToolTipText("Save As a Quote");
        saveButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        saveButton.setIconTextGap(8);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(saveButton);

        postButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        postButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Next.png"))); // NOI18N
        postButton.setText(" Post");
        postButton.setToolTipText("Save As an Invoice");
        postButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        postButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                postButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(postButton);

        closeButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        closeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Cancel.png"))); // NOI18N
        closeButton.setText("Discard");
        closeButton.setToolTipText("Cancel / Close");
        closeButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(closeButton);

        jToolBar3.setRollover(true);

        receiptCheckBox.setText("Print Receipt ");
        receiptCheckBox.setToolTipText("Select this to Print to a Reciept Printer");
        receiptCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        receiptCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jToolBar3.add(receiptCheckBox);

        paymentCheckBox.setText("Take Payment ");
        paymentCheckBox.setToolTipText("When posting, this setting displays the payment box before printing");
        paymentCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        paymentCheckBox.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jToolBar3.add(paymentCheckBox);

        discountTextField.setEditable(false);
        discountTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Discounts");

        taxableTotalField.setEditable(false);
        taxableTotalField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Taxable");

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jToolBar3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, taxableTotalField)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, discountTextField)
                            .add(grandTotalField)
                            .add(itemTotalField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jToolBar3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(itemTotalField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(discountTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(taxableTotalField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(grandTotalField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel7))
                .addContainerGap())
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        t1Field.setEditable(false);
        t1Field.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        t1Field.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        t1Field.setToolTipText("Total for All Items");

        t2Field.setEditable(false);
        t2Field.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        t2Field.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        t2Field.setToolTipText("Total for All Items");

        t1Label.setText("Tax1");

        t2Label.setText("Tax2");

        toolBar.setRollover(true);

        paymentButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Money bag.png"))); // NOI18N
        paymentButton.setText("Activity");
        paymentButton.setToolTipText("Process Payments and Adjustments for this Invoice");
        paymentButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        paymentButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        paymentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paymentButtonActionPerformed(evt);
            }
        });
        toolBar.add(paymentButton);

        statementButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Period end.png"))); // NOI18N
        statementButton.setText("Statement");
        statementButton.setToolTipText("Detailed Statement of Activity for this Invoice");
        statementButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        statementButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        statementButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statementButtonActionPerformed(evt);
            }
        });
        toolBar.add(statementButton);

        historyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Case history.png"))); // NOI18N
        historyButton.setText("History");
        historyButton.setToolTipText("Invoice History for the Associated Customer");
        historyButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        historyButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        historyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                historyButtonActionPerformed(evt);
            }
        });
        toolBar.add(historyButton);

        jLabel1.setText("Due Date");

        datePicker2.setEnabled(false);

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(datePicker2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, datePicker2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(t1Label)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(t1Field, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(t2Label)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(t2Field, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 131, Short.MAX_VALUE)
                        .add(30, 30, 30))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(toolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(t1Field, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(t1Label)
                    .add(t2Field, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(t2Label))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(toolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        qtyTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        qtyTextField.setText("1.00");
        qtyTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                qtyTextFieldFocusGained(evt);
            }
        });

        jToolBar4.setRollover(true);

        VATButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Calculator.png"))); // NOI18N
        VATButton.setText("VAT  ");
        VATButton.setToolTipText("VAT - GST Tax Calculator");
        VATButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VATButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(VATButton);

        shippingButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Scales.png"))); // NOI18N
        shippingButton.setText("Weight");
        shippingButton.setToolTipText("Calculate the weight of the select items");
        shippingButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        shippingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shippingButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(shippingButton);

        packingslipButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Pallet.png"))); // NOI18N
        packingslipButton.setText("Packing");
        packingslipButton.setToolTipText("Print a packing slip of the selected items");
        packingslipButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                packingslipButtonActionPerformed(evt);
            }
        });
        jToolBar4.add(packingslipButton);

        jToolBar5.setRollover(true);

        miscButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Login.png"))); // NOI18N
        miscButton.setText("Misc Item");
        miscButton.setToolTipText("Add Non-Inventory Item");
        miscButton.setNextFocusableComponent(qtyTextField);
        miscButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miscButtonActionPerformed(evt);
            }
        });
        jToolBar5.add(miscButton);

        discountButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Percent.png"))); // NOI18N
        discountButton.setText("Disc");
        discountButton.setToolTipText("Insert a discount");
        discountButton.setFocusable(false);
        discountButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        discountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discountButtonActionPerformed(evt);
            }
        });
        jToolBar5.add(discountButton);

        removeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Delete.png"))); // NOI18N
        removeButton.setText("Remove");
        removeButton.setToolTipText("Removes the Selected Items");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });
        jToolBar5.add(removeButton);

        upcCombo.setFont(new java.awt.Font("DejaVu Sans", 0, 12)); // NOI18N
        upcCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "UPC", "Code" }));
        upcCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upcComboActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 896, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel3Layout.createSequentialGroup()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel3Layout.createSequentialGroup()
                        .add(qtyTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(upcCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(upcField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jToolBar5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jToolBar4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 276, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jToolBar5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jToolBar4, 0, 0, Short.MAX_VALUE)
                    .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, upcField)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, qtyTextField)
                        .add(upcCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 36, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 122, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                    .add(jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void viewReturnsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewReturnsButtonActionPerformed

        int r[] = {0, 0, 0, 4, 4, 4};
        int w[] = {50, 100, 300, 80};

//        new TableBrowseDialog(parentWin, true, currentInvoice.getItemReturns()),
//                "Invoice " + currentInvoice.getInvoiceNumber() + " Returns", r, w);

    }//GEN-LAST:event_viewReturnsButtonActionPerformed

    private void autoInvoiceNumberButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoInvoiceNumberButtonActionPerformed

        documentNumberField.setText(invoiceService.getNewInvoiceNumber(appSettings.getInvoice().getInvoicePrefix()));
        invoiceNumberEditCheckBox.setSelected(false);
        documentNumberField.setEditable(invoiceNumberEditCheckBox.isSelected());

    }//GEN-LAST:event_autoInvoiceNumberButtonActionPerformed
    private String mem_invoiceNumber = "";
    private void invoiceNumberEditCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoiceNumberEditCheckBoxActionPerformed
        boolean selected = invoiceNumberEditCheckBox.isSelected();
        documentNumberField.setEditable(selected);
        autoInvoiceNumberButton.setEnabled(selected);
        if (selected) {
            mem_invoiceNumber = documentNumberField.getText();
        } else {
            documentNumberField.setText(mem_invoiceNumber);
        }

    }//GEN-LAST:event_invoiceNumberEditCheckBoxActionPerformed

    private void shipToButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shipToButtonActionPerformed

        shipToAction();

    }//GEN-LAST:event_shipToButtonActionPerformed

    private void packingslipButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_packingslipButtonActionPerformed

        createPackingSlip();

    }//GEN-LAST:event_packingslipButtonActionPerformed

    private void createPackingSlip() {
        var company = appSettings.getCompany();
        var inventory = appSettings.getInventory();
        String measure = inventory.getWeightUnit();
        LinePrinter lp = new LinePrinter(true);
        ArrayList al = calcWeight(false);
        String zone = company.getCountryCode();
        StringBuilder sb = new StringBuilder();

        lp.addLine("P A C K I N G   S L I P" + "                     " + DV.getShortDate());
        lp.newLine();
        lp.addLine("FROM:");

        var companyAddress = new String[]{
            company.getCompanyName(),
            company.getAddress1(),
            company.getCity() + " " + company.getState() + " " + company.getPostalCode(),
            company.getPhoneNumber()
        };

        lp.addLines(companyAddress);
        lp.newLine();
        lp.newLine();

        lp.addLine("TO:");

        if (shipToTextArea.getText().trim().length() > 0) {

            lp.addLines(shipToTextArea.getText().split(System.getProperty("line.separator")));

        } else {

            lp.addLines(custTextArea.getText().split(System.getProperty("line.separator")));

        }
        lp.newLine();
        lp.newLine();

        Object[] itemInfo = new Object[2];

        float totalLineWeight;
        int tableRow;

        if (al != null) {
            al.trimToSize();
            for (int i = 0; i < al.size(); i++) {

                itemInfo = (Object[]) al.get(i);

                tableRow = (Integer) itemInfo[0];
                totalLineWeight = (Float) itemInfo[1];

                String qt = Float.toString((Float) invoiceItemsTable.getModel().getValueAt(tableRow, 2));

                sb.append(DV.addSpace(qt, 4, ' '));
                sb.append(DV.addSpace((String) invoiceItemsTable.getModel().getValueAt(tableRow, 4), 52, ' '));
                sb.append(Float.toString(totalLineWeight) + ' ' + measure);

                lp.addLine(sb.toString());
                sb = new StringBuilder();
            }

            lp.newLine();
            lp.newLine();
            lp.addLine("Item(s) Weight: " + totalWeight + ' ' + measure + "  -  Shipping Weight:_________________");

            lp.formFeed();
            lp.go();
        } else {

            javax.swing.JOptionPane.showMessageDialog(null, "Please select the items to include on the packing list. (Hold Ctrl and Click each row.)");
            return;
        }
    }

    private void shippingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shippingButtonActionPerformed

        /*
         *  scan through and get and calc weight of each item SOLD
         *
         *  total weight of items and display
         *
         */
        calcWeight(true);


    }//GEN-LAST:event_shippingButtonActionPerformed

    private double totalWeight = 0;

    private ArrayList calcWeight(boolean show_total) {

        var measure = appSettings.getInventory().getWeightUnit();

        int[] selectedRows = invoiceItemsTable.getSelectedRows();
        ArrayList items = new ArrayList();

        if (selectedRows.length < 1) {

            if (show_total) {
                javax.swing.JOptionPane.showMessageDialog(null, "Please select some items to calculate. (Hold CTRL key, and CLICK each row.)");
            }
            return null;
        }

        var tableModel = (InvoiceItemsTableModel) invoiceItemsTable.getModel();
        double totalWeight = 0;
        for (int i = 0; i < selectedRows.length; i++) {
            var item = (InvoiceItem) tableModel.getValueAt(selectedRows[i]);
            var weight = Double.parseDouble(item.getWeight());
            var soldWeight = (weight * item.getQuantity());
            totalWeight += soldWeight;
            items.add(new Object[]{selectedRows[i], soldWeight});

        }
        this.totalWeight = totalWeight;

        if (show_total) {
            javax.swing.JOptionPane.showMessageDialog(null, "Total Weight: " + this.totalWeight + ' ' + measure);
        }

        return items;
    }

    private void invoiceItemsTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_invoiceItemsTableKeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE && !viewPrint) {

            removeRows();

        }


    }//GEN-LAST:event_invoiceItemsTableKeyPressed

    private void miscButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miscButtonActionPerformed

        miscAction();

    }//GEN-LAST:event_miscButtonActionPerformed

    private void scanAction() throws SQLException {

        if (!DV.validFloatString(qtyTextField.getText())) {
            JOptionPane.showMessageDialog(this, "Make sure Quantity is a valid number.", "Form Problem!", JOptionPane.OK_OPTION);
            return;
        }

        if (upcField.getText().trim().equals("")) {   // blank upcField ENTER pressed

            var items = addItemFromInventoryApp();
            if (items != null) {
                addItems(items);
                return;
            }

        } else {  //value entered into upcField and ENTER pressed

            if (this.upcField.getText().startsWith("/")) {
                this.getInventoryGroup();
            }

            var searchField = (String) upcCombo.getSelectedItem();
            var searchText = upcField.getText().trim();

            try {
                List<Inventory> items = null;
                if (searchField.equals("UPC")) {
                    items = inventoryService.getAllInventoryByUpc(searchText);
                } else if (searchField.equals("Code")) {
                    items = inventoryService.getAllInventoryByCode(searchText);
                } else if (searchField.equals("Description")) {
                    items = inventoryService.getAllInventoryByDecription(searchText);
                }

                if (items == null) {
                    var choice = javax.swing.JOptionPane.showConfirmDialog(null,
                            "Would you like to open the inventory manager?",
                            searchField + ": " + upcField.getText() + " was not found",
                            JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                    if (choice == JOptionPane.YES_OPTION) {
                        items = addItemFromInventoryApp();
                        addItems(items);
                        return;
                    }
                }

            } catch (SQLException e) {
                ExceptionService.showErrorDialog(this, e, "Error searching inventory database table");
            }

        }

        clearFields();

    }

    private void addItems(List<Inventory> items) throws SQLException {
        if (items != null) {
            for (var item : items) {
                addItem(item);
            }
            clearFields();
        }
    }

    private List<Inventory> addItemFromInventoryApp() {
        var inventoryApp = new InventoryApp(this.parentWin, true, true);

        try {
            inventoryApp.displayApp();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching settings when starting inventory");
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing local settings");
        }

        var items = inventoryApp.getReturnValue();

        if (items == null || items.size() < 1) {
            clearFields();
            return null;
        }
        inventoryApp.dispose();

        return items;
    }

    private void getInventoryGroup() {

//        /* clip everything past 'grp=' & store in grpname */
//        String grpname = group.substring(1, group.length());
//
//        /* scan the grps dir for the specified group */
//        //String path = workingPath + "grps/";
//
//        File dir = new File(path);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File f = new File(path + grpname);
//        String line;
//        int k = 0;
//        boolean skip = false;
//        if (f.exists()) {
//
//            BufferedReader in = null;
//            try {
//                in = new BufferedReader(
//                        new FileReader(f));
//                do {
//
//                    skip = false;
//                    line = in.readLine();
//                    try {
//                        if (line != null) {
//                            k = Integer.valueOf(line);
//                        } else {
//                            skip = true;
//                        }
//                    } catch (NumberFormatException ex) {
//                        /* ignore bad numbers */
//                        skip = true;
//                    }
//
//                    if (!skip) {
//                        currentItem = db.getRecord("inventory", k);
//                        this.addItem(currentItem, addCategoryInfo, -1, -1); //neg one means no replace
//                        /*add category line item here?? added in InvoiceModel  */
//
//                    }
//                    //System.out.println(line);
//
//                } while (line != null);
//
//                in.close();
//                upcField.setText("");
//                clearFields();
//                return;
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                javax.swing.JOptionPane.showMessageDialog(null, "An error occured while processing group: " + grpname);
//                try {
//                    in.close();
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//                return;
//            }
//
//        } else {
//
//            javax.swing.JOptionPane.showMessageDialog(null, "The group doesn't exist.");
//
//        }
    }

    private void upcFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_upcFieldKeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            try {
                scanAction();
            } catch (SQLException ex) {
                ExceptionService.showErrorDialog(this, ex, "Error accessing inventory database");
                return;
            }
        }

        if (evt.getKeyCode() == 123) {  //F12
            miscAction();
        }

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_HOME) {
            post();
        }

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_END) {
            int a = javax.swing.JOptionPane.showConfirmDialog(null, "Discard this invoice?", "Exit", JOptionPane.YES_NO_OPTION);

            if (a == JOptionPane.YES_OPTION) {
                closeInvoiceWindow();
            }
        }
    }//GEN-LAST:event_upcFieldKeyPressed

    private void post() {
        this.viewToModel();
        if (invoiceItemsTable.getRowCount() < 1) {
            return;
        }

        if (!verifyInvoiceNumber(documentNumberField.getText())) {
            return;
        }

        var tableModel = (InvoiceItemsTableModel) this.invoiceItemsTable.getModel();

        if (tableModel.getRowCount() < 1) {
            return; //TODO notify user 
        }

        if (custTextArea.getText().trim().equals("") || custTextArea.getText().trim().length() < 4) {

            javax.swing.JOptionPane.showMessageDialog(null, "Please provide some valid customer information.");
            return;
        }

        int a = javax.swing.JOptionPane.showConfirmDialog(null, "Would you like to commit this invoice?", "Post invoice?", JOptionPane.YES_NO_OPTION);
        if (a == JOptionPane.NO_OPTION) {
            return;
        }

        try {
            var items = tableModel.getCollection();
            var rowIndex = 0;
            for (var item : items) {
                if (!canItemBeSold(item)) {
                    invoiceItemsTable.changeSelection(rowIndex, 0, false, false);
                    return;
                }
                rowIndex++;
            }
        } catch (SQLException e) {
            ExceptionService.showErrorDialog(this, e, "Error performing inventory checks in database");
        }

        try {
            postInvoice(paymentCheckBox.isSelected());
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error posting invoice to database");
            return;
        }

        if (receiptCheckBox.isSelected()) {
            printReciept();
        } else {
            print(false);
        }
        this.dispose();
    }

    private void sendEmail(String toAddress, String file) {

        if (!Tools.verifyEmailAddress(toAddress)) {
            return; // TODO: show dialog?
        }
        var invoiceSettings = appSettings.getInvoice();
        var companySettings = appSettings.getCompany();
        var emailSettings = appSettings.getInternet().getEmailSettings();

        var type = this.currentInvoice.isQuote() ? invoiceSettings.getQuoteName() : invoiceSettings.getInvoiceName();

        NewEmail email = new NewEmail();
        email.setAttachment(file);
        email.setRecipent(toAddress);
        email.setText(type + " from " + companySettings.getCompanyName());
        email.setSubject(type + " number: " + documentNumberField.getText() + " From " + companySettings.getCompanyName());
        email.setFrom(emailSettings.getReturnAddress());
        email.setServer(emailSettings.getServerAddress());
        email.setPort(emailSettings.getServerPort());
        email.setUsername(emailSettings.getServerUsername());
        email.setPassword(emailSettings.getServerPassword());
        email.setSSL(emailSettings.isUseSSL());
        email.sendEmail();

    }

    private void postButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_postButtonActionPerformed

        post();

    }//GEN-LAST:event_postButtonActionPerformed


    private void printButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printButtonActionPerformed

        try {

            var html = createHtml();
            //System.out.print(html);

            // write and open HTML file
            Path htmlTempFile = Files.createTempFile(this.currentInvoice.getInvoiceNumber() + "__", ".html");
            FileWriter fileWriter = new FileWriter(htmlTempFile.toFile());
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(html);
            printWriter.close();
            Desktop.getDesktop().browse(htmlTempFile.toUri());

            // write and open PDF file
            Path pdfTempFile = Files.createTempFile(this.currentInvoice.getInvoiceNumber() + "__", ".pdf");
//            MediaDeviceDescription mediaDeviceDescription = new MediaDeviceDescription(MediaType.PRINT);
//            ConverterProperties props = new ConverterProperties();
//            props.setMediaDeviceDescription(mediaDeviceDescription);
//            HtmlConverter.convertToPdf(html, new FileOutputStream(pdfTempFile.toFile()), props);

            try (OutputStream os = new FileOutputStream(pdfTempFile.toAbsolutePath().toString())) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                var file = htmlTempFile.toUri().toString(); // .getFileName().toAbsolutePath().toString();
                builder.withUri(file);
                builder.toStream(os);
                builder.run();
            }

            Desktop.getDesktop().browse(pdfTempFile.toUri());

        } catch (URISyntaxException ex) {
            Logger.getLogger(InvoiceApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InvoiceApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TemplateException ex) {
            Logger.getLogger(InvoiceApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OutputException ex) {
            Logger.getLogger(InvoiceApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BarcodeException ex) {
            Logger.getLogger(InvoiceApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (WriterException ex) {
            Logger.getLogger(InvoiceApp.class.getName()).log(Level.SEVERE, null, ex);
        }catch(SQLException ex) {
            Logger.getLogger(InvoiceApp.class.getName()).log(Level.SEVERE, null, ex);
        }


    }//GEN-LAST:event_printButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed

        closeInvoiceWindow();

    }//GEN-LAST:event_closeButtonActionPerformed

    private void closeInvoiceWindow() {

        this.dispose();

    }

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed

        saveQuote();

    }//GEN-LAST:event_saveButtonActionPerformed

    private void saveQuote() {

        viewToModel();

        if (invoiceItemsTable.getRowCount() < 1) {
            return;  // TODO: notify via dialog?
        }
        String qmsg = "this as a quote?";
        if (this.currentInvoice.getId() != null) {
            qmsg = "this quote?";
        }
        int a = javax.swing.JOptionPane.showConfirmDialog(null,
                "Do you want to save " + qmsg, "Save Quote", JOptionPane.YES_NO_OPTION);
        if (a == JOptionPane.NO_OPTION) {
            return;
        }

        this.currentInvoice.setQuote(true);
        this.setInvoiceNumber(this.currentInvoice);
        computePrices();

        try {
            this.invoiceService.postInvoice(currentInvoice);
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error saving quote");
        }
        // this.print(true);  //prints a quote from this invoice

        this.dispose();
    }

    private String setInvoiceNumber(Invoice invoice) {

        String documentNumber;
        if (invoice.isQuote()) {
            documentNumber = invoiceService.getNewInvoiceNumber(appSettings.getInvoice().getQuotePrefix());
        } else {
            documentNumber = invoiceService.getNewInvoiceNumber(appSettings.getInvoice().getInvoicePrefix());
        }
        this.documentNumberField.setText(documentNumber);
        invoice.setInvoiceNumber(documentNumber);
        return documentNumber;
    }

    private boolean verifyInvoiceNumber(String invoiceNumber) {
        return true;
    }

    private boolean postInvoice(boolean takePayment) throws SQLException {

        computePrices();

        invoiceService.postInvoice(currentInvoice); // TODO: save items ....create postInvoice service method to handle all

        if (takePayment) {

//            PaymentDialog pd = new PaymentDialog(parentWin, true, invoiceKey, application);
//            pd.setVisible(true);
//            invoice = new OldInvoice(application, invoiceKey);
            //paidCheckBox.setSelected(invoice.isPaid());
        }

//        /* delete quote? */
//        if (removeQuote && quoteToRemove > 0) {
//
//            Quote aQuote = new Quote(db, quoteToRemove);
//            int tmpKey;
//
//            // delete quote items
//            DefaultTableModel items = aQuote.getItems();
//          
//            for (int r = 0; r < items.getRowCount(); r++) {
//
//                tmpKey = (Integer) items.getValueAt(r, 0);
//                db.removeRecord("qitems", tmpKey);
//
//            }
//
//            db.removeRecord("quote", quoteToRemove);
//
//            // delete quote shipto
//            tmpKey = aQuote.getShipToKey();
//            if (tmpKey > 0) {
//                db.removeRecord("qshipto", tmpKey);
//            }
//            aQuote = null;
//        }//end quote delete
        return true;
    }

    private String createHtml() throws URISyntaxException, IOException, TemplateException, OutputException, BarcodeException, WriterException, SQLException {

        this.viewToModel();
        var totals = InvoiceService.calculateInvoiceTotals(currentInvoice);

        /* Create and adjust the configuration singleton */
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
        cfg.setDirectoryForTemplateLoading(new File(getClass().getResource("/html").toURI()));

        // Recommended settings for new projects:
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);

        /* Create variables--and populate with data--for the template */
        Map root = new HashMap();

        //var url = getClass().getResource("/html/demo-logo.png");
        //var companyLogoImage = imageService.getImageFromUrl(url);
        //var base64CompanyLogo = imageService.convertImageToBase64Png(companyLogoImage);
        var base64CompanyLogo = this.settingsService.getObject().getCompany().getCompanyLogo();
        var companyLogoImgSrc = imageService.createHtmlImgSrc(base64CompanyLogo);
        root.put("companyLogo", companyLogoImgSrc);
        
        var coQrCode = barcodeService.createQrCodeImage("https://datavirtue.com", 80);
        var base64coQrCode = imageService.convertImageToBase64Png(coQrCode);
        var coQrCodeImgSrc = imageService.createHtmlImgSrc(base64coQrCode);
        root.put("companyQrCode", coQrCodeImgSrc);

        var invoiceNumberBarcode = barcodeService.createCode128Image(this.currentInvoice.getInvoiceNumber(), 10);
        var base64code128 = imageService.convertImageToBase64Png(invoiceNumberBarcode);
        var code128ImgSrc = imageService.createHtmlImgSrc(base64code128);
        root.put("invoiceNumberBarcode", code128ImgSrc);

        var websiteQrCode = barcodeService.createQrCodeImage("https://datavirtue.com", 100);
        var base64QrCode = imageService.convertImageToBase64Png(websiteQrCode);
        var qrCodeImgSrc = imageService.createHtmlImgSrc(base64QrCode);
        root.put("websiteQrCode", qrCodeImgSrc);

        
        if (this.currentInvoice.getBillTo() != null) {
            var billToGoogleMapsLink = contactService.createGoogleMapLink(this.currentInvoice.getBillTo());
            
            var billToMapQrCode = barcodeService.createQrCodeImage(billToGoogleMapsLink, 80);
            var base64BillToQr = imageService.convertImageToBase64Png(billToMapQrCode);
            var billToQrCodeImgSrc = imageService.createHtmlImgSrc(base64BillToQr);
            root.put("billToQrCode", billToQrCodeImgSrc);        
       } else {
           root.put("billToQrCode", null); 
       }
        
       if (this.currentInvoice.getShiptTo() != null) {
            var shipToGoogleMapsLink = contactService.createGoogleMapLink(this.currentInvoice.getShiptTo());
            
            var shipToMapQrCode = barcodeService.createQrCodeImage(shipToGoogleMapsLink, 80);
            var base64ShipToQr = imageService.convertImageToBase64Png(shipToMapQrCode);
            var shipToQrCodeImgSrc = imageService.createHtmlImgSrc(base64ShipToQr);
            root.put("shipToQrCode", shipToQrCodeImgSrc);        
       } else {
           root.put("shipToQrCode", null); 
       }
       
        root.put("invoice", this.currentInvoice);
        root.put("billTo", this.currentInvoice.getBillTo());
        root.put("shipTo", this.currentInvoice.getShiptTo());
        root.put("items", this.currentInvoice.getItems());

        /*  Totals */
        root.put("subTotal", CurrencyUtil.money(totals.subTotal));
        root.put("discountsTotal", CurrencyUtil.money(totals.pretaxDiscounts));
        root.put("taxable1Subtotal", CurrencyUtil.money(totals.taxable1Subtotal));
        root.put("taxable2Subtotal", CurrencyUtil.money(totals.taxable2Subtotal));
        root.put("tax1Total", CurrencyUtil.money(totals.tax1Total));
        root.put("tax2Total", CurrencyUtil.money(totals.tax2Total));
        root.put("payments", CurrencyUtil.money(0.00));
        root.put("amountDue", CurrencyUtil.money(totals.getGrandTotal()));

        /* Get the template (uses cache internally) */
        Template template = cfg.getTemplate("invoice.html");

        Writer html = new CharArrayWriter();
        template.process(root, html);
        return html.toString();

    }


    private void calcButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_calcButtonActionPerformed

        if (invoiceItemsTable.getRowCount() < 1) {
            return;
        }

        if (!viewPrint) {
            computePrices();
        }

    }//GEN-LAST:event_calcButtonActionPerformed

    private void invoiceItemsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_invoiceItemsTableMouseClicked

        int mouseButton = evt.getButton();
        if (mouseButton == evt.BUTTON2 || mouseButton == evt.BUTTON3) {
            return;
        }
        if (evt.getClickCount() == 2) {
            if (this.currentInvoice.getId() != null) {
                return;
            }

            var selectedRow = invoiceItemsTable.getSelectedRow();

            var tableModel = (InvoiceItemsTableModel) invoiceItemsTable.getModel();

            var item = tableModel.getValueAt(selectedRow);

            if (item.getCode().equals("MISC")) {
                MiscItemDialog mi = new MiscItemDialog(null, true, item);//send the misc item to edit

                item = mi.getItem();

                mi.dispose();
            }

        }

        computePrices();

    }//GEN-LAST:event_invoiceItemsTableMouseClicked

    private void removeRows() {
        if (invoiceItemsTable.getRowCount() < 1) {
            return;
        }
        int[] selectedRows = invoiceItemsTable.getSelectedRows();

        if (selectedRows == null || selectedRows.length == 0) {
            return;
        }

        var tableModel = (InvoiceItemsTableModel) invoiceItemsTable.getModel();
        var invoiceItems = tableModel.getCollection();

        var selectedItems = new ArrayList<InvoiceItem>();

        for (int index = 0; index < selectedRows.length; index++) {
            var item = invoiceItems.get(selectedRows[index]);
            selectedItems.add(item);
        }

        for (var item : selectedItems) {
            invoiceItems.remove(item);

            if (invoiceItems.size() > 0) {
                var relatedDiscounts = invoiceItems
                        .stream()
                        .filter(x -> x.getRelatedInvoiceItem() != null && x.getRelatedInvoiceItem().equals(item.getId()))
                        .collect(Collectors.toList());
                if (relatedDiscounts.size() > 0) {
                    for (var relatedDiscount : relatedDiscounts) {
                        invoiceItems.remove(relatedDiscount);
                    }
                }
            }

        }

        computePrices();
        upcField.requestFocus();

    }

    private void getDiscount() {
        var selectedRow = invoiceItemsTable.getSelectedRow();

        if (selectedRow < 0) {
            return; //TODO: give feedback about selecting items?
        }

        var tableModel = (InvoiceItemsTableModel) this.invoiceItemsTable.getModel();
        var item = tableModel.getValueAt(selectedRow);

        var discountDialog = new DiscountDialog(this.parentWin, true, item);
        discountDialog.displayApp();

        var discountItem = discountDialog.getDiscountItem();

        this.addItemToInvoiceItemsTable(discountItem);

        computePrices();

    }

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed

        removeRows();

    }//GEN-LAST:event_removeButtonActionPerformed

    private void messageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_messageButtonActionPerformed

//        NoteDialog nd = new NoteDialog(parentWin, true, application, true);
//        nd.setVisible(true);
//        String m = nd.getReturnValue();
//        if (m.length() > 1) {
//            invoiceMessage = nd.getReturnValue();
//            messageButton.setToolTipText(invoiceMessage);
//
//        }
//        nd.dispose();
    }//GEN-LAST:event_messageButtonActionPerformed

    private void shipToAction() {

        if (this.currentInvoice.getCustomerId() != null) {

            int a = javax.swing.JOptionPane.showConfirmDialog(null,
                    "Would you like to select from this customer's shipping addresses? ",
                    "Shipping Address Option", JOptionPane.YES_NO_OPTION);
            if (a == JOptionPane.YES_OPTION) {

                var shippingDialog
                        = new ContactShippingDialog(parentWin, true, this.currentInvoice.getCustomerId(), true);
                shippingDialog.displayApp();

                var address = shippingDialog.getSelectedAddress();

                if (address != null) {

                    this.setAddressView(address, shipToTextArea);
                    var customerInfo = invoiceService.mapContactAddressToInvoiceShipTo(address);
                    this.currentInvoice.setShiptTo(customerInfo);
                }
                return;
            }
        }

        // open MyConnectionsApp and get ANY address
        var contactsApp = new ContactsApp(this.parentWin, true, true, true, false);

        try {
            contactsApp.displayApp();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing settings database");
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing local settings");
        }

        var shippingContact = contactsApp.getReturnValue();

        if (shippingContact == null) {
            return;
        }
        
        if (shippingContact != null) {
            this.setAddressView(shippingContact, shipToTextArea);
        } else {
            shipToTextArea.setEditable(true);
        }

        contactsApp.dispose();
        contactsApp = null;

    }

    private void custAction() {

        var contactsApp
                = new ContactsApp(this.parentWin, true, true, true, false);

        try {
            contactsApp.displayApp();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing settings database");
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing local settings");
        }

        var contact = contactsApp.getReturnValue();
       
        if (contact == null) {
            return;
        }

        custTextArea.setText("");

        this.customer = contact;
        this.currentInvoice.setCustomerId(contact.getId());
        var customer = invoiceService.mapContactToInvoiceCustomer(contact);
        this.currentInvoice.setBillTo(customer);
        
        contactsApp.dispose();
        contactsApp = null;

        if (contact != null) {

            shipToButton.setEnabled(true);
            setAddressView(contact, custTextArea);

        } else {
            this.customer = new Contact();
            this.currentInvoice.setCustomerId(null);
            custTextArea.setText("S A L E");
            custTextArea.setEditable(true);
        }

        upcField.requestFocus();
    }

    private void setAddressView(ContactAddressInterface contact, JTextArea textArea) {

        if (contact == null) {
            return;
        }

        String[] cust = contactService.formatContactAddress(contact);

        textArea.setText("");
        for (int i = 0; i < cust.length; i++) {
            textArea.append((String) cust[i]);
        }
        textArea.setEditable(false);
    }


    private void custButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_custButtonActionPerformed

        custAction();

    }//GEN-LAST:event_custButtonActionPerformed

    private void convertButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_convertButtonActionPerformed

        convertOrCopy();

    }//GEN-LAST:event_convertButtonActionPerformed

    private void copyBillToButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyBillToButtonActionPerformed

        if (this.currentInvoice.getBillTo() != null) {
            var shipTo = invoiceService.mapContactAddressToInvoiceShipTo(this.currentInvoice.getBillTo());
            this.currentInvoice.setShiptTo(shipTo);
            setAddressView(shipTo, shipToTextArea);
        }
    }//GEN-LAST:event_copyBillToButtonActionPerformed

    private void clearShipToButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearShipToButtonActionPerformed

        shipToTextArea.setText("");
        shipToTextArea.setEditable(true);

    }//GEN-LAST:event_clearShipToButtonActionPerformed

    private void paymentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paymentButtonActionPerformed
        doPayment();
    }//GEN-LAST:event_paymentButtonActionPerformed

    private void statementButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statementButtonActionPerformed
        doStatement();
    }//GEN-LAST:event_statementButtonActionPerformed

    private void historyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_historyButtonActionPerformed
        doHistory();
    }//GEN-LAST:event_historyButtonActionPerformed

    private void qtyTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_qtyTextFieldFocusGained
        qtyTextField.selectAll();
    }//GEN-LAST:event_qtyTextFieldFocusGained

    private void upcFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_upcFieldFocusGained
        upcField.selectAll();
    }//GEN-LAST:event_upcFieldFocusGained

    private void upcFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_upcFieldMouseClicked
        if (evt.getClickCount() == 2) {
            try {
                scanAction();
            } catch (SQLException ex) {
                ExceptionService.showErrorDialog(this, ex, "Error scanning inventory database");
            }
        }
    }//GEN-LAST:event_upcFieldMouseClicked

    private void VATButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VATButtonActionPerformed
        new VATCalculator(parentWin, true, appSettings.getInvoice().getTax1Rate());
    }//GEN-LAST:event_VATButtonActionPerformed

    private void printReceiptButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printReceiptButtonActionPerformed
        this.printReciept();
    }//GEN-LAST:event_printReceiptButtonActionPerformed

private void datePicker1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_datePicker1PropertyChange
    Calendar gc = new GregorianCalendar();
    gc.setTime(datePicker1.getDate());
    //dueDateChooser.setCurrent(gc);// TODO add your handling code here:
}//GEN-LAST:event_datePicker1PropertyChange

    private void upcFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_upcFieldKeyReleased
        if (upcField.getText().equals("+")) {
            String num = qtyTextField.getText();
            double qty = DV.parseDouble(num);
            qty++;
            qtyTextField.setText(CurrencyUtil.money(qty));
            upcField.setText("");
            upcField.requestFocus();

        }

        if (upcField.getText().equals("-")) {
            String num = qtyTextField.getText();
            double qty = DV.parseDouble(num);
            if (qty > 1) {
                qty--;
            }
            qtyTextField.setText(CurrencyUtil.money(qty));
            upcField.setText("");
            upcField.requestFocus();
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_upcFieldKeyReleased

    private void upcComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upcComboActionPerformed
        upcField.requestFocus();
    }//GEN-LAST:event_upcComboActionPerformed

    private void discountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discountButtonActionPerformed

        this.getDiscount();

//cycle thru selected items and total prices
        //if one do something with the desc in the discdialog?
        //erect discdialog
        //resume from dialog and apply new item
//        if (invoiceItemsTable.getSelectedRows() == null || invoiceItemsTable.getSelectedRow() < 0) {
//
//            //display a warning dialog, JIDE???
//            return;
//        }
//
//        int[] selectedRows = invoiceItemsTable.getSelectedRows();
//        float tot = 0.00f;
//        boolean tx1 = false;
//        boolean tx2 = false;
//        for (int r = 0; r < selectedRows.length; r++) {
//            tot += ((Float) invoiceItemsTable.getModel().getValueAt(selectedRows[r], 8)); //add the totals of the selected items
//            if ((Boolean) invoiceItemsTable.getModel().getValueAt(selectedRows[r], 7)) {
//                tx2 = true;
//            }
//            if ((Boolean) invoiceItemsTable.getModel().getValueAt(selectedRows[r], 6)) {
//                tx1 = true;
//            }
//        }
//
//        if (tot <= 0) {
//            //gripe
//            return;
//        }
//
//        DiscountDialog disc = new DiscountDialog(null, true, application, "Discount", tot, tx1, tx2);
//        Object[] discountItem;
//        if (disc.getStat()) {
//            discountItem = disc.getDisc();
//        } else {
//            return;
//        }
//
//        int insert = -1;
//
//        if (selectedRows[selectedRows.length - 1] >= invoiceItemsTable.getRowCount() - 1) {
//            insert = -1;
//        } else {
//            insert = selectedRows[(selectedRows.length - 1)] + 1;
//        }//if the last row is not selected make it the insert point
//
//        if (discountItem != null) {
//            this.addItem(discountItem, false, -1, insert);
//        }
//
//        disc.dispose();

    }//GEN-LAST:event_discountButtonActionPerformed

    private void upcFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upcFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_upcFieldActionPerformed

    private void doPayment() {
        //new PaymentActivityDialog(null, true, application, invoice);

    }

    private void doStatement() {

//        if (!accessKey.checkInvoice(500)) {
//            accessKey.showMessage("Statements");
//            return;
//        }
//
//        int k = invoice.getInvoiceKey();
//
//        ReportFactory.generateStatements(application, k);
    }

    private void doHistory() {
//
//        if (!accessKey.checkReports(500)) {
//            accessKey.showMessage("Customer/Supplier Reports");
//            return;
//        }
//
//        if (customer != null) {
//            ReportFactory.generateCustomerStatement(application, customer);
//        } else {
//
//            javax.swing.JOptionPane.showMessageDialog(null,
//                    "This invoice is not assigned to a specific customer.");
//        }
    }

    private void convertOrCopy() {
        saveButton.setEnabled(false);

        Object[] options = {"Convert", "Copy", "Cancel"};
        var selectedOption = JOptionPane.showOptionDialog(null,
                "Choose 'Convert' if you DO NOT want to keep a copy of this quote." + nl + "Select 'Copy' to preserve this quote for later use." + nl + "If you select 'Convert,' the quote is only deleted if you post the invoice.",
                "Preserve Quote?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[2]);

        //Cancel
        if (selectedOption == 2) {
            saveButton.setEnabled(true);
            return;
        }

        //Convert
        if (selectedOption == 0) {
            convertToInvoice();
            return;
        }

        //Copy
        if (selectedOption == 1) {
            this.copyToInvoice();
        }

    }

    private void convertToInvoice() {
        this.currentInvoice.setQuote(false);
        this.setInvoiceNumber(this.currentInvoice);
        this.modelToView();
        saveButton.setEnabled(true);
        this.postButton.setEnabled(true);
    }

    private void copyToInvoice() {
        var newInvoice = new Invoice();

        var items = new ArrayList<InvoiceItem>();

        for (InvoiceItem item : this.currentInvoice.getItems()) {
            var newItem = new InvoiceItem();
            newItem.setCode(item.getCode());
            newItem.setCost(item.getCost());
            newItem.setDate(new Date());
            newItem.setDescription(item.getDescription());
            newItem.setInvoice(newInvoice);
            newItem.setPartialSaleAllowed(item.isPartialSaleAllowed());
            newItem.setQuantity(item.getQuantity());
            newItem.setSourceInventoryId(item.getSourceInventoryId());
            newItem.setTaxable1(item.isTaxable1());
            newItem.setTaxable1Rate(item.getTaxable1Rate());
            newItem.setTaxable2(item.isTaxable2());
            newItem.setTaxable2Rate(item.getTaxable2Rate());
            newItem.setUnitPrice(item.getUnitPrice());
            newItem.setWeight(item.getWeight());
            items.add(newItem);
        }

        newInvoice.setItems(items);
        newInvoice.setBillTo(this.currentInvoice.getBillTo());
        newInvoice.setShiptTo(this.currentInvoice.getShiptTo());
        newInvoice.setCustomerId(this.currentInvoice.getCustomerId());
        newInvoice.setInvoiceDate(new Date());
        newInvoice.setMessage(this.currentInvoice.getMessage());
        
        newInvoice.setVoided(false);

        newInvoice.setInvoiceNumber(this.setInvoiceNumber(newInvoice));
        this.setInvoice(newInvoice);
        this.modelToView();

        convertButton.setVisible(false);
        invoiceNumberEditCheckBox.setEnabled(true);
        //printButton.setVisible(false);
        computePrices();
        postButton.setEnabled(true);
        saveButton.setEnabled(true);
        upcField.requestFocus();
    }

    private void printCustomInvoice() {

        //load document layout
        //start a new book
        //initiate an InvoicePrintPanel passing in Book and documentLayout
        //cycle through invitems
        /*  get item, see if the item carries an annotation
         *  calculate the space needed to render the item (plus annotation if required)
         *  make sure annotation and item fit on the page
         *  render item and annotaion (subtract available space)
         *  repeat, when out of space, stuff into Book and launch another InvoicePrintPanel?
         *  What about a PrinterJob?
         *
         */
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton VATButton;
    private javax.swing.JButton autoInvoiceNumberButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JButton calcButton;
    private javax.swing.JButton clearShipToButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton convertButton;
    private javax.swing.JButton copyBillToButton;
    private javax.swing.JButton custButton;
    private javax.swing.JTextArea custTextArea;
    private com.michaelbaranov.microba.calendar.DatePicker datePicker1;
    private com.michaelbaranov.microba.calendar.DatePicker datePicker2;
    private javax.swing.JButton discountButton;
    private javax.swing.JTextField discountTextField;
    private javax.swing.JTextField documentNumberField;
    private javax.swing.JTextField grandTotalField;
    private javax.swing.JButton historyButton;
    private javax.swing.JTable invoiceItemsTable;
    private javax.swing.JCheckBox invoiceNumberEditCheckBox;
    private javax.swing.JTextField itemTotalField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JToolBar jToolBar5;
    private javax.swing.JPanel logoPanel;
    private javax.swing.JButton messageButton;
    private javax.swing.JButton miscButton;
    private javax.swing.JToolBar optionsToolbar;
    private javax.swing.JButton packingslipButton;
    private javax.swing.JButton paymentButton;
    private javax.swing.JCheckBox paymentCheckBox;
    private javax.swing.JButton postButton;
    private javax.swing.JButton printButton;
    private javax.swing.JButton printReceiptButton;
    private javax.swing.JTextField qtyTextField;
    private javax.swing.JCheckBox receiptCheckBox;
    private javax.swing.JButton removeButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JButton shipToButton;
    private javax.swing.JTextArea shipToTextArea;
    private javax.swing.JButton shippingButton;
    private javax.swing.JButton statementButton;
    private javax.swing.JTextField t1Field;
    private javax.swing.JLabel t1Label;
    private javax.swing.JTextField t2Field;
    private javax.swing.JLabel t2Label;
    private javax.swing.JTextField taxableTotalField;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JComboBox upcCombo;
    private javax.swing.JTextField upcField;
    private javax.swing.JButton viewReturnsButton;
    // End of variables declaration//GEN-END:variables

}
