/*
 * ControlCenter.java
 *
 * Created on June 22, 2006, 9:47 AM
 ** Copyright (c) Data Virtue 2006 - 2022
 */
package com.datavirtue.axiom.ui;

import com.datavirtue.axiom.ui.inventory.InventoryApp;
import com.datavirtue.axiom.ui.contacts.ContactsApp;
import com.datavirtue.axiom.ui.invoices.InvoiceApp;
import com.datavirtue.axiom.ui.invoices.InvoiceManager;
import businessmanager.ReturnMessageThread;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Image;
import java.io.IOException;
import com.datavirtue.axiom.models.settings.AppSettings;
import com.datavirtue.axiom.models.settings.LocalAppSettings;
import com.datavirtue.axiom.services.AppSettingsService;
import com.datavirtue.axiom.services.DatabaseService;
import com.datavirtue.axiom.services.ExceptionService;
import com.datavirtue.axiom.services.LocalSettingsService;
import com.datavirtue.axiom.services.UserService;
import com.datavirtue.axiom.services.util.DV;
import com.google.inject.Inject;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

/**
 *
 * @author Sean K Anderson - Data Virtue
 * @rights Copyright Data Virtue 2006, 2007, 2022 All Rights Reserved.
 */
public class ControlCenter extends javax.swing.JFrame implements AxiomApp {

    private Image winIcon;
    private String nl = System.getProperty("line.separator");
    private Toolkit tools = Toolkit.getDefaultToolkit();
    @Inject
    private final AppSettingsService appSettingsService = null;
    private AppSettings appSettings;
    private LocalAppSettings localSettings;
    @Inject
    private UserService userService;

