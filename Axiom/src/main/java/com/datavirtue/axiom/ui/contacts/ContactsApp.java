/*
 * ConnectionsDialog.java
 *
 * Created on June 22, 2006, 10:08 AM
 ** Copyright (c) Data Virtue 2006
 */
package com.datavirtue.axiom.ui.contacts;

import com.datavirtue.axiom.ui.util.LimitedDocument;
import com.datavirtue.axiom.ui.invoices.PaymentDialog;
import com.datavirtue.axiom.ui.invoices.InvoiceApp;
import com.datavirtue.axiom.models.contacts.old.ContactJournalListModel;
import com.datavirtue.axiom.models.contacts.old.ContactsTableModel;
import com.google.inject.Injector;
import com.datavirtue.axiom.services.DiService;
import javax.swing.table.*;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.net.URI;
import java.sql.SQLException;
import com.datavirtue.axiom.models.contacts.Contact;
import com.datavirtue.axiom.models.contacts.ContactJournal;
import com.datavirtue.axiom.models.invoices.CustomerInvoiceListTableModel;
import com.datavirtue.axiom.models.invoices.Invoice;
import com.datavirtue.axiom.models.settings.AppSettings;
import com.datavirtue.axiom.models.settings.CompanySettings;
import com.datavirtue.axiom.services.AppSettingsService;
import com.datavirtue.axiom.services.ContactJournalService;
import com.datavirtue.axiom.services.ContactService;
import com.datavirtue.axiom.services.ExceptionService;
import com.datavirtue.axiom.services.LocalSettingsService;
import com.datavirtue.axiom.services.UserService;
import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.ui.AxiomApp;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.prefs.BackingStoreException;

/**
 *
 * @author Sean K Anderson - Data Virtue
 * @rights Copyright Data Virtue 2006, 2007, 2008, 2009, 2010, 2022 All Rights
 * Reserved.
 */
public class ContactsApp extends javax.swing.JDialog implements AxiomApp {

    private Contact currentContact = new Contact();
    private final ContactService contactService;
    private final ContactJournalService journalService;
    private final AppSettingsService appSettingsService;

    private Contact returnValue = null;
    private boolean selectMode = false;
    private int edit_key = 0;
    private java.awt.Frame parentWin;

    private String nl = System.getProperty("line.separator");
    private Image winIcon;

