package com.datavirtue.axiom.services;

import com.formdev.flatlaf.util.StringUtils;
import com.google.gson.Gson;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import com.datavirtue.axiom.models.settings.LocalAppSettings;
import com.datavirtue.axiom.models.settings.WindowSizeAndPosition;
import com.datavirtue.axiom.services.util.DV;
import com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author SeanAnderson
 */
public class LocalSettingsService {

    private static final String APP_NODE = "com/datavirtue/axiom";
    private static final String USER_SETTINGS_KEY = "LocalUserSettings";
    private static final String DEFAULT_VALUE = "";
    public static final String DEFAULT_DATA_PATH = "~/axiom-invoice/axiom_database";
    public static final String DEFAULT_CONNECTION_STRING = "jdbc:h2:" + DEFAULT_DATA_PATH + ";AUTO_SERVER=TRUE";

    public static final String ARC_ORANGE_THEME = "ArcOrange";
    public static final String PURPLE_DARK_THEME = "DarkPurple";
    public static final String HIGH_CONTRAST_THEME = "HighContrast";

    public static final String[] THEME_NAMES = {ARC_ORANGE_THEME, PURPLE_DARK_THEME, HIGH_CONTRAST_THEME};
    public static final String DEFAULT_THEME = THEME_NAMES[0];

    public LocalSettingsService() {

    }

    public static LocalAppSettings getLocalAppSettings() throws BackingStoreException {
        Preferences prefs = Preferences.userRoot().node(APP_NODE);
        var settings = prefs.get(USER_SETTINGS_KEY, DEFAULT_VALUE);
        if (StringUtils.isEmpty(settings)) {
            return null;
        }
        return new Gson().fromJson(settings, LocalAppSettings.class);
    }

    public static LocalAppSettings createDefaultLocalAppSettings() {
        var userConfig = new LocalAppSettings();
        userConfig.setConnectionString(DEFAULT_CONNECTION_STRING);
        userConfig.setDataPath(DEFAULT_DATA_PATH);
        return userConfig;
    }

    public static void saveLocalAppSettings(LocalAppSettings settings) throws BackingStoreException {
        Preferences prefs = Preferences.userRoot().node(APP_NODE);
        prefs.put(USER_SETTINGS_KEY, new Gson().toJson(settings));
        prefs.flush();
    }

    public static void removeLocalAppSettings() throws BackingStoreException {
        Preferences prefs = Preferences.userRoot().node(APP_NODE);
        prefs.remove(USER_SETTINGS_KEY);
        prefs.removeNode();
        prefs.flush();
    }

    public static void setLookAndFeel() throws BackingStoreException {
        // https://www.formdev.com/flatlaf/themes/#intellij_themes_pack
        //      Documentation on how to create custom .json themes
        // https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-intellij-themes
        //      Documentation for LeF themes...just new-up the class you want

        if (LocalSettingsService.getLocalAppSettings() == null) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                //javax.swing.UIManager.setLookAndFeel(new FlatArcOrangeIJTheme());
            } catch (UnsupportedLookAndFeelException ex) {
                
            } catch (ClassNotFoundException ex) {
                
            } catch (InstantiationException ex) {
                
            } catch (IllegalAccessException ex) {
                
            }
            return;
        }

        var theme = LocalSettingsService.getLocalAppSettings().getTheme();

        try {
            if (theme.equals(LocalSettingsService.ARC_ORANGE_THEME)) {
                javax.swing.UIManager.setLookAndFeel(new FlatArcOrangeIJTheme());
            } else if (theme.equals(LocalSettingsService.PURPLE_DARK_THEME)) {
                //javax.swing.UIManager.setLookAndFeel(new FlatGitHubDarkIJTheme());
                javax.swing.UIManager.setLookAndFeel(new FlatDarkPurpleIJTheme());
                //javax.swing.UIManager.setLookAndFeel(new FlatGradiantoDarkFuchsiaIJTheme());
            } else if (theme.equals(LocalSettingsService.HIGH_CONTRAST_THEME)) {
                javax.swing.UIManager.setLookAndFeel(new FlatHighContrastIJTheme());
            }
        } catch (UnsupportedLookAndFeelException e) {

        }
        // may not set a look and feel      

    }

    public static WindowSizeAndPosition getWindowSizeAndPosition(Component window) throws BackingStoreException {
        Point screenLocation = window.getLocationOnScreen();
        Dimension sizeOnScreen = window.getSize();
        var sizeAndPosition = new WindowSizeAndPosition();
        sizeAndPosition.setLocation(screenLocation);
        sizeAndPosition.setSize(sizeOnScreen);
        return sizeAndPosition;
    }

    public static void applyScreenSizeAndPosition(WindowSizeAndPosition sizeAndPosition, Component window) throws BackingStoreException {
        Point location = null; 
        Dimension size = null;
        
        if (sizeAndPosition == null || sizeAndPosition.getLocation() == null) {
            var dimension = DV.computeCenter((java.awt.Window) window);
            location = new Point(dimension.width, dimension.height);            
        }else {
            location = sizeAndPosition.getLocation();
        }
        
        if (sizeAndPosition == null || sizeAndPosition.getSize() == null) {
            size = null; // default size            
        }else {
            size = sizeAndPosition.getSize();
        }
                
        window.setLocation(location);
        if (size != null) {
            window.setSize(sizeAndPosition.getSize());
        }
    }

}
