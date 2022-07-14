/*
 * SettingsDialog.java
 *
 * Created on July 4, 2006, 11:38 AM
 ** Copyright (c) Data Virtue 2006
 * revised Dec 2022 
 */
package com.datavirtue.axiom.ui;

import com.datavirtue.axiom.ui.util.NewEmail;
import com.datavirtue.axiom.ui.util.LimitedDocument;
import java.io.*;
import javax.swing.*;
import com.google.inject.Injector;
import com.datavirtue.axiom.services.DiService;
import com.datavirtue.axiom.services.AppSettingsService;
import java.sql.SQLException;
import com.datavirtue.axiom.models.settings.AppSettings;
import com.datavirtue.axiom.models.settings.CompanySettings;
import com.datavirtue.axiom.models.settings.DataSettings;
import com.datavirtue.axiom.models.settings.EmailSettings;
import com.datavirtue.axiom.models.settings.FontSetting;
import com.datavirtue.axiom.models.settings.InternetSettings;
import com.datavirtue.axiom.models.settings.InventorySettings;
import com.datavirtue.axiom.models.settings.InvoiceSettings;
import com.datavirtue.axiom.models.settings.OutputSettings;
import com.datavirtue.axiom.models.settings.SecuritySettings;
import com.datavirtue.axiom.services.DatabaseService;
import com.datavirtue.axiom.services.ExceptionService;
import com.datavirtue.axiom.services.ImageService;
import com.datavirtue.axiom.services.LocalSettingsService;
import com.datavirtue.axiom.services.UserService;
import com.datavirtue.axiom.services.util.CurrencyUtil;
import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.services.util.DVNET;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.prefs.BackingStoreException;

/**
 *
 * @author Sean K Anderson - Data Virtue
 * @copyright Copyright Data Virtue 2006-2022 All Rights Reserved.
 */
public class SettingsDialog extends javax.swing.JDialog implements AxiomApp {

    private final AppSettingsService appSettingsService;
    private ImageService imageService;
    private java.awt.Frame parentWin;
    private java.awt.Font companyFont = new java.awt.Font("Roman", Font.PLAIN, 12);
    private Cursor defaultCursor;
    private Image winIcon;