    public ContactsApp(java.awt.Frame parent, boolean modal, boolean select, boolean customers, boolean suppliers) {

        super(parent, modal);

        Injector injector = DiService.getInjector();
        contactService = injector.getInstance(ContactService.class);
        journalService = injector.getInstance(ContactJournalService.class);
        appSettingsService = injector.getInstance(AppSettingsService.class);
        appSettingsService.setObjectType(AppSettings.class);

        winIcon = Toolkit.getDefaultToolkit().getImage(ContactsApp.class.getResource("/Orange.png"));

        initComponents();

        /* Limit chars availble in textfields */
        companyTextField.setDocument(new LimitedDocument(35));
        firstTextField.setDocument(new LimitedDocument(20));
        lastTextField.setDocument(new LimitedDocument(20));
        addressTextField.setDocument(new LimitedDocument(40));
        suiteTextField.setDocument(new LimitedDocument(40));
        cityTextField.setDocument(new LimitedDocument(30));
        stateTextField.setDocument(new LimitedDocument(20));
        zipTextField.setDocument(new LimitedDocument(10));
        contactTextField.setDocument(new LimitedDocument(20));
        phoneTextField.setDocument(new LimitedDocument(20));
        faxTextField.setDocument(new LimitedDocument(20));
        emailTextField.setDocument(new LimitedDocument(40));
        wwwTextField.setDocument(new LimitedDocument(50));
        notesTextArea.setDocument(new LimitedDocument(100));

        functionToolbar.setLayout(new FlowLayout());
        invoiceToolbar.setLayout(new FlowLayout());

        if (customers && !suppliers) {
            custRadio.setSelected(true);
        }
        if (suppliers && !customers) {
            suppRadio.setSelected(true);
        }
        if (customers && suppliers) {
            allRadio.setSelected(true);
        }

        selectMode = select;

        if (select) {
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
        /* End Close Dialog on Escape*/

    }

    public void displayApp() throws SQLException, BackingStoreException {

        var user = UserService.getCurrentUser();

        if (!user.isAdmin() && user.getContacts() < 300) {
            JOptionPane.showMessageDialog(this, "Please see the admin about permissions.", "Access denied", JOptionPane.OK_OPTION);
            this.dispose();
            return;
        }

        var settings = appSettingsService.getObject();
        var company = settings.getCompany();
        this.setTitle(company.getCompanyName() + " Business Contacts");

        var contactSettings = appSettingsService.getObject().getContacts();
        var defaultSearchField = contactSettings != null
                ? contactSettings.getContactDefaultSearchField()
                : "Company";

        searchFieldCombo.setModel(
                new javax.swing.DefaultComboBoxModel(
                        new String[]{"Company", "First Name", "Last Name", "Address1", "Address2", "City", "State", "Postal Code", "Contact", "Phone", "Fax", "Email", "Web Link"}));
        searchFieldCombo.setSelectedItem(defaultSearchField);

        contactDetailTabPane.setSelectedIndex(0);
        findField.requestFocus();

        var invoiceSettings = appSettingsService.getObject().getInvoice();
        tax1CheckBox.setText(invoiceSettings.getTax1Name());
        tax2CheckBox.setText(invoiceSettings.getTax2Name());

        this.refreshTable();

        try {
            restoreSavedWindowSizeAndPosition();
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching local screen settings");
        }

        this.setVisible(true);
    }

    private ContactsTableModel filter() {

        try {

            if (allRadio.isSelected()) {
                return new ContactsTableModel(contactService.getAll());
            }
            if (custRadio.isSelected()) {
                return new ContactsTableModel(contactService.getAllCustomers());
            }
            if (suppRadio.isSelected()) {
                return new ContactsTableModel(contactService.getAllVendors());
            }
            if (unpaidRadio.isSelected()) {
                return new ContactsTableModel(contactService.getUnpaidCustomers());
            }
            return new ContactsTableModel(contactService.getAll());

        } catch (SQLException e) {
            JOptionPane.showConfirmDialog(this, e.getMessage(), "DATABASE ERROR", WIDTH);
            return null;
        }
    }

    public Contact getReturnValue() {

        return returnValue;
    }

    private void recordWindowSizeAndPosition() throws BackingStoreException {
        var localSettings = LocalSettingsService.getLocalAppSettings();
        var screenSettings = localSettings.getScreenSettings();
        var sizeAndPosition = LocalSettingsService.getWindowSizeAndPosition(this);
        screenSettings.setContacts(sizeAndPosition);
        LocalSettingsService.saveLocalAppSettings(localSettings);
    }

    private void restoreSavedWindowSizeAndPosition() throws BackingStoreException {
        var localSettings = LocalSettingsService.getLocalAppSettings();
        var screenSettings = localSettings.getScreenSettings().getContacts();
        LocalSettingsService.applyScreenSizeAndPosition(screenSettings, this);
    }

    private void clearFields() {
        currentContact = new Contact();

        CompanySettings settings = null;

        try {
            settings = appSettingsService.getObject().getCompany();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching settings");

        }

        var countryCode = settings != null ? settings.getCountryCode() : Locale.getDefault().getCountry();

        keyLabel.setText(currentContact.getId() != null ? currentContact.getId().toString() : "NEW - UNSAVED");  //show the user the key for the record

        //populateInvoices(false);
        companyTextField.setText("");
        firstTextField.setText("");
        lastTextField.setText("");
        addressTextField.setText("");
        suiteTextField.setText("");
        cityTextField.setText("");
        stateTextField.setText("");
        zipTextField.setText("");
        countryCombo.setSelectedItem(countryCode);
        contactTextField.setText("");
        phoneTextField.setText("");
        faxTextField.setText("");
        emailTextField.setText("");
        wwwTextField.setText("https://");
        notesTextArea.setText("");
        custCheckBox.setSelected(false);
        supplierCheckBox.setSelected(false);
        tax1CheckBox.setSelected(false);
        tax2CheckBox.setSelected(false);

        journalList.setModel(new javax.swing.DefaultListModel());

        journalTextArea.setText("");

    }

    private void setFieldsEnabled(boolean enabled) {

        companyTextField.setEnabled(enabled);
        firstTextField.setEnabled(enabled);
        lastTextField.setEnabled(enabled);
        addressTextField.setEnabled(enabled);
        suiteTextField.setEnabled(enabled);
        cityTextField.setEnabled(enabled);
        stateTextField.setEnabled(enabled);
        zipTextField.setEnabled(enabled);
        countryCombo.setEnabled(enabled);
        contactTextField.setEnabled(enabled);
        phoneTextField.setEnabled(enabled);
        faxTextField.setEnabled(enabled);
        emailTextField.setEnabled(enabled);
        wwwTextField.setEnabled(enabled);
        notesTextArea.setEnabled(enabled);
        custCheckBox.setEnabled(enabled);
        supplierCheckBox.setEnabled(enabled);
        tax1CheckBox.setEnabled(enabled);
        tax2CheckBox.setEnabled(enabled);
        shipToButton.setEnabled(enabled);
        zipButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        viewButton.setEnabled(enabled);
        journalList.setEnabled(enabled);
        journalTextArea.setEnabled(enabled);

        if (currentContact.getId() != null) {
            newButton.setEnabled(enabled);
        } else {
            newButton.setEnabled(false);
        }

        if (enabled) {
            messageField.setText("Remember to click 'Save' when you modify a record.");
        } else {

            messageField.setText("Click the Company Field to start a new record.");

        }

    }

    private void populateFields() {

        if (contactTable.getSelectedRow() > -1) {
            //int key = (Integer) connTable.getModel().getValueAt(connTable.getSelectedRow(), 0);
            //connDAO = new ConnectionsDAO(db, application, key);
            //edit_key = connDAO.getKey();
            var tableModel = (ContactsTableModel) contactTable.getModel();
            currentContact = (Contact) tableModel.getValueAt(contactTable.getSelectedRow());
        } else {
            return;
        }

//        companyTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 1));
//        firstTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 2));
//        lastTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 3));
//        addressTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 4));
//        suiteTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 5));
//        cityTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 6));
//        stateTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 7));
//        zipTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 8));
//        countryCombo.setSelectedItem((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 17));
//        contactTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 9));
//        phoneTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 10));
//        faxTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 11));
//        emailTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 12));
//        wwwTextField.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 13));
//        notesTextArea.setText((String) connTable.getModel().getValueAt(connTable.getSelectedRow(), 14));
//        custCheckBox.setSelected((Boolean) connTable.getModel().getValueAt(connTable.getSelectedRow(), 15));
//        supplierCheckBox.setSelected((Boolean) connTable.getModel().getValueAt(connTable.getSelectedRow(), 16));
//        tax1CheckBox.setSelected((Boolean) connTable.getModel().getValueAt(connTable.getSelectedRow(), 18));
//        tax2CheckBox.setSelected((Boolean) connTable.getModel().getValueAt(connTable.getSelectedRow(), 19));
        companyTextField.setText(currentContact.getCompanyName());
        firstTextField.setText(currentContact.getFirstName());
        lastTextField.setText(currentContact.getLastName());
        addressTextField.setText(currentContact.getAddress1());
        suiteTextField.setText(currentContact.getAddress2());
        cityTextField.setText(currentContact.getCity());
        stateTextField.setText(currentContact.getState());
        zipTextField.setText(currentContact.getPostalCode());
        countryCombo.setSelectedItem(currentContact.getCountryCode());
        contactTextField.setText(currentContact.getContactName());
        phoneTextField.setText(currentContact.getPhoneNumber());
        faxTextField.setText(currentContact.getFax());
        emailTextField.setText(currentContact.getEmail());
        wwwTextField.setText(currentContact.getWebLink());
        notesTextArea.setText(currentContact.getNotes());
        custCheckBox.setSelected(currentContact.isCustomer());
        supplierCheckBox.setSelected(currentContact.isVendor());
        tax1CheckBox.setSelected(currentContact.isTaxable1());
        tax2CheckBox.setSelected(currentContact.isTaxable2());

        //populateInvoices(false);
        populateJournals();
        keyLabel.setText(currentContact.getId().toString());  //show the user the key for the record
        this.setFieldsEnabled(true);

    }

    private void populateInvoices(boolean change) {

        
        
        
        if (contactTable.getSelectedRow() < 0) {
            return;
        }

        /* Determin the table we wil work from, quotes or invoices */
        String table = "invoice";

        if (invoiceToggleButton.getText().endsWith("Quotes")) {
            if (change) {
                table = "quote";
                invoiceLabel.setText("Quotes ");

                invoiceTable.setToolTipText("Quotes found for this contact");
                invoiceToggleButton.setText("Show Invoices");
            } else {
                table = "invoice";
            }
        } else {
            if (change) {
                table = "invoice";
                invoiceLabel.setText("Invoices ");

                invoiceTable.setToolTipText("Invoices found for this contact");
                invoiceToggleButton.setText("Show Quotes");
            } else {
                table = "quote";
            }
        }

        int key = (Integer) contactTable.getModel().getValueAt(contactTable.getSelectedRow(), 0);

        /* the custom TableModel is assigned to the My Connections invoiceTable */
        //invoiceTable.setModel(connDAO.getInvoiceTableModel(table, key));
        if (invoiceTable.getRowCount() < 1) {
            invoiceReportButton.setEnabled(false);
            purchaseHistoryButton.setEnabled(false);
        } else {
            invoiceReportButton.setEnabled(true);
            purchaseHistoryButton.setEnabled(true);
        }

        /* remove key field - clean up*/
        setView();

    }

    private void setView() {

        TableColumnModel cm = invoiceTable.getColumnModel();
        TableColumn tc;

        if (invoiceTable.getColumnCount() > 2) {

            //setup hold table view
            tc = cm.getColumn(0);
            invoiceTable.removeColumn(tc);//remove key column
        }

        tc = invoiceTable.getColumnModel().getColumn(0);
        tc.setPreferredWidth(90);
        tc = invoiceTable.getColumnModel().getColumn(1);
        tc.setPreferredWidth(40);

    }

    public void setView(int[] cols) {

        if (contactTable.getModel().getRowCount() > 0) {
            TableColumnModel cm = contactTable.getColumnModel();
            TableColumn tc;
            //connTable.setCellEditor(null);

            for (int i = 0; i < cols.length; i++) {
                tc = cm.getColumn(cols[i]);
                contactTable.removeColumn(tc);
            }

            int a = contactTable.getColumnCount();
            javax.swing.JTextField tf = new javax.swing.JTextField();
            tf.setEditable(false);
            for (int i = 0; i < a; i++) {

                cm.getColumn(i).setCellEditor(new javax.swing.DefaultCellEditor(tf));
            }

        }

    }

    private void find() {

        if (!findField.getText().equals("")) {

            var searchField = (String) searchFieldCombo.getSelectedItem();
            searchField = searchField.replace(" ", "").toLowerCase();
            try {
                var results = contactService.searchField(searchField, findField.getText());
                if (results != null && results.size() > 0) {
                    contactTable.setModel(new ContactsTableModel(results));
                    //TODO: capture default search column
                } else {
                    JOptionPane.showMessageDialog(this, "No matching records were found.", "Find Failed", JOptionPane.OK_OPTION);
                }
            } catch (SQLException e) {
                ExceptionService.showErrorDialog(this, e, "Error searching contacts");
            }

        } else {
            refreshTable();
        }

    }

    private void export(String filename) {
//        ReportModel rm = new ReportModel(contactTable.getModel());
//        StringBuilder sb = new StringBuilder();
//        int col_count = contactTable.getModel().getColumnCount();
//
//        /* Headers  */
//        if (!new File(filename).exists()) {
//            String[] headers = db.getFieldNames("conn");
//            for (int i = 0; i < headers.length; i++) {
//                sb.append(headers[i]);
//                if (i < headers.length - 1) {
//                    sb.append(',');
//                }
//            }
//            sb.append(System.getProperty("line.separator"));
//        }
//        /* Data  */
//        do {
//            for (int c = 0; c < col_count; c++) {
//                sb.append(rm.getValueAt(c).replace(',', ';'));
//                if (c < contactTable.getModel().getColumnCount() - 1) {
//                    sb.append(',');
//                }
//            }
//            sb.append(System.getProperty("line.separator"));
//        } while (rm.next());
//        DV.writeFile(filename, sb.toString(), true);
    }

    private ContactJournal currentJournal;
    private java.util.List<ContactJournal> currentJournals;

    private void newJournal() {
        try {
            var journal = new ContactJournal();
            journal.setContact(currentContact);
            journal.setContent(new String());
            journalService.save(journal);
            populateJournals();
        } catch (SQLException e) {
            ExceptionService.showErrorDialog(this, e, "Error saving new journal");
        }
    }

    private void saveJournal() {
        try {
            int index = journalList.getSelectedIndex();
            if (index > -1) {
                var list = (ContactJournalListModel) journalList.getModel();
                currentJournal.setContent(journalTextArea.getText());

                journalService.save(currentJournal);
            }
        } catch (SQLException e) {
            ExceptionService.showErrorDialog(this, e, "Error saving journal");
        }
    }

    private void getJournal() {
        int index = journalList.getSelectedIndex();
        if (index > -1) {
            var list = (ContactJournalListModel) journalList.getModel();
            currentJournal = list.getJournalAt(index);
            journalTextArea.setText(currentJournal.getContent());
        }
    }

    private void populateJournals() {
        try {
            currentJournals = journalService.getJournalsForContact(currentContact.getId());
            currentJournals = currentJournals != null ? currentJournals : new ArrayList();
            journalList.setModel(new ContactJournalListModel(currentJournals));
            journalTextArea.setText("");
        } catch (SQLException e) {
            ExceptionService.showErrorDialog(this, e, "Error getting journals");
        }
    }

    public static void launch(String com, String target) {

        String osName = System.getProperty("os.name");

        try {

            if (osName.contains("Windows")) {
                String[] cm = {"cmd.exe", com + target};
                Runtime.getRuntime().exec(cm);

            } //FOR WINDOWS NT/XP/2000 USE CMD.EXE
            else {

                Runtime.getRuntime().exec(com + target);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void zipAction() {

//        java.util.ArrayList al = null;
//
//        if (zipTextField.getText().length() > 4 && zipTextField.getText().length() < 6 && DV.validIntString(zipTextField.getText())) {
//
//            al = zip.searchFast("zip", 1, zipTextField.getText(), false);
//
//        }
//
//        if (al != null) {
//            Object[] zipinfo = new Object[6];
//            zipinfo = zip.getRecord("zip", (Long) al.get(0));
//            cityTextField.setText((String) zipinfo[2]);
//            stateTextField.setText((String) zipinfo[3]);
//        }
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
        jSeparator2 = new javax.swing.JSeparator();
        contactTableScrollPane = new javax.swing.JScrollPane();
        contactTable = new javax.swing.JTable();
        contactTableToolPanel = new javax.swing.JPanel();
        findField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        searchFieldCombo = new javax.swing.JComboBox();
        functionToolbar = new javax.swing.JToolBar();
        toggleButton = new javax.swing.JButton();
        labelButton = new javax.swing.JButton();
        exportButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        filterToolbar = new javax.swing.JToolBar();
        allRadio = new javax.swing.JToggleButton();
        custRadio = new javax.swing.JToggleButton();
        suppRadio = new javax.swing.JToggleButton();
        unpaidRadio = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        selectButton = new javax.swing.JButton();
        voidButton = new javax.swing.JButton();
        detailPanel = new javax.swing.JPanel();
        contactDetailTabPane = new javax.swing.JTabbedPane();
        addressPanel = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        companyTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        firstTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        lastTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        addressTextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        suiteTextField = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        zipButton = new javax.swing.JButton();
        zipTextField = new javax.swing.JTextField();
        stateTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        cityTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        countryCombo = new javax.swing.JComboBox();
        shipToButton = new javax.swing.JButton();
        custCheckBox = new javax.swing.JCheckBox();
        supplierCheckBox = new javax.swing.JCheckBox();
        tax1CheckBox = new javax.swing.JCheckBox();
        tax2CheckBox = new javax.swing.JCheckBox();
        keyLabel = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        contactPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        invoiceTable = new javax.swing.JTable();
        invoiceToolbar = new javax.swing.JToolBar();
        viewButton = new javax.swing.JButton();
        invoiceReportButton = new javax.swing.JButton();
        purchaseHistoryButton = new javax.swing.JButton();
        invoiceLabel = new javax.swing.JLabel();
        invoiceToggleButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        emailTextField = new javax.swing.JTextField();
        phoneTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        wwwTextField = new javax.swing.JTextField();
        emailButton = new javax.swing.JButton();
        contactTextField = new javax.swing.JTextField();
        faxTextField = new javax.swing.JTextField();
        wwwButton = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        journalPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        notesTextArea = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        journalList = new javax.swing.JList();
        newButton = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        journalTextArea = new javax.swing.JTextPane();
        jLabel17 = new javax.swing.JLabel();
        contactDetailsToolBar = new javax.swing.JToolBar();
        clearButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        messageField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("My Connections");
        setIconImage(winIcon);
        setModal(true);

        contactTable.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        contactTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        contactTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                contactTableMousePressed(evt);
            }
        });
        contactTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                contactTableKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                contactTableKeyReleased(evt);
            }
        });
        contactTableScrollPane.setViewportView(contactTable);

        contactTableToolPanel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        findField.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        findField.setToolTipText("Input Search Text Here and Hit ENTER to Search");
        findField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                findFieldFocusGained(evt);
            }
        });
        findField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                findFieldKeyPressed(evt);
            }
        });

        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Zoom.png"))); // NOI18N
        jLabel9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel9MouseClicked(evt);
            }
        });

        searchFieldCombo.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        searchFieldCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchFieldComboActionPerformed(evt);
            }
        });

        functionToolbar.setRollover(true);

        toggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Down.png"))); // NOI18N
        toggleButton.setText("Less Detail");
        toggleButton.setToolTipText("Click this to Toggle the Form On or Off");
        toggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        toggleButton.setPreferredSize(new java.awt.Dimension(85, 49));
        toggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleButtonActionPerformed(evt);
            }
        });
        functionToolbar.add(toggleButton);

        labelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Tag.png"))); // NOI18N
        labelButton.setText("Labels");
        labelButton.setToolTipText("Select Rows and Click this Button to Generate Labels");
        labelButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        labelButton.setPreferredSize(new java.awt.Dimension(78, 49));
        labelButton.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        labelButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        labelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                labelButtonActionPerformed(evt);
            }
        });
        functionToolbar.add(labelButton);

        exportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Export text.png"))); // NOI18N
        exportButton.setText("Export");
        exportButton.setToolTipText("Export the Current Table to a (.csv text) File");
        exportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exportButton.setPreferredSize(new java.awt.Dimension(78, 49));
        exportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });
        functionToolbar.add(exportButton);

        deleteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Delete.png"))); // NOI18N
        deleteButton.setText("Delete");
        deleteButton.setToolTipText("Permenant Delete!");
        deleteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteButton.setMargin(new java.awt.Insets(2, 10, 2, 10));
        deleteButton.setPreferredSize(new java.awt.Dimension(78, 53));
        deleteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        functionToolbar.add(deleteButton);

        filterToolbar.setRollover(true);
        filterToolbar.setBorderPainted(false);

        buttonGroup1.add(allRadio);
        allRadio.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Navigator.png"))); // NOI18N
        allRadio.setText("All Records");
        allRadio.setFocusable(false);
        allRadio.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        allRadio.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        allRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allRadioActionPerformed(evt);
            }
        });
        filterToolbar.add(allRadio);

        buttonGroup1.add(custRadio);
        custRadio.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Customers.png"))); // NOI18N
        custRadio.setText("Cutomers");
        custRadio.setFocusable(false);
        custRadio.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        custRadio.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        custRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                custRadioActionPerformed(evt);
            }
        });
        filterToolbar.add(custRadio);

        buttonGroup1.add(suppRadio);
        suppRadio.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Trailer.png"))); // NOI18N
        suppRadio.setText("Suppliers");
        suppRadio.setFocusable(false);
        suppRadio.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        suppRadio.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        suppRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                suppRadioActionPerformed(evt);
            }
        });
        filterToolbar.add(suppRadio);

        buttonGroup1.add(unpaidRadio);
        unpaidRadio.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Call.png"))); // NOI18N
        unpaidRadio.setText("Unpaid");
        unpaidRadio.setFocusable(false);
        unpaidRadio.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        unpaidRadio.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        unpaidRadio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unpaidRadioActionPerformed(evt);
            }
        });
        filterToolbar.add(unpaidRadio);
        filterToolbar.add(jSeparator1);

        selectButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/OK.png"))); // NOI18N
        selectButton.setText("Select");
        selectButton.setFocusable(false);
        selectButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        selectButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectButtonActionPerformed(evt);
            }
        });
        filterToolbar.add(selectButton);

        voidButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/No.png"))); // NOI18N
        voidButton.setText("None");
        voidButton.setFocusable(false);
        voidButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        voidButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        voidButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                voidButtonActionPerformed(evt);
            }
        });
        filterToolbar.add(voidButton);

        org.jdesktop.layout.GroupLayout contactTableToolPanelLayout = new org.jdesktop.layout.GroupLayout(contactTableToolPanel);
        contactTableToolPanel.setLayout(contactTableToolPanelLayout);
        contactTableToolPanelLayout.setHorizontalGroup(
            contactTableToolPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(contactTableToolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(contactTableToolPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(contactTableToolPanelLayout.createSequentialGroup()
                        .add(searchFieldCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(10, 10, 10)
                        .add(findField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 165, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 34, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(filterToolbar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(functionToolbar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        contactTableToolPanelLayout.setVerticalGroup(
            contactTableToolPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(contactTableToolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(contactTableToolPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, functionToolbar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)
                    .add(contactTableToolPanelLayout.createSequentialGroup()
                        .add(filterToolbar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 44, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(contactTableToolPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                            .add(searchFieldCombo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                            .add(findField))))
                .addContainerGap())
        );

        detailPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        contactDetailTabPane.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        addressPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Company");

        companyTextField.setColumns(35);
        companyTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        companyTextField.setToolTipText("Click here to Create a New Contact [35 Char] [Company or First Name REQUIRED]");
        companyTextField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                companyTextFieldMouseClicked(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("First");

        firstTextField.setColumns(20);
        firstTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        firstTextField.setToolTipText("[20 Char] Company or First Name REQUIRED");

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("Last");

        jLabel16.setForeground(new java.awt.Color(255, 0, 0));
        jLabel16.setText("*");

        jLabel13.setForeground(new java.awt.Color(255, 0, 0));
        jLabel13.setText("*");

        lastTextField.setColumns(20);
        lastTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lastTextField.setToolTipText("[20 Char]");

        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("Address");

        addressTextField.setColumns(40);
        addressTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        addressTextField.setToolTipText("[40 Char] 38 Characters is the Suggested Limit for Address Lines");

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Addr #2");

        suiteTextField.setColumns(10);
        suiteTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        suiteTextField.setToolTipText("[40 Char] 38 Characters is the Suggested Limit for Address Lines");

        jPanel6.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        zipButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        zipButton.setText("Postal code");
        zipButton.setToolTipText("Look up the city and state");
        zipButton.setMargin(new java.awt.Insets(2, 4, 2, 4));
        zipButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zipButtonActionPerformed(evt);
            }
        });

        zipTextField.setColumns(10);
        zipTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        zipTextField.setToolTipText("[10 Char Stored]");
        zipTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                zipTextFieldKeyPressed(evt);
            }
        });

        stateTextField.setColumns(2);
        stateTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        stateTextField.setToolTipText("[2 Char] Locality");

        jLabel8.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("State/Prov/Region");

        cityTextField.setColumns(30);
        cityTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cityTextField.setToolTipText("[30 Char] Dependent Locality");
        cityTextField.setNextFocusableComponent(stateTextField);

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("City/Town");

        jLabel14.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel14.setText("Format");

        countryCombo.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        countryCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "US", "CA", "AU", "UK", "ZA", "IN", "NZ", "PH" }));
        countryCombo.setToolTipText("Sets the Country Code for this Contact");

        shipToButton.setText("More addresses");
        shipToButton.setToolTipText("Create or Modify Shipping Addresses");
        shipToButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shipToButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(zipButton)
                        .add(6, 6, 6))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 112, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 112, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(zipTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel14)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(countryCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 72, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(shipToButton))
                    .add(stateTextField)
                    .add(cityTextField))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cityTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel7))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(stateTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(zipTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(zipButton)
                    .add(jLabel14)
                    .add(countryCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(shipToButton))
                .addContainerGap())
        );

        custCheckBox.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        custCheckBox.setText("Customer");
        custCheckBox.setToolTipText("Marks This Contact as a Customer");
        custCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        custCheckBox.setNextFocusableComponent(supplierCheckBox);

        supplierCheckBox.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        supplierCheckBox.setText("Supplier");
        supplierCheckBox.setToolTipText("Marks This Contact as a Supplier");
        supplierCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        supplierCheckBox.setNextFocusableComponent(contactTextField);

        tax1CheckBox.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        tax1CheckBox.setText("Tax 1");
        tax1CheckBox.setToolTipText("Tax Status");

        tax2CheckBox.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        tax2CheckBox.setText("Tax 2");
        tax2CheckBox.setToolTipText("Tax Status");

        keyLabel.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        keyLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        keyLabel.setText("ID");
        keyLabel.setEnabled(false);

        jLabel18.setForeground(new java.awt.Color(255, 0, 0));
        jLabel18.setText("*");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(custCheckBox)
                        .add(18, 18, 18)
                        .add(supplierCheckBox)
                        .add(18, 18, 18)
                        .add(tax1CheckBox)
                        .add(18, 18, 18)
                        .add(tax2CheckBox)
                        .add(18, 18, 18)
                        .add(keyLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jLabel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(suiteTextField)
                            .add(addressTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)))
                    .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(lastTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                            .add(companyTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                            .add(firstTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jLabel16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jLabel18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 10, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(companyTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(firstTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(lastTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addressTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(suiteTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(custCheckBox)
                    .add(supplierCheckBox)
                    .add(tax2CheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(tax1CheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(keyLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(27, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 0, Short.MAX_VALUE)
        );

        jSplitPane1.setRightComponent(jPanel5);

        org.jdesktop.layout.GroupLayout addressPanelLayout = new org.jdesktop.layout.GroupLayout(addressPanel);
        addressPanel.setLayout(addressPanelLayout);
        addressPanelLayout.setHorizontalGroup(
            addressPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(addressPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 842, Short.MAX_VALUE)
                .addContainerGap())
        );
        addressPanelLayout.setVerticalGroup(
            addressPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, addressPanelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(438, 438, 438))
        );

        contactDetailTabPane.addTab("Address", addressPanel);

        contactPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        invoiceTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        invoiceTable.setToolTipText("Invoices found for this contact");
        invoiceTable.setSelectionBackground(new java.awt.Color(204, 255, 204));
        invoiceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                invoiceTableMouseClicked(evt);
            }
        });
        invoiceTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                invoiceTableKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(invoiceTable);

        invoiceToolbar.setRollover(true);

        viewButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        viewButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Eye.png"))); // NOI18N
        viewButton.setText("View");
        viewButton.setToolTipText("Open an Invoice or Quote");
        viewButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        viewButton.setMargin(new java.awt.Insets(1, 14, 1, 14));
        viewButton.setPreferredSize(new java.awt.Dimension(72, 49));
        viewButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        viewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewButtonActionPerformed(evt);
            }
        });
        invoiceToolbar.add(viewButton);

        invoiceReportButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        invoiceReportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Paper.png"))); // NOI18N
        invoiceReportButton.setText("History");
        invoiceReportButton.setToolTipText("Customer's invoice history statement");
        invoiceReportButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        invoiceReportButton.setPreferredSize(new java.awt.Dimension(72, 47));
        invoiceReportButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        invoiceReportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoiceReportButtonActionPerformed(evt);
            }
        });
        invoiceToolbar.add(invoiceReportButton);

        purchaseHistoryButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        purchaseHistoryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Credit.png"))); // NOI18N
        purchaseHistoryButton.setText("Purchases");
        purchaseHistoryButton.setToolTipText("Customer's product purchase history report");
        purchaseHistoryButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        purchaseHistoryButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        purchaseHistoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                purchaseHistoryButtonActionPerformed(evt);
            }
        });
        invoiceToolbar.add(purchaseHistoryButton);

        invoiceLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        invoiceLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        invoiceLabel.setText("Invoices");
        invoiceLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        invoiceToggleButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        invoiceToggleButton.setText("Show Quotes");
        invoiceToggleButton.setToolTipText("Toggles the Display of Quotes or Invoices");
        invoiceToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoiceToggleButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                    .add(invoiceToolbar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                    .add(invoiceToggleButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                    .add(invoiceLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(invoiceToggleButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(invoiceLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(invoiceToolbar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        emailTextField.setColumns(40);
        emailTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        emailTextField.setToolTipText("[40 Char]");

        phoneTextField.setColumns(12);
        phoneTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        phoneTextField.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        phoneTextField.setToolTipText("[20 Characters]");
        phoneTextField.setNextFocusableComponent(faxTextField);
        phoneTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                phoneTextFieldActionPerformed(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Phone");

        jLabel10.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Name");

        wwwTextField.setColumns(50);
        wwwTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        wwwTextField.setText("http://");
        wwwTextField.setToolTipText("[50 Char] Must Contain a Protocol Such as http:// or https://");
        wwwTextField.setNextFocusableComponent(notesTextArea);

        emailButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        emailButton.setText("email");
        emailButton.setToolTipText("New Email");
        emailButton.setMargin(new java.awt.Insets(1, 2, 1, 2));
        emailButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                emailButtonActionPerformed(evt);
            }
        });

        contactTextField.setColumns(20);
        contactTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        contactTextField.setToolTipText("[20 Char]");
        contactTextField.setNextFocusableComponent(phoneTextField);
        contactTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                contactTextFieldFocusGained(evt);
            }
        });

        faxTextField.setColumns(12);
        faxTextField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        faxTextField.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        faxTextField.setToolTipText("[20 Characters]");
        faxTextField.setNextFocusableComponent(emailTextField);

        wwwButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        wwwButton.setText("www");
        wwwButton.setToolTipText("Launch");
        wwwButton.setMargin(new java.awt.Insets(1, 2, 1, 2));
        wwwButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wwwButtonActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Fax");

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jLabel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(emailButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(wwwButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(wwwTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
                    .add(contactTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 483, Short.MAX_VALUE)
                    .add(faxTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 132, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(phoneTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 132, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(emailTextField, 0, 483, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(contactTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel11)
                    .add(phoneTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel12)
                    .add(faxTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(emailButton)
                    .add(emailTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(wwwTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(wwwButton))
                .addContainerGap(160, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout contactPanelLayout = new org.jdesktop.layout.GroupLayout(contactPanel);
        contactPanel.setLayout(contactPanelLayout);
        contactPanelLayout.setHorizontalGroup(
            contactPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, contactPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(8, 8, 8)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        contactPanelLayout.setVerticalGroup(
            contactPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(contactPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(contactPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        contactDetailTabPane.addTab("Contact/Invoices", contactPanel);

        journalPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Misc:");

        notesTextArea.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        notesTextArea.setToolTipText("[100 Character Limit]");
        notesTextArea.setNextFocusableComponent(saveButton);
        notesTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                notesTextAreaFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                notesTextAreaFocusLost(evt);
            }
        });

        journalList.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        journalList.setToolTipText("Daily Journals");
        journalList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                journalListMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(journalList);

        newButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        newButton.setText("New");
        newButton.setToolTipText("Create new journal for today");
        newButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });

        journalTextArea.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        journalTextArea.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        journalTextArea.setToolTipText("The selected journal's text");
        journalTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                journalTextAreaFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                journalTextAreaFocusLost(evt);
            }
        });
        journalTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                journalTextAreaMouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(journalTextArea);

        jLabel17.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel17.setText("Journal:");

        org.jdesktop.layout.GroupLayout journalPanelLayout = new org.jdesktop.layout.GroupLayout(journalPanel);
        journalPanel.setLayout(journalPanelLayout);
        journalPanelLayout.setHorizontalGroup(
            journalPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(journalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(journalPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(journalPanelLayout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(notesTextArea, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 809, Short.MAX_VALUE))
                    .add(journalPanelLayout.createSequentialGroup()
                        .add(journalPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(journalPanelLayout.createSequentialGroup()
                                .add(newButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel17))
                            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 725, Short.MAX_VALUE)))
                .addContainerGap())
        );
        journalPanelLayout.setVerticalGroup(
            journalPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(journalPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(journalPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(notesTextArea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(journalPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(journalPanelLayout.createSequentialGroup()
                        .add(journalPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel17)
                            .add(newButton))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE))
                .addContainerGap())
        );

        contactDetailTabPane.addTab("Journal", journalPanel);

        contactDetailsToolBar.setRollover(true);

        clearButton.setText("Clear");
        clearButton.setToolTipText("Clears the Form");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        contactDetailsToolBar.add(clearButton);

        saveButton.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        saveButton.setText("Save");
        saveButton.setToolTipText("Save Modifications");
        saveButton.setMaximumSize(new java.awt.Dimension(79, 25));
        saveButton.setMinimumSize(new java.awt.Dimension(79, 25));
        saveButton.setNextFocusableComponent(companyTextField);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        contactDetailsToolBar.add(saveButton);

        messageField.setEditable(false);
        messageField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        contactDetailsToolBar.add(messageField);

        org.jdesktop.layout.GroupLayout detailPanelLayout = new org.jdesktop.layout.GroupLayout(detailPanel);
        detailPanel.setLayout(detailPanelLayout);
        detailPanelLayout.setHorizontalGroup(
            detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, detailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, contactDetailTabPane)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, contactDetailsToolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        detailPanelLayout.setVerticalGroup(
            detailPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(detailPanelLayout.createSequentialGroup()
                .add(contactDetailsToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(contactDetailTabPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 358, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, contactTableScrollPane)
                    .add(contactTableToolPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(detailPanel, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(contactTableScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(contactTableToolPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(detailPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void invoiceTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_invoiceTableKeyPressed

        int kc = evt.getKeyCode();

        if (kc == evt.VK_ADD) {

            takePayment();
            return;

        }

        if (kc == evt.VK_ENTER) {

            viewInvoice();
            return;

        }

    }//GEN-LAST:event_invoiceTableKeyPressed

    private void toggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleButtonActionPerformed

        if (detailPanel.isVisible()) {

            detailPanel.setVisible(false);
            toggleButton.setText("More Detail");
            toggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Up.png")));

        } else {

            detailPanel.setVisible(true);
            toggleButton.setText("Less Detail");
            toggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Down.png")));
        }

        findField.requestFocus();
    }//GEN-LAST:event_toggleButtonActionPerformed

    private void invoiceTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_invoiceTableMouseClicked

        if (evt.getClickCount() == 2) {

//            if (!accessKey.checkManager(300)) {
//                accessKey.showMessage("Invoice Manager");
//                return;
//            }
            viewInvoice();

        }

    }//GEN-LAST:event_invoiceTableMouseClicked

    private void labelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_labelButtonActionPerformed
//        if (!accessKey.checkConnections(300)) {
//            accessKey.showMessage("Labels");
//            return;
//        }

        if (contactTable.getSelectedRow() > -1) {

            new ConnLabelDialog(null, true, contactTable.getModel(), contactTable.getSelectedRows());

        } else {

            javax.swing.JOptionPane.showMessageDialog(null, "Select rows from the Connections table to create labels.");

        }

    }//GEN-LAST:event_labelButtonActionPerformed

    private void contactTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_contactTextFieldFocusGained

        if (contactTextField.getText().trim().equals("")) {

            contactTextField.setText(firstTextField.getText().trim() + " " + lastTextField.getText().trim());

        }


    }//GEN-LAST:event_contactTextFieldFocusGained

    private void wwwButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wwwButtonActionPerformed
        /* Add protocol info if none is specified */
        if (!wwwTextField.getText().toUpperCase().contains("HTTP://") && !wwwTextField.getText().toUpperCase().contains("FTP://")) {
            wwwTextField.setText("http://" + wwwTextField.getText());
        }

        if (Desktop.isDesktopSupported()) {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {

                    Desktop.getDesktop().mail(new URI(wwwTextField.getText()));
                    return;
                } catch (Exception ex) {
                    //try the old manual method below
                }
            }
        }

        try {
            Desktop.getDesktop().browse(new URI(wwwTextField.getText()));
        } catch (URISyntaxException ex) {
            ExceptionService.showErrorDialog(this, ex, "URL is invalid");
        } catch (IOException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error opening url with local browser");
        }


    }//GEN-LAST:event_wwwButtonActionPerformed

    private void emailButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emailButtonActionPerformed

        if (emailTextField.getText().equals("") || emailTextField.getText().length() < 8) {
            /*REGEX*/

            javax.swing.JOptionPane.showMessageDialog(null, "You need to enter a good email address.");
            return;

        }

        if (Desktop.isDesktopSupported()) {
            if (Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                try {

                    Desktop.getDesktop().mail(new URI("mailto:" + emailTextField.getText()));
                    return;
                } catch (Exception ex) {
                    //try the old manual method below
                }
            }
        }

        try {
            Desktop.getDesktop().browse(new URI("mailto:" + emailTextField.getText()));
        } catch (URISyntaxException ex) {
            ExceptionService.showErrorDialog(this, ex, "URL is invalid");
        } catch (IOException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error opening url with local browser");
        }
       

    }//GEN-LAST:event_emailButtonActionPerformed

    private void zipTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_zipTextFieldKeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {

            zipAction();

        }


    }//GEN-LAST:event_zipTextFieldKeyPressed

    private void zipButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zipButtonActionPerformed

        zipAction();


    }//GEN-LAST:event_zipButtonActionPerformed

    private void takePayment() {

        int selectedRow = invoiceTable.getSelectedRow();
        var tableModel = (CustomerInvoiceListTableModel) invoiceTable.getModel();

        if (selectedRow < 0) {
            return; //TODO: show dialog?
        }
        var invoice = tableModel.getValueAt(selectedRow);

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID ) {
            JOptionPane.showMessageDialog(this, "Invoice is marked as paid.");
            invoiceTable.changeSelection(selectedRow, 0, false, false);
            invoiceTable.requestFocus();
            return;
        } else {
            PaymentDialog pd = new PaymentDialog(parentWin, true, invoice);
            pd.setVisible(true);
            populateInvoices(false);
        }

        invoiceTable.changeSelection(selectedRow, 0, false, false);
        invoiceTable.requestFocus();

    }

    private void findFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_findFieldFocusGained

        findField.selectAll();

    }//GEN-LAST:event_findFieldFocusGained

    private void viewInvoice() {

        int row = invoiceTable.getSelectedRow();

        if (invoiceTable.getSelectedRow() > -1) {

            var tableModel = (CustomerInvoiceListTableModel) invoiceTable.getModel();

            var invoice = (Invoice) tableModel.getValueAt(invoiceTable.getSelectedRow());

            if (invoiceToggleButton.getText().endsWith("Quotes")) {

                if (invoice != null) {
                    /* Opening quotes */
                    InvoiceApp id = new InvoiceApp(parentWin, true);
                    id.setInvoice(invoice);
                    id.displayApp();
                    id.dispose();
                }
            }

        }
        this.populateInvoices(false);
        invoiceTable.changeSelection(row, 0, false, false);
        invoiceTable.requestFocus();

    }

    private void viewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewButtonActionPerformed

//        if (!accessKey.checkManager(300)) {
//            accessKey.showMessage("Invoice Manager");
//            return;
//        }
        viewInvoice();

    }//GEN-LAST:event_viewButtonActionPerformed

    private void journalTextAreaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_journalTextAreaMouseClicked


    }//GEN-LAST:event_journalTextAreaMouseClicked

    private void journalTextAreaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_journalTextAreaFocusLost

        if (!journalList.isEnabled()) {

            saveJournal();
            journalList.requestFocus();

        }


    }//GEN-LAST:event_journalTextAreaFocusLost

    private void journalTextAreaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_journalTextAreaFocusGained
        journalList.setEnabled(false);
    }//GEN-LAST:event_journalTextAreaFocusGained

    private void contactTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_contactTableKeyReleased

        if (contactTable.getSelectedRow() > -1) {
            Integer key = (Integer) contactTable.getModel().getValueAt(contactTable.getSelectedRow(), 0);
            edit_key = key;
            populateFields();
            setFieldsEnabled(true);

            saveButton.setEnabled(true);

        }

    }//GEN-LAST:event_contactTableKeyReleased

    private void journalListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_journalListMouseClicked

        if (!journalList.isEnabled() && companyTextField.isEnabled()) {
            saveJournal();
            journalList.requestFocus();
        }

        if (companyTextField.isEnabled()) {
            getJournal();
        }


    }//GEN-LAST:event_journalListMouseClicked

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed

        if (currentContact.getId() != null) {
            newJournal();
        }

    }//GEN-LAST:event_newButtonActionPerformed

    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed

        if (contactTable.getModel().getRowCount() > 0) {
//            if (!accessKey.checkExports(500)) {
//                accessKey.showMessage("Export");
//                return;
//            }
            String home = System.getProperty("user.home");
            if (System.getProperty("os.name").contains("Windows")) {
                home = home + '\\' + "My Documents";
            }

            com.datavirtue.axiom.ui.OldFileDialog fd = new com.datavirtue.axiom.ui.OldFileDialog(parentWin, true, home, "export.csv");

            fd.setVisible(true);

            if (!fd.getPath().equals("")) {
                export(fd.getPath());
            }
        }
    }//GEN-LAST:event_exportButtonActionPerformed

    private void findFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_findFieldKeyPressed

        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {

            find();

        }
    }//GEN-LAST:event_findFieldKeyPressed

    private void contactTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_contactTableKeyPressed

        if (selectMode) {

            if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {

                if (contactTable.getSelectedRow() > -1 && selectMode) {
                    var tableModel = (ContactsTableModel) contactTable.getModel();
                    returnValue = (Contact) tableModel.getValueAt(contactTable.getSelectedRow());
                    this.setVisible(false);
                }

            }

        }


    }//GEN-LAST:event_contactTableKeyPressed

    private void companyTextFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_companyTextFieldMouseClicked

        if (!companyTextField.isEnabled()) {

//            if (!accessKey.checkConnections(200)) {
//                accessKey.showMessage("Create");
//                return;
//            }
            clearFields();
            setFieldsEnabled(true);

            companyTextField.requestFocus();

        }


    }//GEN-LAST:event_companyTextFieldMouseClicked

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed

        if (contactTable.getSelectedRow() > -1) {

//            if (!accessKey.checkConnections(500)) {
//                accessKey.showMessage("Delete");
//                return;
//            }
            int a = JOptionPane.showConfirmDialog(this, "Delete Selected Record?", "ERASE", JOptionPane.YES_NO_OPTION);

            if (a == 0) {
                try {
                    int numberDeleted = contactService.delete(currentContact);
                    if (numberDeleted > 0) {
                        clearFields();
                        setFieldsEnabled(false);
                        refreshTable();
                        JOptionPane.showMessageDialog(null, "The record was deleted.");
                    }
                } catch (SQLException e) {
                    ExceptionService.showErrorDialog(this, e, "Error deleting contact");
                }
            }
        }
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void refreshTable() {

        contactTable.setModel(filter());
        //setView(vals);

    }


    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed

        clearFields();
        setFieldsEnabled(false);


    }//GEN-LAST:event_clearButtonActionPerformed

    private boolean checkDuplicates() throws SQLException {
        if (currentContact.getId() == null
                && contactService.newContactCandidateExists(
                        companyTextField.getText().trim(),
                        emailTextField.getText().trim(),
                        phoneTextField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "The email address, company name, or phone number is already used in another contact.", "Duplicate contact", JOptionPane.OK_OPTION);
            return false;

        }
        return true;
    }

    private void saveAction() {

//        if (!accessKey.checkConnections(200)) {
//            accessKey.showMessage("Create, Edit");
//            return;
//        }
        if (custCheckBox.isSelected() == false && supplierCheckBox.isSelected() == false) {
            int a = JOptionPane.showConfirmDialog(this, "You did NOT select 'Customer' OR 'Supplier'.  Is this ok? ", "No Category", JOptionPane.YES_NO_OPTION);
            if (a != 0) {
                return;
            }
        }

        if (companyTextField.getText().trim().equals("") && firstTextField.getText().trim().equals("")
                && contactTextField.getText().trim().equals("")) {

            JOptionPane.showMessageDialog(this, "You have to provide some type of contact data; Company, First Name, or Contact.", "Form Problem!", JOptionPane.OK_OPTION);
            return;
        }

//        if (!checkDuplicates()) {
//            return;
//        }
        currentContact.setCompanyName(companyTextField.getText().trim());
        currentContact.setFirstName(firstTextField.getText().trim());
        currentContact.setLastName(lastTextField.getText().trim());
        currentContact.setAddress1(addressTextField.getText().trim());
        currentContact.setAddress2(suiteTextField.getText().trim());
        currentContact.setCity(cityTextField.getText().trim());
        currentContact.setState(stateTextField.getText().trim());
        currentContact.setPostalCode(zipTextField.getText().trim());
        currentContact.setContactName(contactTextField.getText().trim());
        currentContact.setPhoneNumber(phoneTextField.getText().trim());
        currentContact.setFax(faxTextField.getText().trim());
        currentContact.setEmail(emailTextField.getText().trim());
        currentContact.setWebLink(wwwTextField.getText().trim());
        currentContact.setNotes(notesTextArea.getText().trim());
        currentContact.setCustomer(custCheckBox.isSelected());
        currentContact.setVendor(supplierCheckBox.isSelected());
        currentContact.setCountryCode((String) countryCombo.getSelectedItem());
        currentContact.setTaxable1(tax1CheckBox.isSelected());
        currentContact.setTaxable2(tax2CheckBox.isSelected());

        try {
            contactService.save(currentContact);
        } catch (SQLException e) {
            ExceptionService.showErrorDialog(this, e, "Error Saving Contact");
            return;
        }

        clearFields();
        setFieldsEnabled(false);

        saveButton.setEnabled(false);
        allRadio.setSelected(true);
        refreshTable();

        /*  select  */
        int row = ((ContactsTableModel) contactTable.getModel()).getCollection().indexOf(currentContact);

        if (row <= contactTable.getModel().getRowCount()) {
            contactTable.changeSelection(row, 0, false, false);
        }

    }


    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed

        saveAction();

    }//GEN-LAST:event_saveButtonActionPerformed

    private void importCountries() {

        JFileChooser fileChooser = DV.getFileChooser("c:/1Data/Data Virtue/Research/International/");

        File f = fileChooser.getSelectedFile();

        int[] r = {0, 1, 2, 3, 4, 5};
        //db.csvImport("countries", f, false, r, false);

    }

    private void notesTextAreaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_notesTextAreaFocusGained
        messageField.setText("The Misc field can be used to store any information. (Searchable)");
    }//GEN-LAST:event_notesTextAreaFocusGained

    private void notesTextAreaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_notesTextAreaFocusLost
        messageField.setText("Remember to click 'Save' when you modify a record.");
    }//GEN-LAST:event_notesTextAreaFocusLost

    private void shipToButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shipToButtonActionPerformed
        if (this.currentContact.getId() != null) {
            shipToAction();
        }

    }//GEN-LAST:event_shipToButtonActionPerformed

    private void invoiceToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoiceToggleButtonActionPerformed
        populateInvoices(true);
    }//GEN-LAST:event_invoiceToggleButtonActionPerformed

    private void purchaseHistoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_purchaseHistoryButtonActionPerformed
        doPurchaseReport();
    }//GEN-LAST:event_purchaseHistoryButtonActionPerformed

    private void invoiceReportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoiceReportButtonActionPerformed

        doInvoiceReport();

    }//GEN-LAST:event_invoiceReportButtonActionPerformed

    private void phoneTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_phoneTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_phoneTextFieldActionPerformed

    private void jLabel9MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel9MouseClicked
        find();
    }//GEN-LAST:event_jLabel9MouseClicked

    private void allRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_allRadioActionPerformed
        refreshTable();
        clearFields();
        this.setFieldsEnabled(false);
    }//GEN-LAST:event_allRadioActionPerformed

    private void custRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_custRadioActionPerformed
        refreshTable();
        clearFields();
        this.setFieldsEnabled(false);
    }//GEN-LAST:event_custRadioActionPerformed

    private void suppRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_suppRadioActionPerformed
        refreshTable();
        clearFields();
        this.setFieldsEnabled(false);
    }//GEN-LAST:event_suppRadioActionPerformed

    private void unpaidRadioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unpaidRadioActionPerformed
        refreshTable();
        clearFields();
        this.setFieldsEnabled(false);
    }//GEN-LAST:event_unpaidRadioActionPerformed

    private void selectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectButtonActionPerformed

        if (contactTable.getSelectedRow() > -1) {
            var tableModel = (ContactsTableModel) contactTable.getModel();
            returnValue = (Contact) tableModel.getValueAt(contactTable.getSelectedRow());
            this.dispose();

        }
    }//GEN-LAST:event_selectButtonActionPerformed

    private void voidButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_voidButtonActionPerformed
        returnValue = null;
        this.setVisible(false);
    }//GEN-LAST:event_voidButtonActionPerformed

    private void searchFieldComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchFieldComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchFieldComboActionPerformed

    private void contactTableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_contactTableMousePressed
         int mouseButton = evt.getButton();
        if (mouseButton == evt.BUTTON2 || mouseButton == evt.BUTTON3) {
            return;
        }
        //on Double Click

        if (selectMode) {

            if (evt.getClickCount() == 2) {

                int row = contactTable.rowAtPoint(new Point(evt.getX(), evt.getY()));

                if (contactTable.getSelectedRow() > -1) {
                    var tableModel = (ContactsTableModel) contactTable.getModel();
                    returnValue = (Contact) tableModel.getValueAt(row);
                    this.setVisible(false);
                }
            }
        }

        populateFields();
    }//GEN-LAST:event_contactTableMousePressed

    private void doInvoiceReport() {
        int r = contactTable.getSelectedRow();

        if (r < 0) {
            return;
        }
//        if (!accessKey.checkReports(500)) {
//            accessKey.showMessage("Customer/Supplier Reports");
//            return;
//        }

        int k = (Integer) contactTable.getModel().getValueAt(r, 0);

        if (k > 0) {
            // ReportFactory.generateCustomerStatement(application, this.currentContact);
        }

    }

    private void doPurchaseReport() {

//        if (!accessKey.checkReports(500)) {
//            accessKey.showMessage("Customer/Supplier Reports");
//            return;
//        }
        int row = contactTable.getSelectedRow();

        if (row < 0) {
            return;
        }

        int k = edit_key;

        //java.util.ArrayList al = db.search("invoice", 11, Integer.toString(k), false);
//        if (al == null || al.size() < 1) {
//
//            javax.swing.JOptionPane.showMessageDialog(null,
//                    "No invoices found for this contact.");
//            return;
//        }
//
//        k = (Integer) contactTable.getModel().getValueAt(row, 0);
//        PurchaseHistoryReport phr = new PurchaseHistoryReport(application);
//        phr.SetTitle("Customer Purchase History Report");
//        phr.setCustomer(k);
//        phr.buildReport();
//        new ReportTableDialog(parentWin, true, phr, props);
    }

    private void shipToAction() {
        var dialog = new ContactShippingDialog(parentWin, true, this.currentContact.getId(), false);
        dialog.displayApp();

    }

    private void getCountry() {

        int[] r = {0};

        //TableView tv = new TableView(parentWin, true, db, "countries", 0,
        //       "Select a country from the table.", r);
        // tv.dispose();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel addressPanel;
    private javax.swing.JTextField addressTextField;
    private javax.swing.JToggleButton allRadio;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JTextField cityTextField;
    private javax.swing.JButton clearButton;
    private javax.swing.JTextField companyTextField;
    private javax.swing.JTabbedPane contactDetailTabPane;
    private javax.swing.JToolBar contactDetailsToolBar;
    private javax.swing.JPanel contactPanel;
    private javax.swing.JTable contactTable;
    private javax.swing.JScrollPane contactTableScrollPane;
    private javax.swing.JPanel contactTableToolPanel;
    private javax.swing.JTextField contactTextField;
    private javax.swing.JComboBox countryCombo;
    private javax.swing.JCheckBox custCheckBox;
    private javax.swing.JToggleButton custRadio;
    private javax.swing.JButton deleteButton;
    private javax.swing.JPanel detailPanel;
    private javax.swing.JButton emailButton;
    private javax.swing.JTextField emailTextField;
    private javax.swing.JButton exportButton;
    private javax.swing.JTextField faxTextField;
    private javax.swing.JToolBar filterToolbar;
    private javax.swing.JTextField findField;
    private javax.swing.JTextField firstTextField;
    private javax.swing.JToolBar functionToolbar;
    private javax.swing.JLabel invoiceLabel;
    private javax.swing.JButton invoiceReportButton;
    private javax.swing.JTable invoiceTable;
    private javax.swing.JButton invoiceToggleButton;
    private javax.swing.JToolBar invoiceToolbar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JList journalList;
    private javax.swing.JPanel journalPanel;
    private javax.swing.JTextPane journalTextArea;
    private javax.swing.JLabel keyLabel;
    private javax.swing.JButton labelButton;
    private javax.swing.JTextField lastTextField;
    private javax.swing.JTextField messageField;
    private javax.swing.JButton newButton;
    private javax.swing.JTextField notesTextArea;
    private javax.swing.JTextField phoneTextField;
    private javax.swing.JButton purchaseHistoryButton;
    private javax.swing.JButton saveButton;
    private javax.swing.JComboBox searchFieldCombo;
    private javax.swing.JButton selectButton;
    private javax.swing.JButton shipToButton;
    private javax.swing.JTextField stateTextField;
    private javax.swing.JTextField suiteTextField;
    private javax.swing.JToggleButton suppRadio;
    private javax.swing.JCheckBox supplierCheckBox;
    private javax.swing.JCheckBox tax1CheckBox;
    private javax.swing.JCheckBox tax2CheckBox;
    private javax.swing.JButton toggleButton;
    private javax.swing.JToggleButton unpaidRadio;
    private javax.swing.JButton viewButton;
    private javax.swing.JButton voidButton;
    private javax.swing.JButton wwwButton;
    private javax.swing.JTextField wwwTextField;
    private javax.swing.JButton zipButton;
    private javax.swing.JTextField zipTextField;
    // End of variables declaration//GEN-END:variables

}