    /**
     * Creates new form ControlCenter
     */
    public ControlCenter() {

        //this.getLookAndFeel();
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    recordWindowSizeAndPosition();
                } catch (BackingStoreException ex) {
                    ExceptionService.showErrorDialog(e.getComponent(), ex, "Error saving local screen preferences");
                }
                closeAll();
            }
        });

        System.setProperty("http.agent", "Axiom Business Terminal");
        winIcon = tools.getImage(getClass().getResource("/Orange.png"));
        initComponents();

    }

    private void recordWindowSizeAndPosition() throws BackingStoreException {
        var screenSettings = localSettings.getScreenSettings();
        var sizeAndPosition = LocalSettingsService.getWindowSizeAndPosition(this);
        screenSettings.setMain(sizeAndPosition);
        LocalSettingsService.saveLocalAppSettings(localSettings);
    }

    private void restoreSavedWindowSizeAndPosition() throws BackingStoreException {
        var screenSettings = localSettings.getScreenSettings().getMain();
        LocalSettingsService.applyScreenSizeAndPosition(screenSettings, this);
    }

    public void displayApp() throws BackingStoreException, SQLException {

        appSettingsService.setObjectType(AppSettings.class);
        localSettings = LocalSettingsService.getLocalAppSettings();

        mainToolbar.setLayout(new FlowLayout());
        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
        this.setLocation(dim.width, dim.height);

        try {
            appSettings = this.appSettingsService.getObject();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching settings from database");
        }
        updateMessage();
        setBG();
        restoreSavedWindowSizeAndPosition();
        this.setVisible(true);
        if (this.userService.isSecurityEnabled()) {
            this.authenticateUser();
        }

    }

    private void setBG() {

        try {
            String screenPic = appSettingsService.getObject().getCompany().getMainScreenLogo();
            picLabel.setIcon(new javax.swing.ImageIcon(screenPic));
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching settings");
        }

    }

    private void updateMessage() {

        if (!appSettings.getInternet().isShowRemoteMessage()) {
            internetStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Disconnect.png")));
        } else {
            internetStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Connect.png")));
        }
        if (appSettings.getInternet().isShowRemoteMessage()) {
            /* Thread Example */
            ReturnMessageThread rm = new ReturnMessageThread("http://datavirtue.com/axiom/update/nevstat.txt",
                    remoteMessageBox, internetStatus);
            rm.start();

        } else {

            remoteMessageBox.setText(" Please visit datavirtue.com for updates & support.");

        }
    }

    private void authenticateUser() {

        var accessDialog = new AccessDialog(this, true);

        accessDialog.displayApp();
        String authResult = accessDialog.getAuthResult();
        try {
            if ((this.userService.isSecurityEnabled() && this.userService.getCurrentUser() == null)
                    || authResult.equals(UserService.NOT_AUTHORIZED) || authResult.equals(UserService.PASSWORD_FAILED)) {
                System.exit(0);
            }
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing security settings in the database.");
        }

    }

    private void launchContactsApp() {
        var contactsApp = new ContactsApp(this, true, false, true, true);
        try {
            contactsApp.displayApp();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error access contacts database");
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing local settings");
        }

    }

    private void launchInventoryApp() {
        var inventoryApp = new InventoryApp(null, true, false);
        try {
            inventoryApp.displayApp();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error starting the inventory app");
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error fetching local settings from the OS");
        }
    }

    private void launchInvoiceManagerApp() {
        var invoiceManager = new InvoiceManager(this, true);

        try {
            invoiceManager.displayApp();
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing local settings");
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing database");
        }
    }

    private void launchQuickInvoiceApp() {
        var invoiceDialog = new InvoiceApp(this, true);
        invoiceDialog.displayApp();
        invoiceDialog.dispose();
    }

    private void launchLocalAppSettingsApp() {
        var localSettingsApp = new LocalSettingsDialog(this, true);
        try {
            localSettingsApp.displayApp();
            if (localSettingsApp.isSettingsSaved()) {
                try {
                    DatabaseService.createEachTableIfNotExist();
                    //TODO: need to restart ControlCenter to take effect
                    LocalSettingsService.setLookAndFeel();
                    // check for app settings in database
                    var settings = appSettingsService.getObject();

                    if (settings == null) {
                        var settingsDialog = new SettingsDialog(this, true, 0);
                        settingsDialog.displayApp();
                    }
                } catch (SQLException ex) {
                    ExceptionService.showErrorDialog(this, ex, "Failed to create ");
                }
            }
        } catch (BackingStoreException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error getting local settings");
        }

    }

    private void launchInfoSettings() {

        var settingsDialog = new SettingsDialog(this, true, 8);
        settingsDialog.displayApp();
        updateMessage();
        setBG();

    }

    private void launchUserManager() {
        var userManager = new com.datavirtue.axiom.ui.SecurityManager(this, true);
        try {
            userManager.displayApp();
        } catch (SQLException ex) {
            ExceptionService.showErrorDialog(this, ex, "Error accessing database");
        }
    }

    private void launchPaymentSystem() {

        boolean usePaymentSystem = false;

        String paymentURL = appSettings.getOutput().getPaymentSystemUri();

        if (paymentURL.length() > 0) {
            usePaymentSystem = true;
        } else {
            usePaymentSystem = false;
        }

        if (usePaymentSystem == false) {

            javax.swing.JOptionPane.showMessageDialog(null,
                    "It appears that you have not configured an external payment system." + nl
                    + "Go to File-->Settings-->Output to configure a payment system.");

            return;
        }

        if (usePaymentSystem) {

            boolean webPayment = appSettings.getOutput().isPaymentSystemUriIsWeblink();
            if (webPayment) {
                String url = paymentURL;
                if (!url.contains("http://") && !url.contains("HTTP://")
                        && !url.contains("https://") && !url.contains("HTTPS://")) {

                    javax.swing.JOptionPane.showMessageDialog(null,
                            "You must spcifiy a protocol in the web address" + nl
                            + "Example: http://www.stripe.com instead of just: www.stripe.com");
                    return;
                }

                try {
                    Desktop.getDesktop().browse(new URI(paymentURL));
                } catch (URISyntaxException ex) {
                    ExceptionService.showErrorDialog(this, ex, "URL is invalid");
                } catch (IOException ex) {
                    ExceptionService.showErrorDialog(this, ex, "Error opening url with local browser");
                }

            } else {

                String nl = System.getProperty("line.separator");

                String osName = System.getProperty("os.name");

                try {

                    if (osName.contains("Windows")) {
                        Runtime.getRuntime().exec('"' + paymentURL + '"');
                    } //FOR WINDOWS NT/XP/2000 USE CMD.EXE
                    else {

                        //System.out.println(acro + " " + file);
                        Runtime.getRuntime().exec(paymentURL);

                    }
                } catch (IOException ex) {

                    javax.swing.JOptionPane.showMessageDialog(null,
                            "error: There was a problem launching the payment system!" + nl
                            + "<<" + paymentURL + ">>");
                    //ex.printStackTrace();
                }
            }
        }
    }

    private void launchSettingsApp() {
        var settingsDialog = new SettingsDialog(this, true, 0);
        settingsDialog.displayApp();
        updateMessage();
        setBG();
    }

    private void closeAll() {
        DatabaseService.closeDatabaseConnections();
        System.exit(0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        picLabel = new javax.swing.JLabel();
        remoteMessageBox = new javax.swing.JTextField();
        mainToolbar = new javax.swing.JToolBar();
        connectionsButton = new javax.swing.JButton();
        inventoryButton = new javax.swing.JButton();
        activityButton = new javax.swing.JButton();
        invoiceButton = new javax.swing.JButton();
        settingsButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        statusMessagePanel = new javax.swing.JPanel();
        internetStatus = new javax.swing.JLabel();
        jMenuBar2 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newCompanyMenuItem = new javax.swing.JMenuItem();
        openCompanyMenuItem = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        loginLogoutMenuItem = new javax.swing.JMenuItem();
        userManagerMenuItem = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        shutDownMenuItem = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        contactsMenuItem = new javax.swing.JMenuItem();
        inventoryMenuItem = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        invoicesAndQuotesMenuItem = new javax.swing.JMenuItem();
        newInvoiceMenuItem = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        printChecksMenuItem = new javax.swing.JMenuItem();
        takePaymentMenuItem = new javax.swing.JMenuItem();
        prepaidAccountManagerMenuItem = new javax.swing.JMenuItem();
        reportsMenu = new javax.swing.JMenu();
        unpaidInvoiceMenuItem = new javax.swing.JMenuItem();
        salesAndCogsMenuItem = new javax.swing.JMenuItem();
        inventoryStatusMenuItem = new javax.swing.JMenuItem();
        inventoryReorderMenuItem = new javax.swing.JMenuItem();
        supportMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        aboutAxiomMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Axiom Business Terminal");
        setIconImage(winIcon);

        picLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jScrollPane1.setViewportView(picLabel);

        remoteMessageBox.setEditable(false);

        mainToolbar.setRollover(true);
        mainToolbar.setBorderPainted(false);

        connectionsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/connected_people_64px.png"))); // NOI18N
        connectionsButton.setText("Connections");
        connectionsButton.setFocusable(false);
        connectionsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        connectionsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        connectionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectionsButtonActionPerformed(evt);
            }
        });
        mainToolbar.add(connectionsButton);

        inventoryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/barcode_reader_64px.png"))); // NOI18N
        inventoryButton.setText("Inventory");
        inventoryButton.setFocusable(false);
        inventoryButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        inventoryButton.setMinimumSize(new java.awt.Dimension(91, 81));
        inventoryButton.setPreferredSize(new java.awt.Dimension(91, 81));
        inventoryButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        inventoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inventoryButtonActionPerformed(evt);
            }
        });
        mainToolbar.add(inventoryButton);

        activityButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/ledger_64px.png"))); // NOI18N
        activityButton.setText("Invoices");
        activityButton.setFocusable(false);
        activityButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        activityButton.setPreferredSize(new java.awt.Dimension(91, 81));
        activityButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        activityButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                activityButtonActionPerformed(evt);
            }
        });
        mainToolbar.add(activityButton);

        invoiceButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/cash_register_64px.png"))); // NOI18N
        invoiceButton.setText("SALE");
        invoiceButton.setFocusable(false);
        invoiceButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        invoiceButton.setMinimumSize(new java.awt.Dimension(91, 81));
        invoiceButton.setPreferredSize(new java.awt.Dimension(91, 81));
        invoiceButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        invoiceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoiceButtonActionPerformed(evt);
            }
        });
        mainToolbar.add(invoiceButton);

        settingsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/administrative_tools_64px.png"))); // NOI18N
        settingsButton.setText("Setup");
        settingsButton.setFocusable(false);
        settingsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        settingsButton.setPreferredSize(new java.awt.Dimension(91, 81));
        settingsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        settingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsButtonActionPerformed(evt);
            }
        });
        mainToolbar.add(settingsButton);

        exitButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/outline-green/close_window_64px.png"))); // NOI18N
        exitButton.setText("Exit");
        exitButton.setFocusable(false);
        exitButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exitButton.setPreferredSize(new java.awt.Dimension(91, 81));
        exitButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });
        mainToolbar.add(exitButton);

        statusMessagePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        org.jdesktop.layout.GroupLayout statusMessagePanelLayout = new org.jdesktop.layout.GroupLayout(statusMessagePanel);
        statusMessagePanel.setLayout(statusMessagePanelLayout);
        statusMessagePanelLayout.setHorizontalGroup(
            statusMessagePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 627, Short.MAX_VALUE)
        );
        statusMessagePanelLayout.setVerticalGroup(
            statusMessagePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 124, Short.MAX_VALUE)
        );

        internetStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        internetStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Connect.png"))); // NOI18N
        internetStatus.setToolTipText("Nevitium Internet Status");

        fileMenu.setText("Data");

        newCompanyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Database.png"))); // NOI18N
        newCompanyMenuItem.setText("New company database");
        newCompanyMenuItem.setActionCommand("jMenuItem1");
        newCompanyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newCompanyMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newCompanyMenuItem);

        openCompanyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Open file.png"))); // NOI18N
        openCompanyMenuItem.setText("Open company database");
        openCompanyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openCompanyMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openCompanyMenuItem);
        fileMenu.add(jSeparator13);

        loginLogoutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        loginLogoutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/User login.png"))); // NOI18N
        loginLogoutMenuItem.setText("Login / Logout user");
        loginLogoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginLogoutMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(loginLogoutMenuItem);

        userManagerMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Users.png"))); // NOI18N
        userManagerMenuItem.setText("User access manager");
        userManagerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userManagerMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(userManagerMenuItem);
        fileMenu.add(jSeparator14);

        shutDownMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        shutDownMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Close.png"))); // NOI18N
        shutDownMenuItem.setText("Shut down Axiom");
        shutDownMenuItem.setToolTipText("");
        shutDownMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shutDownMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(shutDownMenuItem);

        jMenuBar2.add(fileMenu);

        toolsMenu.setText("Tools");

        contactsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        contactsMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Conference.png"))); // NOI18N
        contactsMenuItem.setText("Contacts");
        contactsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contactsMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(contactsMenuItem);

        inventoryMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        inventoryMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Barcode scanner1.png"))); // NOI18N
        inventoryMenuItem.setText("Inventory");
        inventoryMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inventoryMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(inventoryMenuItem);
        toolsMenu.add(jSeparator11);

        invoicesAndQuotesMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        invoicesAndQuotesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Money.png"))); // NOI18N
        invoicesAndQuotesMenuItem.setText("Invoices and Quotes");
        invoicesAndQuotesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invoicesAndQuotesMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(invoicesAndQuotesMenuItem);

        newInvoiceMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        newInvoiceMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Create.png"))); // NOI18N
        newInvoiceMenuItem.setText("New Invoice");
        newInvoiceMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newInvoiceMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(newInvoiceMenuItem);
        toolsMenu.add(jSeparator12);

        printChecksMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Notebook.png"))); // NOI18N
        printChecksMenuItem.setText("Print Checks");
        printChecksMenuItem.setToolTipText("");
        printChecksMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printChecksMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(printChecksMenuItem);

        takePaymentMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        takePaymentMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Card terminal.png"))); // NOI18N
        takePaymentMenuItem.setText("Take Payment");
        takePaymentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                takePaymentMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(takePaymentMenuItem);

        prepaidAccountManagerMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        prepaidAccountManagerMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Credit cards.png"))); // NOI18N
        prepaidAccountManagerMenuItem.setText("Prepaid account manager");
        prepaidAccountManagerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prepaidAccountManagerMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(prepaidAccountManagerMenuItem);

        jMenuBar2.add(toolsMenu);

        reportsMenu.setText("Reports");

        unpaidInvoiceMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Report.png"))); // NOI18N
        unpaidInvoiceMenuItem.setText("Unpaid invoice report");
        reportsMenu.add(unpaidInvoiceMenuItem);

        salesAndCogsMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Report.png"))); // NOI18N
        salesAndCogsMenuItem.setText("Sales and costs");
        salesAndCogsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                salesAndCogsMenuItemActionPerformed(evt);
            }
        });
        reportsMenu.add(salesAndCogsMenuItem);

        inventoryStatusMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Reports 3d.png"))); // NOI18N
        inventoryStatusMenuItem.setText("Inventory status");
        inventoryStatusMenuItem.setToolTipText("");
        reportsMenu.add(inventoryStatusMenuItem);

        inventoryReorderMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Reports 3d.png"))); // NOI18N
        inventoryReorderMenuItem.setText("Inventory re-order");
        reportsMenu.add(inventoryReorderMenuItem);

        jMenuBar2.add(reportsMenu);

        supportMenu.setText("Support");

        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Video file.png"))); // NOI18N
        jMenuItem1.setText("Training videos");
        supportMenu.add(jMenuItem1);

        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Knowledge.png"))); // NOI18N
        jMenuItem2.setText("User manual (PDF)");
        supportMenu.add(jMenuItem2);

        jMenuItem3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Home.png"))); // NOI18N
        jMenuItem3.setText("Website");
        supportMenu.add(jMenuItem3);

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/E-mail.png"))); // NOI18N
        jMenuItem4.setText("Email (copy to clipboard)");
        jMenuItem4.setToolTipText("Copies support email address to clipboard");
        supportMenu.add(jMenuItem4);

        aboutAxiomMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Information.png"))); // NOI18N
        aboutAxiomMenuItem.setText("About Axiom");
        aboutAxiomMenuItem.setToolTipText("");
        aboutAxiomMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutAxiomMenuItemActionPerformed(evt);
            }
        });
        supportMenu.add(aboutAxiomMenuItem);

        jMenuBar2.add(supportMenu);

        setJMenuBar(jMenuBar2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
                    .add(mainToolbar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
                    .add(statusMessagePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(remoteMessageBox, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 603, Short.MAX_VALUE)
                        .add(1, 1, 1)
                        .add(internetStatus, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(mainToolbar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusMessagePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(remoteMessageBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(internetStatus, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void settingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsButtonActionPerformed
        launchSettingsApp();
    }//GEN-LAST:event_settingsButtonActionPerformed

    private void connectionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectionsButtonActionPerformed
        launchContactsApp();
    }//GEN-LAST:event_connectionsButtonActionPerformed

    private void invoiceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoiceButtonActionPerformed
        launchQuickInvoiceApp();

    }//GEN-LAST:event_invoiceButtonActionPerformed

    private void activityButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_activityButtonActionPerformed
        launchInvoiceManagerApp();
    }//GEN-LAST:event_activityButtonActionPerformed

    private void inventoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inventoryButtonActionPerformed
        launchInventoryApp();

    }//GEN-LAST:event_inventoryButtonActionPerformed

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        closeAll();
    }//GEN-LAST:event_exitButtonActionPerformed

    private void contactsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contactsMenuItemActionPerformed
        launchContactsApp();
    }//GEN-LAST:event_contactsMenuItemActionPerformed

    private void printChecksMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printChecksMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_printChecksMenuItemActionPerformed

    private void loginLogoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginLogoutMenuItemActionPerformed
        authenticateUser();
    }//GEN-LAST:event_loginLogoutMenuItemActionPerformed

    private void salesAndCogsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_salesAndCogsMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_salesAndCogsMenuItemActionPerformed

    private void newCompanyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newCompanyMenuItemActionPerformed
        launchLocalAppSettingsApp();
    }//GEN-LAST:event_newCompanyMenuItemActionPerformed

    private void takePaymentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_takePaymentMenuItemActionPerformed
        launchPaymentSystem();
    }//GEN-LAST:event_takePaymentMenuItemActionPerformed

    private void aboutAxiomMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutAxiomMenuItemActionPerformed
        launchInfoSettings();
    }//GEN-LAST:event_aboutAxiomMenuItemActionPerformed

    private void shutDownMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shutDownMenuItemActionPerformed
        closeAll();
    }//GEN-LAST:event_shutDownMenuItemActionPerformed

    private void inventoryMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inventoryMenuItemActionPerformed
        launchInventoryApp();
    }//GEN-LAST:event_inventoryMenuItemActionPerformed

    private void newInvoiceMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newInvoiceMenuItemActionPerformed
        launchQuickInvoiceApp();
    }//GEN-LAST:event_newInvoiceMenuItemActionPerformed

    private void invoicesAndQuotesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invoicesAndQuotesMenuItemActionPerformed
        launchInvoiceManagerApp();
    }//GEN-LAST:event_invoicesAndQuotesMenuItemActionPerformed

    private void prepaidAccountManagerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prepaidAccountManagerMenuItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_prepaidAccountManagerMenuItemActionPerformed

    private void userManagerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userManagerMenuItemActionPerformed
        launchUserManager();
    }//GEN-LAST:event_userManagerMenuItemActionPerformed

    private void openCompanyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openCompanyMenuItemActionPerformed
        launchLocalAppSettingsApp();
    }//GEN-LAST:event_openCompanyMenuItemActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutAxiomMenuItem;
    private javax.swing.JButton activityButton;
    private javax.swing.JButton connectionsButton;
    private javax.swing.JMenuItem contactsMenuItem;
    private javax.swing.JButton exitButton;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel internetStatus;
    private javax.swing.JButton inventoryButton;
    private javax.swing.JMenuItem inventoryMenuItem;
    private javax.swing.JMenuItem inventoryReorderMenuItem;
    private javax.swing.JMenuItem inventoryStatusMenuItem;
    private javax.swing.JButton invoiceButton;
    private javax.swing.JMenuItem invoicesAndQuotesMenuItem;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator14;
    private javax.swing.JMenuItem loginLogoutMenuItem;
    private javax.swing.JToolBar mainToolbar;
    private javax.swing.JMenuItem newCompanyMenuItem;
    private javax.swing.JMenuItem newInvoiceMenuItem;
    private javax.swing.JMenuItem openCompanyMenuItem;
    private javax.swing.JLabel picLabel;
    private javax.swing.JMenuItem prepaidAccountManagerMenuItem;
    private javax.swing.JMenuItem printChecksMenuItem;
    private javax.swing.JTextField remoteMessageBox;
    private javax.swing.JMenu reportsMenu;
    private javax.swing.JMenuItem salesAndCogsMenuItem;
    private javax.swing.JButton settingsButton;
    private javax.swing.JMenuItem shutDownMenuItem;
    private javax.swing.JPanel statusMessagePanel;
    private javax.swing.JMenu supportMenu;
    private javax.swing.JMenuItem takePaymentMenuItem;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JMenuItem unpaidInvoiceMenuItem;
    private javax.swing.JMenuItem userManagerMenuItem;
    // End of variables declaration//GEN-END:variables

}