    public SettingsDialog(java.awt.Frame parent, boolean modal, int tabIndex) {
        super(parent, modal);
        parentWin = parent;

        Toolkit tools = Toolkit.getDefaultToolkit();
        winIcon = tools.getImage(getClass().getResource("/Orange.png"));
        initComponents();

        themeComboBox.setModel(new DefaultComboBoxModel(LocalSettingsService.THEME_NAMES));

        Injector injector = DiService.getInjector();
        appSettingsService = injector.getInstance(AppSettingsService.class);
        appSettingsService.setObjectType(AppSettings.class);
        imageService = injector.getInstance(ImageService.class);

        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
        this.setLocation(dim.width, dim.height);

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                int a = javax.swing.JOptionPane.showConfirmDialog(null, "Do you want to save any changes?", "Save Settings?", JOptionPane.YES_NO_OPTION);
                if (a == 0) {

                    saveSettings();

                } else {
                    ((JDialog) evt.getSource()).dispose();
                }
            }
        });
        
        // the Netbeans Swing builder is settings these then I do it again to left-justify them and set the font....can this be done in the builder?
        this.addTab(0, "My Company", "/Aha-24/enabled/Globe.png");
        this.addTab(1, "Integrations  ", "/Aha-24/enabled/Connect.png");
        this.addTab(2, "Backups   ", "/Aha-24/enabled/Archive.png");
        this.addTab(3, "Security  ", "/Aha-24/enabled/Lock.png");
        this.addTab(4, "Invoice   ", "/Aha-24/enabled/Barcode scanner1.png");
        this.addTab(5, "Layouts   ", "/Aha-24/enabled/Measure.png");
        this.addTab(6, "Inventory ", "/Aha-24/enabled/Book library.png");
        this.addTab(7, "Automation", "/Aha-24/enabled/Export table.png");
        this.addTab(8, "Info      ", "/Aha-24/enabled/Info.png");

        invoicePrefixField.setDocument(new LimitedDocument(3));
        quotePrefixField.setDocument(new LimitedDocument(3));
        jTabbedPane1.setSelectedIndex(tabIndex);

    }

    private void addTab(int index, String title, String iconRes) {
        JLabel lbl = new JLabel(title);
        Icon icon = new ImageIcon(getClass().getResource(iconRes));
        lbl.setIcon(icon);

        lbl.setIconTextGap(5);
        lbl.setHorizontalTextPosition(JLabel.RIGHT);
        lbl.setHorizontalAlignment(JLabel.RIGHT);
        lbl.setFont(new Font("courier", 0, 12));
        jTabbedPane1.setTabComponentAt(index, lbl);
    }

    public void displayApp() {

        var user = UserService.getCurrentUser();

        if (!user.isAdmin() && user.getSettings() < 500) {
            JOptionPane.showMessageDialog(this, "Please see the admin about permissions.", "Access denied", JOptionPane.OK_OPTION);
            this.dispose();
            return;
        }

        posPrinterPaperWidthSpinner.getModel().setValue(80);
        ///boolean settingsFileExists = (new File(workingPath + "settings.ini")).exists();
        try {
            var settings = appSettingsService.getObject();

            if (settings == null) {
                createNewDefaultSettings();
            }
            loadSettings();

        } catch (SQLException e) {
            ExceptionService.showErrorDialog(this, e, "Error loading settings");
        }

        this.setVisible(true);

    }

    private void createNewDefaultSettings() throws SQLException {
        appSettingsService.set(new AppSettings());
        var settings = appSettingsService.getObject();

        settings.setCompany(new CompanySettings());
        settings.setInternet(new InternetSettings());
        settings.getInternet().setEmailSettings(new EmailSettings());
        settings.setInventory(new InventorySettings());
        settings.setInvoice(new InvoiceSettings());
        settings.setOutput(new OutputSettings());
        settings.setSecurity(new SecuritySettings());
        settings.setBackups(new DataSettings());

        var company = settings.getCompany();
        company.setCompanyName("My Company");
        company.setAddress1("");
        company.setAddress2("");
        company.setCity("");
        company.setState(""); // not in use
        company.setPostalCode(""); // not in use
        company.setPhoneNumber("");
        company.setEmail("");
        company.setTaxId("");
        company.setShowTaxIdOnInvoice(true);
        company.setCountryCode(Locale.getDefault().getCountry());

        var email = settings.getInternet().getEmailSettings();
        email.setServerAddress("smtp");
        email.setServerPort("25");
        email.setUseSSL(true);
        email.setServerUsername("");
        email.setServerPassword("");
        email.setReturnAddress("reply@fakeemail.com");

        settings.getInternet().setShowRemoteMessage(true);

        var backups = settings.getBackups();
        backups.setPrimaryBackupPath("");
        backups.setSecondaryBackupPath("");
        backups.setDisableBackupFeatures(false);

        var invoice = settings.getInvoice();
        invoice.setTax1Name("Tax1");
        invoice.setTax2Name("Tax2");
        invoice.setTax1Rate(0);
        invoice.setTax2Rate(0);
        invoice.setShowTax1(true);
        invoice.setShowTax2(true);
        invoice.setInterestRate(0);
        invoice.setInterestGracePeriodInDays(30);
        invoice.setCurrencySymbol("$");
        invoice.setCashRoundingRate(0);
        invoice.setInvoiceName("I N V O I C E");
        invoice.setInvoicePrefix("INV");
        invoice.setQuoteName("Q U O T E");
        invoice.setQuotePrefix("QT");
        invoice.setBillToLabel("Bill To:");

        invoice.setPointOfSaleMode(false);
        invoice.setDefaultBarcodeScanField("upc");
        invoice.setRecieptPaperWidthInMm(80);
        invoice.setPrintZeros(true);
        invoice.setProcessPaymentOnPosting(true);

        invoice.setInkSaver(false);

        var output = settings.getOutput();
        output.setPdfReaderUri("");
        output.setUseSystemDefaultPdfReader(true);
        output.setPaymentSystemUri("");
        output.setPaymentSystemUriIsWeblink(true);
        output.setUsePaymentSystemForCard(true);
        output.setUsePaymentSystemForChecks(true);
        output.setInvoiceDestinationUri("");
        output.setQuoteDestinationUri("");
        output.setReportDestinationUri("");
        output.setWatermarkImage(null);
        output.setUserWatermarkInReports(false);

        String iValue = "10000";
        String qValue = "10000";
        String tmp = "";

        do {

            tmp = javax.swing.JOptionPane.showInputDialog("Please provide a starting INVOICE # between 1000 and 50000", iValue);

            if (tmp == null) {
                iValue = "10000";
            } else {
                iValue = tmp.trim();
            }

            if (DV.validIntString(iValue) && Integer.parseInt(iValue) < 50000 && Integer.parseInt(iValue) > 999) {
                break;
            } else {
                continue;
            }

        } while (true);

        do { //setup starting quote number

            tmp = javax.swing.JOptionPane.showInputDialog("Please provide a starting QUOTE # between 1000 and 50000", qValue);

            if (tmp == null) {
                qValue = "10000";
            } else {
                qValue = tmp.trim();
            }

            if (DV.validIntString(qValue) && Integer.parseInt(qValue) < 50000 && Integer.parseInt(iValue) > 999) {
                break;
            } else {
                continue;
            }

        } while (true);

        invoice.setNextInvoiceNumber(Integer.parseInt(iValue));
        invoice.setNextQuoteNumber(Integer.parseInt(qValue));
        appSettingsService.save();
    }

    private void loadSettings() throws SQLException {

        try {
            var localSettings = LocalSettingsService.getLocalAppSettings();
            var theme = localSettings.getTheme();
            this.themeComboBox.setSelectedItem(theme);
            var connectionString = localSettings.getConnectionString();
            this.currentConnectionStringTextField.setText(connectionString);
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error loading local settings");
        }

        var settings = appSettingsService.getObject();

        var internet = settings.getInternet();
        showRemoteMessageCheckbox.setSelected(internet.isShowRemoteMessage());

        var company = settings.getCompany();
        companyNameField.setText(company.getCompanyName());
        address1Field.setText(company.getAddress1());
        address2Field.setText(company.getAddress2());
        cityStateZipField.setText(company.getCity());
        phoneField.setText(company.getPhoneNumber());

        String region = company.getCountryCode();
        if (region.equals("US")) {
            countryCombo.setSelectedItem("US");
        }
        if (region.equals("CA")) {
            countryCombo.setSelectedItem("CA");
        }
        if (region.equals("AU")) {
            countryCombo.setSelectedItem("AU");
        }
        if (region.equals("UK")) {
            countryCombo.setSelectedItem("UK");
        }
        if (region.equals("IN")) {
            countryCombo.setSelectedItem("IN");
        }
        if (region.equals("ZA")) {
            countryCombo.setSelectedItem("ZA");
        }
        if (region.equals("NZ")) {
            countryCombo.setSelectedItem("NZ");
        }
        if (region.equals("PH")) {
            countryCombo.setSelectedItem("PH");
        }

        var font = company.getCompanyFont();
        if (font == null) {
            companyFont = new java.awt.Font("Roman", Font.PLAIN, 12);
        } else {
            companyFont = new java.awt.Font(font.getFontName(), font.getFontStyle(), font.getFontSize());
        }
        companyNameField.setFont(companyFont);

        if (company.getCompanyLogo() != null && !company.getCompanyLogo().isEmpty()) {
            try {
                var image = imageService.convertBase64PngToBufferedImage(company.getCompanyLogo());
                imageService.resizeAndSetImageToJLabel(image, this.companyLogoImage);
            } catch (Exception e) {
                ExceptionService.showErrorDialog(parentWin, e, "Error loading company logo from settings");
            }
        }

        taxIdField.setText(company.getTaxId());
        showTaxIdCheckBox.setSelected(company.isShowTaxIdOnInvoice());

        var email = settings.getInternet().getEmailSettings();
        emailServerField.setText(email.getServerAddress());
        emailPortField.setText(email.getServerPort());
        emailSslCheckbox.setSelected(email.isUseSSL());
        emailAddressField.setText(email.getReturnAddress());
        emailUserName.setText(email.getServerUsername());
        emailPassword.setText(email.getServerPassword());

        // TODO: show logos in image panels
        try {
            dataFolderField.setText(DatabaseService.getConnection().getUrl());
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error getting database file location");
        }

        var output = settings.getOutput();
        reportOutputFolderField.setText(output.getReportDestinationUri());
        quoteOutputFolderField.setText(output.getQuoteDestinationUri());
        invoiceOutputFolderField.setText(output.getInvoiceDestinationUri());

        pdfReaderBinField.setText(output.getPdfReaderUri());
        useSystemDefaultPdfReaderCheckbox.setSelected(output.isUseSystemDefaultPdfReader());

        paymentSystemUriField.setText(output.getPaymentSystemUri());
        paymentSystemIsWebCheckbox.setSelected(output.isPaymentSystemUriIsWeblink());
        usePaymentSystemForCardsCheckbox.setSelected(output.isUsePaymentSystemForCard());
        usePaymentSystemForChecksCheckbox.setSelected(output.isUsePaymentSystemForChecks());

        //TODO: show watermark image
        useWatermarkOnReportsCheckbox.setSelected(output.isUserWatermarkInReports());

        var backups = settings.getBackups();
        backupFolderField.setText(backups.getPrimaryBackupPath());
        secondaryCheckBox.setSelected(backups.isDisableBackupFeatures());
        secondaryBackupFolderField.setText(backups.getSecondaryBackupPath());

        var invoice = settings.getInvoice();
        tax1Field.setText(Double.toString(invoice.getTax1Rate()));
        tax2Field.setText(Double.toString(invoice.getTax2Rate()));

        tax1NameField.setText(invoice.getTax1Name());
        tax2NameField.setText(invoice.getTax2Name());

        showTax1Box.setSelected(invoice.isShowTax1());
        showTax2Box.setSelected(invoice.isShowTax2());

        double rounding = invoice.getCashRoundingRate();

        cashRoundingCombo.setSelectedItem("N/A");
        if (rounding == .05) {
            cashRoundingCombo.setSelectedItem(".05");
        }
        if (rounding == .10) {
            cashRoundingCombo.setSelectedItem(".10");
        }

        try {
            posPrinterPaperWidthSpinner.getModel().setValue(invoice.getRecieptPaperWidthInMm());
        } catch (Exception e) {
            posPrinterPaperWidthSpinner.getModel().setValue(80);  //Default
        }

        updateInches();

        invoicePrefixField.setText(invoice.getInvoicePrefix());
        quotePrefixField.setText(invoice.getQuotePrefix());

        interestRateField.setText(Double.toString(invoice.getInterestRate()));

        if (invoice.getDefaultBarcodeScanField().equalsIgnoreCase("upc")) {
            defaultScanUpcRadio.setSelected(true);
        } else {
            defaultScanCodeRadio.setSelected(true);
        }

        interestGracePeriodField.setText(Integer.toString(invoice.getInterestGracePeriodInDays()));

        posModeBox.setSelected(invoice.isPointOfSaleMode());

        processPaymentWhenPostingCheckbox.setSelected(invoice.isProcessPaymentOnPosting());

        inkSaverCheckbox.setSelected(invoice.isInkSaver());

        //invoiceColorField.setBackground(Tools.stringToColor(props.getProp("INCOLOR")));
        //statementColorField.setBackground(Tools.stringToColor(props.getProp("STCOLOR")));
        var inventory = settings.getInventory();
        String weight = inventory.getWeightUnit();
        if (weight.equals("lbs")) {
            lbsRadio.setSelected(true);
        } else {
            kgsRadio.setSelected(true);
        }

        invoiceNameField.setText(invoice.getInvoiceName());
        quoteNameField.setText(invoice.getQuoteName());

        billToTextField.setText(invoice.getBillToLabel());
        catLineCheckBox.setSelected(inventory.isAddCategoryLineToInvoiceItems());

        currencyField.setText(invoice.getCurrencySymbol());

        printZerosCheckBox.setSelected(invoice.isPrintZeros());

        markupField.setText(Double.toString(inventory.getDefaultProductMarkupFactor()));

        quantityWarningCheckBox.setSelected(inventory.isIgnoreQuantityWarnings());

        osInfoLabel.setText(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        javaInfoLabel.setText(System.getProperty("java.runtime.name") + " " + System.getProperty("java.vm.version"));
        workingFolderInfoLabel.setText(System.getProperty("user.dir"));
        this.operatingSystemUserTextField.setText(System.getProperty("user.name"));

    }

    private AppSettings getCurrentSettings() throws SQLException {
        var settings = appSettingsService.getObject();

        if (settings == null) {
            ExceptionService.showErrorDialog(this, null, "Settings were not loaded properly");
        }
        return settings;
    }

    private void updateInches() {

        float inches = (Integer) posPrinterPaperWidthSpinner.getModel().getValue() * 0.0393700787f;
        inchLabel.setText(CurrencyUtil.money(inches));

    }

    private void saveSettings() {

        try {
            var settings = getCurrentSettings();
            var company = settings.getCompany();
            company.setCompanyName(this.companyNameField.getText());
            company.setAddress1(this.address1Field.getText());
            company.setAddress2(this.address2Field.getText());
            company.setCity(this.cityStateZipField.getText());
            company.setState("");
            company.setPostalCode("");
            company.setPhoneNumber(this.phoneField.getText());
            company.setEmail("");
            company.setTaxId(this.taxIdField.getText());
            company.setShowTaxIdOnInvoice(this.showTaxIdCheckBox.isSelected());
            company.setCountryCode((String) this.countryCombo.getSelectedItem());

            var font = this.companyFont;
            company.setCompanyFont(new FontSetting(font.getFontName(), font.getStyle(), font.getSize()));

            var email = settings.getInternet().getEmailSettings();
            email.setServerAddress(this.emailServerField.getText());
            email.setServerPort(this.emailPortField.getText());
            email.setUseSSL(this.emailSslCheckbox.isSelected());
            email.setServerUsername(this.emailUserName.getText());
            email.setServerPassword(this.emailPassword.getText());
            email.setReturnAddress(this.emailAddressField.getText());

            settings.getInternet().setShowRemoteMessage(this.showRemoteMessageCheckbox.isSelected());

            var backups = settings.getBackups();
            backups.setPrimaryBackupPath(this.backupFolderField.getText());
            backups.setSecondaryBackupPath(this.secondaryBackupFolderField.getText());
            backups.setDisableBackupFeatures(this.secondaryCheckBox.isSelected());

            var invoice = settings.getInvoice();
            invoice.setTax1Name(this.tax1NameField.getText());
            invoice.setTax2Name(this.tax2NameField.getText());
            invoice.setTax1Rate(Double.parseDouble(this.tax1Field.getText()));
            invoice.setTax2Rate(Double.parseDouble(this.tax2Field.getText()));
            invoice.setShowTax1(this.showTax1Box.isSelected());
            invoice.setShowTax2(this.showTax2Box.isSelected());
            invoice.setInterestRate(Double.parseDouble(this.interestRateField.getText()));
            invoice.setInterestGracePeriodInDays(Integer.parseInt(this.interestGracePeriodField.getText()));
            invoice.setCurrencySymbol(this.currencyField.getText());
            var roundingFactor = (String) this.cashRoundingCombo.getSelectedItem();
            invoice.setCashRoundingRate(
                    roundingFactor == null || roundingFactor.isBlank() || roundingFactor.equals("N/A")
                    ? 0
                    : Double.parseDouble(roundingFactor)
            );
            invoice.setInvoiceName(this.invoiceNameField.getText());
            invoice.setInvoicePrefix(this.invoicePrefixField.getText());
            invoice.setQuoteName(this.quoteNameField.getText());
            invoice.setQuotePrefix(this.quotePrefixField.getText());
            invoice.setBillToLabel(this.billToTextField.getText());

            invoice.setPointOfSaleMode(this.posModeBox.isSelected());
            invoice.setDefaultBarcodeScanField(
                    this.defaultScanUpcRadio.isSelected()
                    ? "upc"
                    : (this.defaultScanCodeRadio.isSelected()
                    ? "code"
                    : (this.defaultScanDescriptionRadio.isSelected()
                    ? "description"
                    : "")));

            invoice.setRecieptPaperWidthInMm((Integer) posPrinterPaperWidthSpinner.getModel().getValue());
            invoice.setPrintZeros(this.printZerosCheckBox.isSelected());
            invoice.setProcessPaymentOnPosting(this.processPaymentWhenPostingCheckbox.isSelected());

            invoice.setInkSaver(this.inkSaverCheckbox.isSelected());

            var inventory = settings.getInventory();
            inventory.setAddCategoryLineToInvoiceItems(this.catLineCheckBox.isSelected());
            inventory.setAllowPartialQuantitySales(this.partialQuantityCheckBox.isSelected());
            inventory.setWeightUnit(this.kgsRadio.isSelected() ? "metric" : "imperial");
            inventory.setDefaultInventorySearchField("upc");
            inventory.setIgnoreQuantityWarnings(this.quantityWarningCheckBox.isSelected());

            var output = settings.getOutput();
            output.setPdfReaderUri(this.pdfReaderBinField.getText());
            output.setUseSystemDefaultPdfReader(this.useSystemDefaultPdfReaderCheckbox.isSelected());
            output.setPaymentSystemUri(this.paymentSystemUriField.getText());
            output.setPaymentSystemUriIsWeblink(this.paymentSystemIsWebCheckbox.isSelected());
            output.setUsePaymentSystemForCard(this.usePaymentSystemForCardsCheckbox.isSelected());
            output.setUsePaymentSystemForChecks(this.usePaymentSystemForChecksCheckbox.isSelected());
            output.setInvoiceDestinationUri(this.invoiceOutputFolderField.getText());
            output.setQuoteDestinationUri(this.quoteOutputFolderField.getText());
            output.setReportDestinationUri(this.reportOutputFolderField.getText());
            output.setWatermarkImage(null);
            output.setUserWatermarkInReports(this.useWatermarkOnReportsCheckbox.isSelected());

            appSettingsService.save();
        } catch (SQLException e) {
            ExceptionService.showErrorDialog(this, e, "Error saving settings");
        }

        try {
            var localSettings = LocalSettingsService.getLocalAppSettings();
            var currentTheme = localSettings.getTheme();
            var selectedTheme = (String) themeComboBox.getSelectedItem();
            if (!selectedTheme.equals(currentTheme)) {
                localSettings.setTheme(selectedTheme);
                LocalSettingsService.saveLocalAppSettings(localSettings);
                LocalSettingsService.setLookAndFeel();
            }

        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error saving local settings");
        }

        this.dispose();

    }

    private void getCompanyFontFromUser() {
        FontDialog fd = new FontDialog(null, true, companyNameField.getFont());
        fd.setVisible(true);
        var font = fd.getChosenFont();
        this.companyFont = font;
        companyNameField.setFont(font);
        fd.dispose();
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
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        buttonGroup6 = new javax.swing.ButtonGroup();
        buttonGroup7 = new javax.swing.ButtonGroup();
        buttonGroup8 = new javax.swing.ButtonGroup();
        addressButtonGroup = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        companyInfoPanel = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        companyNameField = new javax.swing.JTextField();
        address1Field = new javax.swing.JTextField();
        address2Field = new javax.swing.JTextField();
        cityStateZipField = new javax.swing.JTextField();
        phoneField = new javax.swing.JTextField();
        companyFontButton = new javax.swing.JButton();
        taxIdField = new javax.swing.JTextField();
        jLabel39 = new javax.swing.JLabel();
        showTaxIdCheckBox = new javax.swing.JCheckBox();
        jLabel54 = new javax.swing.JLabel();
        countryCombo = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        logoBrowse = new javax.swing.JButton();
        alternateLogoBrowse = new javax.swing.JButton();
        companyLogoPanel = new javax.swing.JPanel();
        companyLogoImage = new javax.swing.JLabel();
        alternateLogoPanel = new javax.swing.JPanel();
        alternateLogoImage = new javax.swing.JLabel();
        jLabel56 = new javax.swing.JLabel();
        EDIPanel = new javax.swing.JPanel();
        showRemoteMessageCheckbox = new javax.swing.JCheckBox();
        jPanel19 = new javax.swing.JPanel();
        jLabel42 = new javax.swing.JLabel();
        jLabel57 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        jLabel59 = new javax.swing.JLabel();
        emailServerField = new javax.swing.JTextField();
        emailAddressField = new javax.swing.JTextField();
        emailUserName = new javax.swing.JTextField();
        emailPassword = new javax.swing.JPasswordField();
        testEmailButton = new javax.swing.JButton();
        jLabel60 = new javax.swing.JLabel();
        emailPortField = new javax.swing.JTextField();
        emailSslCheckbox = new javax.swing.JCheckBox();
        jLabel35 = new javax.swing.JLabel();
        backupPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        dataFolderField = new javax.swing.JTextField();
        backupFolderButton = new javax.swing.JButton();
        backupFolderField = new javax.swing.JTextField();
        secondaryCheckBox = new javax.swing.JCheckBox();
        secondaryButton = new javax.swing.JButton();
        secondaryBackupFolderField = new javax.swing.JTextField();
        securityPanel = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        manageUsersButton = new javax.swing.JButton();
        logButton = new javax.swing.JButton();
        invoicePanel = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        inkSaverCheckbox = new javax.swing.JCheckBox();
        processPaymentWhenPostingCheckbox = new javax.swing.JCheckBox();
        jLabel26 = new javax.swing.JLabel();
        defaultScanUpcRadio = new javax.swing.JRadioButton();
        defaultScanCodeRadio = new javax.swing.JRadioButton();
        posModeBox = new javax.swing.JCheckBox();
        posPrinterPaperWidthSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        inchLabel = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        defaultScanDescriptionRadio = new javax.swing.JRadioButton();
        jPanel12 = new javax.swing.JPanel();
        jLabel40 = new javax.swing.JLabel();
        currencyField = new javax.swing.JTextField();
        printZerosCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        invoiceColorField = new javax.swing.JTextField();
        statementColorField = new javax.swing.JTextField();
        jPanel13 = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        tax1NameField = new javax.swing.JTextField();
        jLabel51 = new javax.swing.JLabel();
        tax2NameField = new javax.swing.JTextField();
        jLabel45 = new javax.swing.JLabel();
        invoiceNameField = new javax.swing.JTextField();
        invoicePrefixField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        quotePrefixField = new javax.swing.JTextField();
        jLabel46 = new javax.swing.JLabel();
        quoteNameField = new javax.swing.JTextField();
        jLabel55 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        billToTextField = new javax.swing.JTextField();
        jPanel14 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        tax1Field = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        tax2Field = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        interestRateField = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        interestGracePeriodField = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        showTax1Box = new javax.swing.JCheckBox();
        showTax2Box = new javax.swing.JCheckBox();
        jLabel62 = new javax.swing.JLabel();
        cashRoundingCombo = new javax.swing.JComboBox();
        layoutPanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        layoutPathLabel = new javax.swing.JLabel();
        jLabel61 = new javax.swing.JLabel();
        invoiceLayoutComboBox = new javax.swing.JComboBox();
        layoutPathField = new javax.swing.JTextField();
        inventoryPanel = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        markupField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        lbsRadio = new javax.swing.JRadioButton();
        kgsRadio = new javax.swing.JRadioButton();
        quantityWarningCheckBox = new javax.swing.JCheckBox();
        catLineCheckBox = new javax.swing.JCheckBox();
        partialQuantityCheckBox = new javax.swing.JCheckBox();
        outputPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        invoiceButton = new javax.swing.JButton();
        invoiceOutputFolderField = new javax.swing.JTextField();
        quoteButton = new javax.swing.JButton();
        quoteOutputFolderField = new javax.swing.JTextField();
        reportButton = new javax.swing.JButton();
        reportOutputFolderField = new javax.swing.JTextField();
        watermarkBrowse = new javax.swing.JButton();
        watermarkImagePathField = new javax.swing.JTextField();
        useWatermarkOnReportsCheckbox = new javax.swing.JCheckBox();
        jLabel43 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel52 = new javax.swing.JLabel();
        paymentSystemUriField = new javax.swing.JTextField();
        paymentSystemIsWebCheckbox = new javax.swing.JCheckBox();
        paymentSystemBinPathBrowseButton = new javax.swing.JButton();
        jLabel53 = new javax.swing.JLabel();
        usePaymentSystemForCardsCheckbox = new javax.swing.JCheckBox();
        usePaymentSystemForChecksCheckbox = new javax.swing.JCheckBox();
        jLabel47 = new javax.swing.JLabel();
        pdfReaderBinField = new javax.swing.JTextField();
        pdfBinPathBrowseButton = new javax.swing.JButton();
        pdfAutoFindButton = new javax.swing.JButton();
        pdfRevertButton = new javax.swing.JButton();
        useSystemDefaultPdfReaderCheckbox = new javax.swing.JCheckBox();
        infoPanel = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        osInfoLabel = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        javaInfoLabel = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        workingFolderInfoLabel = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel38 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        themeComboBox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        currentConnectionStringTextField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        operatingSystemUserTextField = new javax.swing.JTextField();
        jPanel10 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        checkUpdatesButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        jLabel50 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Nevitium Settings");
        setIconImage(winIcon);
        setResizable(false);

        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        jTabbedPane1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N

        jPanel16.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPanel17.setBorder(javax.swing.BorderFactory.createTitledBorder("Company Information"));

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("Co Name");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("Address");

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel19.setText("Phone");

        companyFontButton.setText("Font");
        companyFontButton.setToolTipText("Set the font formatting for invoices.");
        companyFontButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                companyFontButtonActionPerformed(evt);
            }
        });

        taxIdField.setToolTipText("This is used to store and display a Tax ID on invoices for compliance.");

        jLabel39.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel39.setText("Tax ID");

        showTaxIdCheckBox.setText("Show tax ID on invoice");

        jLabel54.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel54.setText("Address Format:");

        countryCombo.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        countryCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "US", "CA", "AU", "UK", "ZA", "IN", "NZ", "PH" }));
        countryCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countryComboActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel9.setText("Company logo (Invoices etc...)");

        jLabel34.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel34.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel34.setText("Alternate logo");
        jLabel34.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        logoBrowse.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        logoBrowse.setText("Browse File");
        logoBrowse.setToolTipText("Press to select a logo file.");
        logoBrowse.setMargin(new java.awt.Insets(1, 4, 1, 4));
        logoBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoBrowseActionPerformed(evt);
            }
        });

        alternateLogoBrowse.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        alternateLogoBrowse.setText("Browse File");
        alternateLogoBrowse.setMargin(new java.awt.Insets(1, 4, 1, 4));
        alternateLogoBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alternateLogoBrowseActionPerformed(evt);
            }
        });

        companyLogoPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        companyLogoPanel.setPreferredSize(new java.awt.Dimension(165, 165));

        org.jdesktop.layout.GroupLayout companyLogoPanelLayout = new org.jdesktop.layout.GroupLayout(companyLogoPanel);
        companyLogoPanel.setLayout(companyLogoPanelLayout);
        companyLogoPanelLayout.setHorizontalGroup(
            companyLogoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(companyLogoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(companyLogoImage, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                .addContainerGap())
        );
        companyLogoPanelLayout.setVerticalGroup(
            companyLogoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(companyLogoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(companyLogoImage, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        alternateLogoPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        alternateLogoPanel.setPreferredSize(new java.awt.Dimension(165, 165));

        alternateLogoImage.setToolTipText("");

        org.jdesktop.layout.GroupLayout alternateLogoPanelLayout = new org.jdesktop.layout.GroupLayout(alternateLogoPanel);
        alternateLogoPanel.setLayout(alternateLogoPanelLayout);
        alternateLogoPanelLayout.setHorizontalGroup(
            alternateLogoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(alternateLogoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(alternateLogoImage, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                .addContainerGap())
        );
        alternateLogoPanelLayout.setVerticalGroup(
            alternateLogoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(alternateLogoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(alternateLogoImage, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel17Layout = new org.jdesktop.layout.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jLabel39, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel19, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel17Layout.createSequentialGroup()
                        .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, taxIdField)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, phoneField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(showTaxIdCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel17Layout.createSequentialGroup()
                        .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel17Layout.createSequentialGroup()
                                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(countryCombo, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .add(jLabel54))
                                .add(18, 18, 18)
                                .add(companyFontButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 87, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, cityStateZipField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, companyNameField)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, address2Field)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, address1Field))
                            .add(jPanel17Layout.createSequentialGroup()
                                .add(companyLogoPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(alternateLogoPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .add(0, 86, Short.MAX_VALUE))
                    .add(jPanel17Layout.createSequentialGroup()
                        .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(logoBrowse)
                            .add(jLabel9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 165, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(18, 18, 18)
                        .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(alternateLogoBrowse)
                            .add(jLabel34, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel16)
                    .add(companyNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(address1Field, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(address2Field, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(cityStateZipField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel19)
                    .add(phoneField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(taxIdField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel39)
                    .add(showTaxIdCheckBox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel54)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(countryCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(companyFontButton))
                .add(18, 18, 18)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(jLabel34))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(logoBrowse)
                    .add(alternateLogoBrowse))
                .add(18, 18, 18)
                .add(jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(alternateLogoPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                    .add(companyLogoPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                .addContainerGap())
        );

        jLabel56.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel56.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-48/Globe.png"))); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel16Layout = new org.jdesktop.layout.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel16Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel17, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 265, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel16Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel16Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel17, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel56, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout companyInfoPanelLayout = new org.jdesktop.layout.GroupLayout(companyInfoPanel);
        companyInfoPanel.setLayout(companyInfoPanelLayout);
        companyInfoPanelLayout.setHorizontalGroup(
            companyInfoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, companyInfoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        companyInfoPanelLayout.setVerticalGroup(
            companyInfoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(companyInfoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("My Company", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Globe.png")), companyInfoPanel); // NOI18N

        showRemoteMessageCheckbox.setText("Show Remote Message (Grabs a small message from datavirtue.com)");
        showRemoteMessageCheckbox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        showRemoteMessageCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showRemoteMessageCheckboxActionPerformed(evt);
            }
        });

        jPanel19.setBorder(javax.swing.BorderFactory.createTitledBorder(" Email Settings (Send Documents) "));

        jLabel42.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel42.setText("SMTP Server:");

        jLabel57.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel57.setText("My Address:");

        jLabel58.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel58.setText("SMTP User Name:");

        jLabel59.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel59.setText("SMTP Password:");

        emailServerField.setToolTipText("This is the mail server's address. (Port 25)");

        emailAddressField.setToolTipText("Your address known to the mail server. Must correspond to the user name and password.");

        emailUserName.setToolTipText("Your user name for your account on the mail server.");
        emailUserName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                emailUserNameActionPerformed(evt);
            }
        });

        emailPassword.setToolTipText("The password for your email account on the mail server.");

        testEmailButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/E-mail.png"))); // NOI18N
        testEmailButton.setText("Send Test Message");
        testEmailButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testEmailButtonActionPerformed(evt);
            }
        });

        jLabel60.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel60.setText("Port");

        emailPortField.setText("25");

        emailSslCheckbox.setText("SSL");
        emailSslCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                emailSslCheckboxActionPerformed(evt);
            }
        });

        jLabel35.setText("The test message may be rejected by your email service because of an attachment.");

        org.jdesktop.layout.GroupLayout jPanel19Layout = new org.jdesktop.layout.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel59, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel58, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel57, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel42, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(emailAddressField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 674, Short.MAX_VALUE)
                    .add(emailUserName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 674, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel19Layout.createSequentialGroup()
                        .add(emailServerField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel60, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 37, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(emailPortField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 46, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel19Layout.createSequentialGroup()
                        .add(emailPassword, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 141, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(emailSslCheckbox)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(jPanel19Layout.createSequentialGroup()
                        .add(testEmailButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel35, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel42)
                    .add(emailServerField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(emailPortField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel60))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel57)
                    .add(emailAddressField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel58)
                    .add(emailUserName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel59)
                    .add(emailPassword, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(emailSslCheckbox))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(testEmailButton)
                    .add(jLabel35)))
        );

        org.jdesktop.layout.GroupLayout EDIPanelLayout = new org.jdesktop.layout.GroupLayout(EDIPanel);
        EDIPanel.setLayout(EDIPanelLayout);
        EDIPanelLayout.setHorizontalGroup(
            EDIPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(EDIPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(EDIPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel19, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(showRemoteMessageCheckbox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 809, Short.MAX_VALUE))
                .addContainerGap())
        );
        EDIPanelLayout.setVerticalGroup(
            EDIPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(EDIPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(showRemoteMessageCheckbox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(265, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Integrations", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Connect.png")), EDIPanel); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(" Backups "));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Data Folder");

        dataFolderField.setEditable(false);

        backupFolderButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        backupFolderButton.setText("Backup Folder");
        backupFolderButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        backupFolderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupFolderButtonActionPerformed(evt);
            }
        });

        backupFolderField.setToolTipText("This folder will be presented as the primary backup folder.");

        secondaryCheckBox.setText("Secondary Backup");
        secondaryCheckBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        secondaryCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondaryCheckBoxActionPerformed(evt);
            }
        });

        secondaryButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        secondaryButton.setText("Second Folder");
        secondaryButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        secondaryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondaryButtonActionPerformed(evt);
            }
        });

        secondaryBackupFolderField.setToolTipText("Nevitium will always attempt to backup here without prompting.");

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(backupFolderButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(secondaryButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(dataFolderField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 714, Short.MAX_VALUE)
                    .add(secondaryCheckBox)
                    .add(secondaryBackupFolderField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 714, Short.MAX_VALUE)
                    .add(backupFolderField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 714, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(dataFolderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(backupFolderButton)
                    .add(backupFolderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(secondaryCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(secondaryBackupFolderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(secondaryButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout backupPanelLayout = new org.jdesktop.layout.GroupLayout(backupPanel);
        backupPanel.setLayout(backupPanelLayout);
        backupPanelLayout.setHorizontalGroup(
            backupPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        backupPanelLayout.setVerticalGroup(
            backupPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backupPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(336, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Backups", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Transfer.png")), backupPanel); // NOI18N

        jPanel11.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        manageUsersButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Users.png"))); // NOI18N
        manageUsersButton.setText("Manage Users");
        manageUsersButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        manageUsersButton.setMargin(new java.awt.Insets(2, 7, 2, 7));
        manageUsersButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manageUsersButtonActionPerformed(evt);
            }
        });

        logButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Information.png"))); // NOI18N
        logButton.setText("View Log");
        logButton.setEnabled(false);
        logButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        logButton.setMargin(new java.awt.Insets(2, 7, 2, 7));
        logButton.setMaximumSize(new java.awt.Dimension(135, 41));
        logButton.setMinimumSize(new java.awt.Dimension(135, 41));
        logButton.setPreferredSize(new java.awt.Dimension(139, 57));

        org.jdesktop.layout.GroupLayout jPanel11Layout = new org.jdesktop.layout.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, logButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, manageUsersButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE))
                .addContainerGap(626, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .add(manageUsersButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(logButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout securityPanelLayout = new org.jdesktop.layout.GroupLayout(securityPanel);
        securityPanel.setLayout(securityPanelLayout);
        securityPanelLayout.setHorizontalGroup(
            securityPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(securityPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        securityPanelLayout.setVerticalGroup(
            securityPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(securityPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(381, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Security", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Locked folder.png")), securityPanel); // NOI18N

        jPanel9.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        inkSaverCheckbox.setText("Ink Saver");
        inkSaverCheckbox.setToolTipText("Ink Saver prevents color on documents except for logo.");
        inkSaverCheckbox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        processPaymentWhenPostingCheckbox.setText("Process Payment When Posting");
        processPaymentWhenPostingCheckbox.setToolTipText("Auto selects the Take Payment check box on the invoice.");
        processPaymentWhenPostingCheckbox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        jLabel26.setText("Default Scan Field:");

        buttonGroup2.add(defaultScanUpcRadio);
        defaultScanUpcRadio.setSelected(true);
        defaultScanUpcRadio.setText("UPC");
        defaultScanUpcRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        buttonGroup2.add(defaultScanCodeRadio);
        defaultScanCodeRadio.setText("Code");
        defaultScanCodeRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        posModeBox.setText("POS Mode");
        posModeBox.setToolTipText("Assumes the receipt printer is your default printer and changes the invoice screen.");
        posModeBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        posPrinterPaperWidthSpinner.setFont(new java.awt.Font("Courier", 0, 14)); // NOI18N
        posPrinterPaperWidthSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                posPrinterPaperWidthSpinnerStateChanged(evt);
            }
        });

        jLabel1.setText("Paper Roll Width (mm):");

        inchLabel.setText("3.15");

        jLabel49.setText("Inches");

        buttonGroup2.add(defaultScanDescriptionRadio);
        defaultScanDescriptionRadio.setText("Desc");
        defaultScanDescriptionRadio.setEnabled(false);

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(inkSaverCheckbox)
                    .add(processPaymentWhenPostingCheckbox)
                    .add(jPanel9Layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(posPrinterPaperWidthSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(inchLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel49))
                    .add(jPanel9Layout.createSequentialGroup()
                        .add(jLabel26)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(defaultScanUpcRadio)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(defaultScanCodeRadio)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(defaultScanDescriptionRadio))
                    .add(posModeBox))
                .addContainerGap(47, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .add(inkSaverCheckbox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(processPaymentWhenPostingCheckbox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel26)
                    .add(defaultScanUpcRadio)
                    .add(defaultScanCodeRadio)
                    .add(defaultScanDescriptionRadio))
                .add(22, 22, 22)
                .add(posModeBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(posPrinterPaperWidthSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(inchLabel)
                    .add(jLabel49))
                .addContainerGap(56, Short.MAX_VALUE))
        );

        jPanel12.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel40.setText("Currency Symbol: ");

        currencyField.setToolTipText("Select your currency symbol from the Chracter Map (Windows)");

        printZerosCheckBox.setText("Print Zeros");
        printZerosCheckBox.setToolTipText("Enabling this causes zero amounts to be printed for invoice items.");
        printZerosCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printZerosCheckBoxActionPerformed(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(" Document Colors "));

        invoiceColorField.setText("Invoice Color");
        invoiceColorField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                invoiceColorFieldMouseClicked(evt);
            }
        });
        invoiceColorField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoiceColorFieldActionPerformed(evt);
            }
        });

        statementColorField.setText("Statement Color");
        statementColorField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                statementColorFieldMouseClicked(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, statementColorField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, invoiceColorField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(invoiceColorField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 9, Short.MAX_VALUE)
                .add(statementColorField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel12Layout = new org.jdesktop.layout.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel12Layout.createSequentialGroup()
                        .add(jLabel40)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(currencyField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(printZerosCheckBox)))
                .addContainerGap(184, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 74, Short.MAX_VALUE)
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel40)
                    .add(currencyField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(printZerosCheckBox))
                .addContainerGap())
        );

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder(" Customizations "));

        jLabel33.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel33.setText("Tax1 Name");

        tax1NameField.setToolTipText("Limit to three or four characters");

        jLabel51.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel51.setText("Tax2 Name");

        tax2NameField.setToolTipText("Limit to three or four characters");

        jLabel45.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel45.setText("Invoice Name");

        invoicePrefixField.setToolTipText("Invoice numbers are stored with a max 8 characters this is included");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Invoice Prefix");

        jLabel46.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel46.setText("Quote Prefix");

        jLabel55.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel55.setText("Quote Name");

        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel20.setText("Bill To Label");

        billToTextField.setText("BILL TO:");

        org.jdesktop.layout.GroupLayout jPanel13Layout = new org.jdesktop.layout.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel33, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel45, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel51, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .add(jLabel46, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .add(jLabel55, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .add(jLabel20, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(invoicePrefixField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .add(invoiceNameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, tax2NameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .add(tax1NameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .add(quotePrefixField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .add(quoteNameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .add(billToTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel33)
                    .add(tax1NameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tax2NameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel51))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(invoiceNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel45))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(invoicePrefixField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel8))
                .add(8, 8, 8)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(quoteNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel55))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(quotePrefixField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel46))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(billToTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel20))
                .addContainerGap(46, Short.MAX_VALUE))
        );

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder(" Taxes & Interest "));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("Tax 1 Rate");

        tax1Field.setToolTipText("Example: .06 = 6%");

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("Tax 2 Rate");

        tax2Field.setToolTipText("Example: .06 = 6%");

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("(Interest Rate");

        interestRateField.setToolTipText("Simple Interest");

        jLabel25.setText("/ 365) * #days past the grace period * Balance");

        jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel23.setText("Grace Period");

        interestGracePeriodField.setToolTipText("Amount of days before interest is charged on balances.");

        jLabel24.setText("Days");

        showTax1Box.setText("Show Tax 1");

        showTax2Box.setText("Show Tax 2");

        jLabel62.setText("Round cash sales to nearest:");

        cashRoundingCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "N/A", ".05", ".10" }));
        cashRoundingCombo.setToolTipText("Takes affect when applying a cash payment.");

        org.jdesktop.layout.GroupLayout jPanel14Layout = new org.jdesktop.layout.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jLabel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel14Layout.createSequentialGroup()
                        .add(8, 8, 8)
                        .add(jLabel23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 79, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel62, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 486, Short.MAX_VALUE)
                    .add(jPanel14Layout.createSequentialGroup()
                        .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(interestGracePeriodField)
                            .add(interestRateField))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel25)
                            .add(jLabel24)))
                    .add(jPanel14Layout.createSequentialGroup()
                        .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, tax2Field)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, tax1Field))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(showTax2Box)
                            .add(showTax1Box)))
                    .add(cashRoundingCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tax1Field, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6)
                    .add(showTax1Box))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(tax2Field, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(showTax2Box))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel25)
                    .add(interestRateField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel10))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel24)
                    .add(interestGracePeriodField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel23))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel62)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(cashRoundingCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(76, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout invoicePanelLayout = new org.jdesktop.layout.GroupLayout(invoicePanel);
        invoicePanel.setLayout(invoicePanelLayout);
        invoicePanelLayout.setHorizontalGroup(
            invoicePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(invoicePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(invoicePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(invoicePanelLayout.createSequentialGroup()
                        .add(jPanel9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(invoicePanelLayout.createSequentialGroup()
                        .add(jPanel13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel14, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        invoicePanelLayout.setVerticalGroup(
            invoicePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(invoicePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(invoicePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel14, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(invoicePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Invoice", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Order form 3d.png")), invoicePanel); // NOI18N

        layoutPathLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        layoutPathLabel.setText("Layout Path");

        jLabel61.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel61.setText("Invoice Layout");

        invoiceLayoutComboBox.setToolTipText("Selct a custom layout for the invoices.");
        invoiceLayoutComboBox.setEnabled(false);

        layoutPathField.setEditable(false);

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jLabel61, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layoutPathLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(invoiceLayoutComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 197, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(layoutPathField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 701, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(layoutPathLabel)
                    .add(layoutPathField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(invoiceLayoutComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel61))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layoutPanelLayout = new org.jdesktop.layout.GroupLayout(layoutPanel);
        layoutPanel.setLayout(layoutPanelLayout);
        layoutPanelLayout.setHorizontalGroup(
            layoutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layoutPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layoutPanelLayout.setVerticalGroup(
            layoutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layoutPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(414, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Layouts", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Measure.png")), layoutPanel); // NOI18N

        jPanel15.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel21.setText("Default Markup:");

        markupField.setFont(new java.awt.Font("OCR B MT", 0, 14)); // NOI18N
        markupField.setText("2.7");
        markupField.setToolTipText("Standard markup for auto calculating retail price.");

        jLabel22.setText("Points");

        jLabel41.setText("Weight Unit: ");

        buttonGroup7.add(lbsRadio);
        lbsRadio.setText("lbs");
        lbsRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        buttonGroup7.add(kgsRadio);
        kgsRadio.setText("kgs");
        kgsRadio.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        quantityWarningCheckBox.setText("Ignore Quantity Warnings");
        quantityWarningCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quantityWarningCheckBoxActionPerformed(evt);
            }
        });

        catLineCheckBox.setText("Add category line to invoice items");
        catLineCheckBox.setToolTipText("When using Nevitium as an inventory deployment tracker.");

        partialQuantityCheckBox.setText("Allow partial quantity sales by default");
        partialQuantityCheckBox.setToolTipText("When using Nevitium as an inventory deployment tracker.");
        partialQuantityCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                partialQuantityCheckBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel15Layout = new org.jdesktop.layout.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel15Layout.createSequentialGroup()
                        .add(jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jPanel15Layout.createSequentialGroup()
                                .add(jLabel21, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(markupField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jLabel22)
                                .add(26, 26, 26))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel15Layout.createSequentialGroup()
                                .add(jLabel41)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(kgsRadio)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(lbsRadio)))
                        .add(508, 508, 508))
                    .add(jPanel15Layout.createSequentialGroup()
                        .add(jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, catLineCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, quantityWarningCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE))
                        .addContainerGap())
                    .add(partialQuantityCheckBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel21)
                    .add(markupField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel22))
                .add(18, 18, 18)
                .add(jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lbsRadio)
                    .add(kgsRadio)
                    .add(jLabel41))
                .add(18, 18, 18)
                .add(quantityWarningCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(catLineCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(partialQuantityCheckBox)
                .addContainerGap(309, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout inventoryPanelLayout = new org.jdesktop.layout.GroupLayout(inventoryPanel);
        inventoryPanel.setLayout(inventoryPanelLayout);
        inventoryPanelLayout.setHorizontalGroup(
            inventoryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(inventoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel15, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        inventoryPanelLayout.setVerticalGroup(
            inventoryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(inventoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel15, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Inventory", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Book library.png")), inventoryPanel); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(" Output "));

        invoiceButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        invoiceButton.setText("Invoice Folder");
        invoiceButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        invoiceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoiceButtonActionPerformed(evt);
            }
        });

        invoiceOutputFolderField.setToolTipText("this is where the Invoices are sent (.pdf)");
        invoiceOutputFolderField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoiceOutputFolderFieldActionPerformed(evt);
            }
        });

        quoteButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        quoteButton.setText("Quote Folder");
        quoteButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        quoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quoteButtonActionPerformed(evt);
            }
        });

        reportButton.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        reportButton.setText("Report Folder");
        reportButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
        reportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportButtonActionPerformed(evt);
            }
        });

        reportOutputFolderField.setToolTipText("This is where the reports are saved (.pdf)");

        watermarkBrowse.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        watermarkBrowse.setText("Watermark");
        watermarkBrowse.setMargin(new java.awt.Insets(1, 1, 1, 1));
        watermarkBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                watermarkBrowseActionPerformed(evt);
            }
        });

        watermarkImagePathField.setToolTipText("Leave this blank to remove watermark from reports");

        useWatermarkOnReportsCheckbox.setText("Use Watermark in Reports");
        useWatermarkOnReportsCheckbox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        jLabel43.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel43.setText("(JPG, BMP, GIF, PNG)");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(quoteButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(reportButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, invoiceButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, watermarkBrowse, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(useWatermarkOnReportsCheckbox)
                        .add(18, 18, 18)
                        .add(jLabel43))
                    .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, watermarkImagePathField)
                        .add(reportOutputFolderField)
                        .add(quoteOutputFolderField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 564, Short.MAX_VALUE)
                        .add(invoiceOutputFolderField)))
                .addContainerGap(163, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(invoiceButton)
                    .add(invoiceOutputFolderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(quoteButton)
                    .add(quoteOutputFolderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(7, 7, 7)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(reportButton)
                    .add(reportOutputFolderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(watermarkBrowse)
                    .add(watermarkImagePathField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(useWatermarkOnReportsCheckbox)
                    .add(jLabel43))
                .add(16, 16, 16))
        );

        jLabel52.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel52.setText("Pmt Sys:");

        paymentSystemUriField.setToolTipText("Link to an application OR a website to process payments (Ex. PC Charge Pro)");
        paymentSystemUriField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                paymentSystemUriFieldFocusLost(evt);
            }
        });
        paymentSystemUriField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                paymentSystemUriFieldMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                paymentSystemUriFieldMouseExited(evt);
            }
        });
        paymentSystemUriField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                paymentSystemUriFieldKeyTyped(evt);
            }
        });

        paymentSystemIsWebCheckbox.setText("Web Based");
        paymentSystemIsWebCheckbox.setToolTipText("Launches the URL in your web browser");
        paymentSystemIsWebCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paymentSystemIsWebCheckboxActionPerformed(evt);
            }
        });

        paymentSystemBinPathBrowseButton.setText("Browse");
        paymentSystemBinPathBrowseButton.setToolTipText("Browse to a payment application on your system");
        paymentSystemBinPathBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paymentSystemBinPathBrowseButtonActionPerformed(evt);
            }
        });

        jLabel53.setText("Use For:");

        usePaymentSystemForCardsCheckbox.setText("Card Payments");
        usePaymentSystemForCardsCheckbox.setToolTipText("Enable to automatically select the payment system check box in the payment window");
        usePaymentSystemForCardsCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usePaymentSystemForCardsCheckboxActionPerformed(evt);
            }
        });

        usePaymentSystemForChecksCheckbox.setText("Check");
        usePaymentSystemForChecksCheckbox.setToolTipText("Enable to automatically select the payment system check box in the payment window");
        usePaymentSystemForChecksCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usePaymentSystemForChecksCheckboxActionPerformed(evt);
            }
        });

        jLabel47.setText("PDF Reader: ");

        pdfReaderBinField.setText("C:\\Program Files\\Adobe\\");
            pdfReaderBinField.setToolTipText("You need to specify this to View reports.");

            pdfBinPathBrowseButton.setText("Browse");
            pdfBinPathBrowseButton.setToolTipText("Browse and select a PDF reader program.");
            pdfBinPathBrowseButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pdfBinPathBrowseButtonActionPerformed(evt);
                }
            });

            pdfAutoFindButton.setText("Auto Find");
            pdfAutoFindButton.setToolTipText("Works for Windows and some versions of Linux only.");
            pdfAutoFindButton.setMargin(new java.awt.Insets(2, 7, 2, 7));
            pdfAutoFindButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pdfAutoFindButtonActionPerformed(evt);
                }
            });

            pdfRevertButton.setText("Revert");
            pdfRevertButton.setToolTipText("Reverts to previous path after clicking browse or Auto Find.");
            pdfRevertButton.setEnabled(false);
            pdfRevertButton.setMargin(new java.awt.Insets(2, 7, 2, 7));
            pdfRevertButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pdfRevertButtonActionPerformed(evt);
                }
            });

            useSystemDefaultPdfReaderCheckbox.setText("Use System Default");
            useSystemDefaultPdfReaderCheckbox.setToolTipText("Nevitium will use the default application associated with pdf files.");
            useSystemDefaultPdfReaderCheckbox.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    useSystemDefaultPdfReaderCheckboxActionPerformed(evt);
                }
            });

            org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
            jPanel2.setLayout(jPanel2Layout);
            jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jLabel52, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                        .add(jLabel47))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel2Layout.createSequentialGroup()
                            .add(pdfBinPathBrowseButton)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(pdfAutoFindButton)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(pdfRevertButton)
                            .add(18, 18, 18)
                            .add(useSystemDefaultPdfReaderCheckbox))
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, pdfReaderBinField)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel2Layout.createSequentialGroup()
                                .add(paymentSystemBinPathBrowseButton)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel53)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(usePaymentSystemForCardsCheckbox)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(usePaymentSystemForChecksCheckbox)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(paymentSystemIsWebCheckbox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 207, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, paymentSystemUriField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 598, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
            );
            jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                    .addContainerGap(25, Short.MAX_VALUE)
                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel47)
                        .add(pdfReaderBinField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel2Layout.createSequentialGroup()
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(pdfBinPathBrowseButton)
                                .add(pdfAutoFindButton)
                                .add(pdfRevertButton)
                                .add(useSystemDefaultPdfReaderCheckbox))
                            .add(10, 10, 10)
                            .add(paymentSystemUriField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                            .add(51, 51, 51)
                            .add(jLabel52, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(paymentSystemBinPathBrowseButton)
                        .add(jLabel53)
                        .add(usePaymentSystemForCardsCheckbox)
                        .add(usePaymentSystemForChecksCheckbox)
                        .add(paymentSystemIsWebCheckbox))
                    .addContainerGap())
            );

            org.jdesktop.layout.GroupLayout outputPanelLayout = new org.jdesktop.layout.GroupLayout(outputPanel);
            outputPanel.setLayout(outputPanelLayout);
            outputPanelLayout.setHorizontalGroup(
                outputPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(outputPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(outputPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
            );
            outputPanelLayout.setVerticalGroup(
                outputPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(outputPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(18, 18, 18)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(73, 73, 73))
            );

            jTabbedPane1.addTab("Automation", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Export table.png")), outputPanel); // NOI18N

            jPanel7.setBorder(javax.swing.BorderFactory.createEtchedBorder());

            jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "System Information "));

            jLabel27.setText("Operating System:");

            osInfoLabel.setText("os info");

            jLabel29.setText("Java Version:");

            javaInfoLabel.setText("java");

            jLabel32.setText("Working Folder:");

            workingFolderInfoLabel.setText("working folder");

            org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
            jPanel6.setLayout(jPanel6Layout);
            jPanel6Layout.setHorizontalGroup(
                jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel6Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel6Layout.createSequentialGroup()
                            .add(jLabel27)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(osInfoLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .add(jPanel6Layout.createSequentialGroup()
                            .add(jLabel32)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(workingFolderInfoLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .add(jPanel6Layout.createSequentialGroup()
                            .add(jLabel29)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(javaInfoLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addContainerGap())
            );
            jPanel6Layout.setVerticalGroup(
                jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel6Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel27)
                        .add(osInfoLabel))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel29)
                        .add(javaInfoLabel))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel32)
                        .add(workingFolderInfoLabel))
                    .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );

            jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Local Settings"));

            jLabel38.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            jLabel38.setText("OS user");

            jLabel18.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
            jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            jLabel18.setText("Theme");

            themeComboBox.setToolTipText("If you have problems after switching themes you need to restart Nevitium");

            jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
            jLabel4.setText("Connection string");

            currentConnectionStringTextField.setEditable(false);

            jButton1.setText("Advanced");

            operatingSystemUserTextField.setEditable(false);

            org.jdesktop.layout.GroupLayout jPanel8Layout = new org.jdesktop.layout.GroupLayout(jPanel8);
            jPanel8.setLayout(jPanel8Layout);
            jPanel8Layout.setHorizontalGroup(
                jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel8Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jLabel38, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                        .add(jLabel18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                    .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel8Layout.createSequentialGroup()
                            .add(currentConnectionStringTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 556, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jButton1))
                        .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, operatingSystemUserTextField)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, themeComboBox, 0, 179, Short.MAX_VALUE)))
                    .addContainerGap())
            );
            jPanel8Layout.setVerticalGroup(
                jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel8Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel18)
                        .add(themeComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                    .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(operatingSystemUserTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jLabel38))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                    .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel4)
                        .add(currentConnectionStringTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jButton1))
                    .addContainerGap(86, Short.MAX_VALUE))
            );

            jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Axiom Business Terminal"));

            jLabel14.setText("Copyright Data Virtue 2007-2022 - All Rights Reserved.");

            jLabel3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
            jLabel3.setText("Updates:");

            jLabel12.setForeground(new java.awt.Color(0, 102, 255));
            jLabel12.setText("www.datavirtue.com");
            jLabel12.setToolTipText("Click this link to visit datavirtue.com");
            jLabel12.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    jLabel12MouseClicked(evt);
                }
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    jLabel12MouseEntered(evt);
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    jLabel12MouseExited(evt);
                }
            });

            jLabel48.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
            jLabel48.setText("Support: ");

            jLabel13.setForeground(new java.awt.Color(0, 102, 255));
            jLabel13.setText("software@datavirtue.com");
            jLabel13.setToolTipText("Click this link to email Data Virtue");
            jLabel13.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    jLabel13MouseClicked(evt);
                }
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    jLabel13MouseEntered(evt);
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    jLabel13MouseExited(evt);
                }
            });

            checkUpdatesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Update.png"))); // NOI18N
            checkUpdatesButton.setText("Check For Updates");
            checkUpdatesButton.setToolTipText("Tells you if there are new updates for Nevitium.");
            checkUpdatesButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    checkUpdatesButtonActionPerformed(evt);
                }
            });

            org.jdesktop.layout.GroupLayout jPanel10Layout = new org.jdesktop.layout.GroupLayout(jPanel10);
            jPanel10.setLayout(jPanel10Layout);
            jPanel10Layout.setHorizontalGroup(
                jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel10Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jLabel14)
                        .add(jPanel10Layout.createSequentialGroup()
                            .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jLabel12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 167, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jLabel48)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jLabel13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 168, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(checkUpdatesButton))
                    .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );
            jPanel10Layout.setVerticalGroup(
                jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel10Layout.createSequentialGroup()
                    .add(jLabel14)
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel3)
                        .add(jLabel13)
                        .add(jLabel48)
                        .add(jLabel12))
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(checkUpdatesButton)
                    .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );

            org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
            jPanel7.setLayout(jPanel7Layout);
            jPanel7Layout.setHorizontalGroup(
                jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel7Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jPanel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
            );
            jPanel7Layout.setVerticalGroup(
                jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(jPanel7Layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                    .add(jPanel8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(39, Short.MAX_VALUE))
            );

            org.jdesktop.layout.GroupLayout infoPanelLayout = new org.jdesktop.layout.GroupLayout(infoPanel);
            infoPanel.setLayout(infoPanelLayout);
            infoPanelLayout.setHorizontalGroup(
                infoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(infoPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
            );
            infoPanelLayout.setVerticalGroup(
                infoPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(infoPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
            );

            jTabbedPane1.addTab("Info", new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Info.png")), infoPanel); // NOI18N

            saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Floppy.png"))); // NOI18N
            saveButton.setText("Close/Save");
            saveButton.setToolTipText("Click to save and exit.");
            saveButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveButtonActionPerformed(evt);
                }
            });

            jLabel50.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
            jLabel50.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            jLabel50.setText("Questions? Contact: software@datavirtue.com");

            org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
            getContentPane().setLayout(layout);
            layout.setHorizontalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(layout.createSequentialGroup()
                    .addContainerGap()
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jTabbedPane1)
                        .add(layout.createSequentialGroup()
                            .add(jLabel50, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(18, 18, 18)
                            .add(saveButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 138, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
            );
            layout.setVerticalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                    .addContainerGap()
                    .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 496, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(saveButton)
                        .add(jLabel50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap())
            );

            pack();
        }// </editor-fold>//GEN-END:initComponents

    private void checkUpdatesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkUpdatesButtonActionPerformed

        checkForUpdates();

    }//GEN-LAST:event_checkUpdatesButtonActionPerformed
    private void checkForUpdates() {

        String siteData = "";

        siteData = DVNET.HTTPGetFile("http://www.datavirtue.com/axiom/update/nevupdate.html", "Problem retrieving update status.", false);
        if (!siteData.contains("ERR:")) {
            String remoteVersion = siteData;
            String localVersion;
            try {
                localVersion = appSettingsService.getObject().getSoftwareVersion().getVersionString();
            } catch (SQLException ex) {
                ExceptionService.showErrorDialog(this, ex, "Error fetching software version from database");
                return;
            }

            if (!localVersion.equals(remoteVersion)) {
                String nl = System.getProperty("line.separator");
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Your Version: " + localVersion + "     Current Release: " + remoteVersion + nl
                        + "Visit datavirtue.com to download the latest version.");
            } else {

                javax.swing.JOptionPane.showMessageDialog(null, "No updates needed.");
            }
        }
    }
    private void posPrinterPaperWidthSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_posPrinterPaperWidthSpinnerStateChanged

        updateInches();

    }//GEN-LAST:event_posPrinterPaperWidthSpinnerStateChanged

    private void alternateLogoBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alternateLogoBrowseActionPerformed

        JFileChooser fileChooser = DV.getFileChooser(".");

        File curFile = fileChooser.getSelectedFile();
        if (curFile == null) {
            return;
        }
        if (curFile != null) {
            //screenPicField.setText(curFile.toString());
        }


    }//GEN-LAST:event_alternateLogoBrowseActionPerformed

    private void statementColorFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_statementColorFieldMouseClicked

        ColorChooser colorDialog = new ColorChooser(null, true);
        colorDialog.setColor(statementColorField.getBackground());
        colorDialog.setVisible(true);
        statementColorField.setBackground(colorDialog.getColor());
        statementColorField.setText("Statement Color");

        colorDialog.dispose();


}//GEN-LAST:event_statementColorFieldMouseClicked

    private void invoiceColorFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_invoiceColorFieldMouseClicked

        ColorChooser colorDialog = new ColorChooser(null, true);
        colorDialog.setColor(invoiceColorField.getBackground());
        colorDialog.setVisible(true);
        invoiceColorField.setBackground(colorDialog.getColor());
        invoiceColorField.setText("Invoice Color");
        colorDialog.dispose();

}//GEN-LAST:event_invoiceColorFieldMouseClicked

    private void manageUsersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manageUsersButtonActionPerformed
        new com.datavirtue.axiom.ui.SecurityManager(null, true).setVisible(true);

    }//GEN-LAST:event_manageUsersButtonActionPerformed

    private void pdfRevertButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdfRevertButtonActionPerformed

        pdfReaderBinField.setText("");
        pdfRevertButton.setEnabled(false);

    }//GEN-LAST:event_pdfRevertButtonActionPerformed

    private void pdfAutoFindButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdfAutoFindButtonActionPerformed

        pdfReaderBinField.setText("");
        pdfRevertButton.setEnabled(true);

    }//GEN-LAST:event_pdfAutoFindButtonActionPerformed


    private void jLabel12MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel12MouseClicked

        try {
            Desktop.getDesktop().browse(new URI("http://" + jLabel12.getText()));
        } catch (URISyntaxException ex) {
            ExceptionService.showErrorDialog(this, ex, "URL is invalid");
        } catch (IOException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error opening url with local browser");
        }
      
    }//GEN-LAST:event_jLabel12MouseClicked

    private void jLabel13MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel13MouseClicked

        try {
            Desktop.getDesktop().browse(new URI("mailto:" + jLabel13.getText()));
        } catch (URISyntaxException ex) {
            ExceptionService.showErrorDialog(this, ex, "URL is invalid");
        } catch (IOException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error opening url with local browser");
        }
       
    }//GEN-LAST:event_jLabel13MouseClicked

    private void watermarkBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_watermarkBrowseActionPerformed

        JFileChooser fileChooser = DV.getFileChooser(".");

        File curFile = fileChooser.getSelectedFile();

        if (curFile == null) {
            return;
        }
        if (curFile != null) {
            watermarkImagePathField.setText(curFile.toString());
        }


    }//GEN-LAST:event_watermarkBrowseActionPerformed

    private void logoBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoBrowseActionPerformed

//        JFileChooser fileChooser = DV.getFileChooser(".");
//
//        File curFile = fileChooser.getSelectedFile();
//        
        FileDialog fd = new FileDialog(this, "Choose a file", FileDialog.LOAD);

        fd.setDirectory("~/");
        fd.setFile("*.jpg;*.jpeg;*.png");

        fd.setVisible(true);
        //String filename = fd.getFile();
        var selectedFiles = fd.getFiles();

        if (selectedFiles == null) //your user cancelled the choise
        {
            return;
        }

        try {
            var settings = getCurrentSettings();
            if (selectedFiles[0] != null) {
                var logo = imageService.getImageFromDisk(selectedFiles[0]);
                var base64PngLogo = imageService.convertImageToBase64Png(logo);
                settings.getCompany().setCompanyLogo(base64PngLogo);
                imageService.resizeAndSetImageToJLabel(logo, this.companyLogoImage);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }//GEN-LAST:event_logoBrowseActionPerformed

    private void pdfBinPathBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdfBinPathBrowseButtonActionPerformed

        JFileChooser fileChooser = DV.getFileChooser(pdfReaderBinField.getText());

        File curFile = fileChooser.getSelectedFile();
        if (curFile != null) {
            pdfReaderBinField.setText(curFile.toString());
        }

        pdfRevertButton.setEnabled(true);

    }//GEN-LAST:event_pdfBinPathBrowseButtonActionPerformed

    private void reportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reportButtonActionPerformed
        JFileChooser fileChooser = DV.getDirChooser(reportOutputFolderField.getText(), parentWin);

        File curFile = fileChooser.getSelectedFile();
        if (curFile != null)
            reportOutputFolderField.setText(DV.verifyPath(curFile.toString()));
    }//GEN-LAST:event_reportButtonActionPerformed

    private void invoiceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoiceButtonActionPerformed

        JFileChooser fileChooser = DV.getDirChooser(invoiceOutputFolderField.getText(), parentWin);

        File curFile = fileChooser.getSelectedFile();
        if (curFile != null) {
            invoiceOutputFolderField.setText(DV.verifyPath(curFile.toString()));
        }

    }//GEN-LAST:event_invoiceButtonActionPerformed


    private void secondaryCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondaryCheckBoxActionPerformed
        updateSecondary();
    }//GEN-LAST:event_secondaryCheckBoxActionPerformed
    private void updateSecondary() {

        secondaryBackupFolderField.setEnabled(secondaryCheckBox.isSelected());
        secondaryButton.setEnabled(secondaryCheckBox.isSelected());

    }

    private void backupFolderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupFolderButtonActionPerformed

        JFileChooser fileChooser = DV.getDirChooser(backupFolderField.getText(), parentWin);

        File curFile = fileChooser.getSelectedFile();
        if (curFile != null) {
            backupFolderField.setText(DV.verifyPath(curFile.toString()));
        }

    }//GEN-LAST:event_backupFolderButtonActionPerformed

    private void secondaryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondaryButtonActionPerformed

        JFileChooser fileChooser = DV.getDirChooser(secondaryBackupFolderField.getText(), parentWin);
        File curFile = fileChooser.getSelectedFile();
        if (curFile != null) {
            secondaryBackupFolderField.setText(DV.verifyPath(curFile.toString()));
        }


    }//GEN-LAST:event_secondaryButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        saveSettings();
    }//GEN-LAST:event_saveButtonActionPerformed

    private void paymentSystemBinPathBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paymentSystemBinPathBrowseButtonActionPerformed
        JFileChooser fileChooser = DV.getFileChooser(paymentSystemUriField.getText());

        File curFile = fileChooser.getSelectedFile();
        if (curFile != null) {
            paymentSystemUriField.setText(curFile.toString());
        }
        checkForApp();

    }//GEN-LAST:event_paymentSystemBinPathBrowseButtonActionPerformed

    private void paymentSystemUriFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_paymentSystemUriFieldFocusLost
        checkForApp();
    }//GEN-LAST:event_paymentSystemUriFieldFocusLost

    private void paymentSystemUriFieldMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_paymentSystemUriFieldMouseEntered
        checkForApp();
    }//GEN-LAST:event_paymentSystemUriFieldMouseEntered

    private void paymentSystemUriFieldMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_paymentSystemUriFieldMouseExited
        checkForApp();
    }//GEN-LAST:event_paymentSystemUriFieldMouseExited

    private void paymentSystemIsWebCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paymentSystemIsWebCheckboxActionPerformed
        checkForApp();
    }//GEN-LAST:event_paymentSystemIsWebCheckboxActionPerformed

    private void paymentSystemUriFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_paymentSystemUriFieldKeyTyped
        checkForApp();
    }//GEN-LAST:event_paymentSystemUriFieldKeyTyped

    private void companyFontButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_companyFontButtonActionPerformed

        this.getCompanyFontFromUser();

    }//GEN-LAST:event_companyFontButtonActionPerformed

    private void jLabel12MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel12MouseEntered
        changeHandCursor();
    }//GEN-LAST:event_jLabel12MouseEntered

    private void jLabel13MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel13MouseEntered
        changeHandCursor();
    }//GEN-LAST:event_jLabel13MouseEntered

    private void jLabel12MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel12MouseExited
        revertCursor();
    }//GEN-LAST:event_jLabel12MouseExited

    private void jLabel13MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel13MouseExited
        revertCursor();
    }//GEN-LAST:event_jLabel13MouseExited

    private void quoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quoteButtonActionPerformed
        JFileChooser fileChooser = DV.getDirChooser(quoteOutputFolderField.getText(), parentWin);

        File curFile = fileChooser.getSelectedFile();
        if (curFile != null)
            invoiceOutputFolderField.setText(DV.verifyPath(curFile.toString()));
    }//GEN-LAST:event_quoteButtonActionPerformed

    private void showRemoteMessageCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showRemoteMessageCheckboxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_showRemoteMessageCheckboxActionPerformed

    private void countryComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countryComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_countryComboActionPerformed

    private void emailUserNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emailUserNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_emailUserNameActionPerformed

    private void testEmailButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testEmailButtonActionPerformed
        NewEmail email = new NewEmail();
        email.setFrom(emailAddressField.getText());
        email.setRecipent(emailAddressField.getText());
        email.setServer(emailServerField.getText());
        email.setPort(emailPortField.getText());
        email.setUsername(emailUserName.getText());
        email.setSSL(emailSslCheckbox.isSelected());
        email.setPassword(emailPassword.getText());
        email.setSubject("Axiom Test Email");
        email.setText("Axiom Email Test Successful!  Visit datavirtue.com for updates and support.");
        email.setAttachment("ver.inf");
        email.sendEmail();

    }//GEN-LAST:event_testEmailButtonActionPerformed

    private void useSystemDefaultPdfReaderCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useSystemDefaultPdfReaderCheckboxActionPerformed
        if (useSystemDefaultPdfReaderCheckbox.isSelected()) {
            System.out.println(Desktop.getDesktop().toString());
            if (!Desktop.isDesktopSupported()) {
                javax.swing.JOptionPane.showMessageDialog(null, Desktop.getDesktop().toString() + " is not supported.");
                useSystemDefaultPdfReaderCheckbox.setSelected(false);
                return;
            }

            pdfReaderBinField.setEnabled(false);
        } else {
            pdfReaderBinField.setEnabled(true);
        }
    }//GEN-LAST:event_useSystemDefaultPdfReaderCheckboxActionPerformed

    private void usePaymentSystemForChecksCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usePaymentSystemForChecksCheckboxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_usePaymentSystemForChecksCheckboxActionPerformed

    private void emailSslCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emailSslCheckboxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_emailSslCheckboxActionPerformed

    private void quantityWarningCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quantityWarningCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_quantityWarningCheckBoxActionPerformed

    private void printZerosCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printZerosCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_printZerosCheckBoxActionPerformed

