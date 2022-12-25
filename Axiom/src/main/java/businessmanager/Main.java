/*
 * Main.java
 *
 * Created on June 22, 2006, 9:44 AM
 *
 * This application will contain various modules (JDialogs) that operate
 *on the various databases allowing modification and providing consolidated
 *veiws of the data to help manage a service or retail oriented business
 ** Copyright (c) Data Virtue 2006, 2022
 *
 */
package businessmanager;

import com.datavirtue.axiom.models.settings.AppSettings;
import com.datavirtue.axiom.services.AppSettingsService;
import com.datavirtue.axiom.services.DatabaseService;
import com.datavirtue.axiom.ui.ControlCenter;
import com.datavirtue.axiom.services.DiService;
import java.sql.SQLException;
import javax.swing.JFrame;
import com.datavirtue.axiom.services.ExceptionService;
import com.datavirtue.axiom.services.LocalSettingsService;
import com.datavirtue.axiom.services.TestDataService;
import com.datavirtue.axiom.ui.settings.LocalSettingsDialog;
import com.datavirtue.axiom.ui.settings.SettingsDialog;

/**
 *
 * @author Sean K Anderson - Data Virtue
 * @rights Copyright Data Virtue 2006, 2007, 2022 All Rights Reserved.
 */
public class Main {

    /**
     * Creates a new instance of Main
     */
    public Main() {
    }

    public static void main(String args[]) throws Exception {

        System.setProperty("apple.laf.useScreenMenuBar", "true");

        System.setProperty("apple.awt.application.name", "Axiom");

        LocalSettingsService.setLookAndFeel();
        var frame = new JFrame();
        //frame.setIconImage(Toolkit.getDefaultToolkit().getImage(ControlCenter.class.getResource("src/main/java/Orange.png")));
        frame.setVisible(true);
        if (LocalSettingsService.getLocalAppSettings() == null) {
            var localSettingsApp = new LocalSettingsDialog(frame, true);
            localSettingsApp.displayApp();
            frame.dispose();
        }

        if (LocalSettingsService.getLocalAppSettings() == null) {
            System.exit(-1);
        }
        
        // Refresh LnF
        LocalSettingsService.setLookAndFeel();

        var injector = DiService.getInjector();

        DatabaseService.dropAllTables();
        DatabaseService.createEachTableIfNotExist();
        DatabaseService.clearAllTables();
        
        var appSettingsService = injector.getInstance(AppSettingsService.class);
        appSettingsService.setObjectType(AppSettings.class);
        
        // check for app settings in database
        var settings = appSettingsService.getObject();

        if (settings == null) {
            var settingsDialog = new SettingsDialog(frame, true, 0);
            settingsDialog.displayApp();
        }

        try {
            TestDataService.populateTestData();
        } catch (SQLException e) {
            e.printStackTrace();
            ExceptionService.showErrorDialog(frame, e, "Error accessing database");
            System.exit(-2);
        }

        

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                ControlCenter control = injector.getInstance(ControlCenter.class);
                try {
                    control.displayApp();
                } catch (java.lang.UnsupportedClassVersionError e) {
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "Axiom encountered: Unsupported Class Version error. Try updating Java.");
                } catch (Exception e) {//consume linux exceptions
                    e.printStackTrace();
                }
            }
        });
    }

}