private void invoiceOutputFolderFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoiceOutputFolderFieldActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_invoiceOutputFolderFieldActionPerformed

    private void usePaymentSystemForCardsCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usePaymentSystemForCardsCheckboxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_usePaymentSystemForCardsCheckboxActionPerformed

    private void invoiceColorFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoiceColorFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_invoiceColorFieldActionPerformed

    private void partialQuantityCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_partialQuantityCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_partialQuantityCheckBoxActionPerformed

    private void changeHandCursor() {
        saveCursor();
        Cursor c = new Cursor(Cursor.HAND_CURSOR);
        jTabbedPane1.setCursor(c);
    }

    private void revertCursor() {

        if (defaultCursor == null) {
            defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        }
        jTabbedPane1.setCursor(defaultCursor);
    }

    private void saveCursor() {
        defaultCursor = jTabbedPane1.getCursor();
    }

    private void checkForApp() {
        String app = paymentSystemUriField.getText();
        app = app.toLowerCase();
        if (app.endsWith(".exe") || app.endsWith(".app") || !app.contains(".")) {
            paymentSystemIsWebCheckbox.setSelected(false);
        }

    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel EDIPanel;
    private javax.swing.JTextField address1Field;
    private javax.swing.JTextField address2Field;
    private javax.swing.ButtonGroup addressButtonGroup;
    private javax.swing.JButton alternateLogoBrowse;
    private javax.swing.JLabel alternateLogoImage;
    private javax.swing.JPanel alternateLogoPanel;
    private javax.swing.JButton backupFolderButton;
    private javax.swing.JTextField backupFolderField;
    private javax.swing.JPanel backupPanel;
    private javax.swing.JTextField billToTextField;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.ButtonGroup buttonGroup6;
    private javax.swing.ButtonGroup buttonGroup7;
    private javax.swing.ButtonGroup buttonGroup8;
    private javax.swing.JComboBox cashRoundingCombo;
    private javax.swing.JCheckBox catLineCheckBox;
    private javax.swing.JButton checkUpdatesButton;
    private javax.swing.JTextField cityStateZipField;
    private javax.swing.JButton companyFontButton;
    private javax.swing.JPanel companyInfoPanel;
    private javax.swing.JLabel companyLogoImage;
    private javax.swing.JPanel companyLogoPanel;
    private javax.swing.JTextField companyNameField;
    private javax.swing.JComboBox countryCombo;
    private javax.swing.JTextField currencyField;
    private javax.swing.JTextField currentConnectionStringTextField;
    private javax.swing.JTextField dataFolderField;
    private javax.swing.JRadioButton defaultScanCodeRadio;
    private javax.swing.JRadioButton defaultScanDescriptionRadio;
    private javax.swing.JRadioButton defaultScanUpcRadio;
    private javax.swing.JTextField emailAddressField;
    private javax.swing.JPasswordField emailPassword;
    private javax.swing.JTextField emailPortField;
    private javax.swing.JTextField emailServerField;
    private javax.swing.JCheckBox emailSslCheckbox;
    private javax.swing.JTextField emailUserName;
    private javax.swing.JLabel inchLabel;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JCheckBox inkSaverCheckbox;
    private javax.swing.JTextField interestGracePeriodField;
    private javax.swing.JTextField interestRateField;
    private javax.swing.JPanel inventoryPanel;
    private javax.swing.JButton invoiceButton;
    private javax.swing.JTextField invoiceColorField;
    private javax.swing.JComboBox invoiceLayoutComboBox;
    private javax.swing.JTextField invoiceNameField;
    private javax.swing.JTextField invoiceOutputFolderField;
    private javax.swing.JPanel invoicePanel;
    private javax.swing.JTextField invoicePrefixField;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel javaInfoLabel;
    private javax.swing.JRadioButton kgsRadio;
    private javax.swing.JPanel layoutPanel;
    private javax.swing.JTextField layoutPathField;
    private javax.swing.JLabel layoutPathLabel;
    private javax.swing.JRadioButton lbsRadio;
    private javax.swing.JButton logButton;
    private javax.swing.JButton logoBrowse;
    private javax.swing.JButton manageUsersButton;
    private javax.swing.JTextField markupField;
    private javax.swing.JTextField operatingSystemUserTextField;
    private javax.swing.JLabel osInfoLabel;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JCheckBox partialQuantityCheckBox;
    private javax.swing.JButton paymentSystemBinPathBrowseButton;
    private javax.swing.JCheckBox paymentSystemIsWebCheckbox;
    private javax.swing.JTextField paymentSystemUriField;
    private javax.swing.JButton pdfAutoFindButton;
    private javax.swing.JButton pdfBinPathBrowseButton;
    private javax.swing.JTextField pdfReaderBinField;
    private javax.swing.JButton pdfRevertButton;
    private javax.swing.JTextField phoneField;
    private javax.swing.JCheckBox posModeBox;
    private javax.swing.JSpinner posPrinterPaperWidthSpinner;
    private javax.swing.JCheckBox printZerosCheckBox;
    private javax.swing.JCheckBox processPaymentWhenPostingCheckbox;
    private javax.swing.JCheckBox quantityWarningCheckBox;
    private javax.swing.JButton quoteButton;
    private javax.swing.JTextField quoteNameField;
    private javax.swing.JTextField quoteOutputFolderField;
    private javax.swing.JTextField quotePrefixField;
    private javax.swing.JButton reportButton;
    private javax.swing.JTextField reportOutputFolderField;
    private javax.swing.JButton saveButton;
    private javax.swing.JTextField secondaryBackupFolderField;
    private javax.swing.JButton secondaryButton;
    private javax.swing.JCheckBox secondaryCheckBox;
    private javax.swing.JPanel securityPanel;
    private javax.swing.JCheckBox showRemoteMessageCheckbox;
    private javax.swing.JCheckBox showTax1Box;
    private javax.swing.JCheckBox showTax2Box;
    private javax.swing.JCheckBox showTaxIdCheckBox;
    private javax.swing.JTextField statementColorField;
    private javax.swing.JTextField tax1Field;
    private javax.swing.JTextField tax1NameField;
    private javax.swing.JTextField tax2Field;
    private javax.swing.JTextField tax2NameField;
    private javax.swing.JTextField taxIdField;
    private javax.swing.JButton testEmailButton;
    private javax.swing.JComboBox themeComboBox;
    private javax.swing.JCheckBox usePaymentSystemForCardsCheckbox;
    private javax.swing.JCheckBox usePaymentSystemForChecksCheckbox;
    private javax.swing.JCheckBox useSystemDefaultPdfReaderCheckbox;
    private javax.swing.JCheckBox useWatermarkOnReportsCheckbox;
    private javax.swing.JButton watermarkBrowse;
    private javax.swing.JTextField watermarkImagePathField;
    private javax.swing.JLabel workingFolderInfoLabel;
    // End of variables declaration//GEN-END:variables

}
