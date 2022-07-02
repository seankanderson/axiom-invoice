/*
 * Tools.java
 *
 * Created on January 4, 2009, 11:09 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.datavirtue.axiom.ui.util;

import javax.swing.table.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Date;
import com.datavirtue.axiom.models.contacts.Contact;
import com.datavirtue.axiom.models.contacts.ContactAddress;
import com.datavirtue.axiom.services.util.CurrencyUtil;
import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.services.util.PlayWave;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.commons.codec.binary.Hex;

/**
 *
 * @author Data Virtue
 */
public class Tools {

    public static double totalFloatColumn(TableModel tm, int column) {

        if (tm == null) {
            return 0.00f;
        }
        int rowCount = tm.getRowCount();

        if (rowCount < 1) {
            return 0.0f;
        }

        double colTotal = 0.00;

        for (int row = 0; row < rowCount; row++) {

            colTotal += (Double) tm.getValueAt(row, column);

        }

        return CurrencyUtil.round(colTotal);

    }

    public static String buildMD5(String data) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "There was a problem creating the MD5 digest.");
            return "FAILED MD5: " + data;
        }
        messageDigest.reset();
        messageDigest.update(data.getBytes(Charset.forName("UTF8")));
        byte[] resultByte = messageDigest.digest();
        return Hex.encodeHexString(resultByte);
    }

    public static final int YSCR = 10;

    public static void playSound(URL url) {
        if (url == null) {
            return;
        }
        String f = "";
        if (url != null) {
            f = url.getPath();
        }
        
        new PlayWave(f).start();

    }

    public static boolean isDecimal(float f) {
        Float flt = new Float(f);
        int sig = flt.intValue();
        if (f > sig) {
            return true;
        }

        return false;
    }
    
    public static boolean isDecimal(double v) {
        Double d = v;
        int sig = d.intValue();
        if (v > sig) {
            return true;
        }
        return false;
    }

    public static double round(double value) {
        BigDecimal result = new BigDecimal(Double.toString(value)).setScale(2,  RoundingMode.HALF_UP);
        return result.doubleValue();
    }

    public static String colorToString(Color c) {

        String color = Integer.toString(c.getRed()) + ","
                + Integer.toString(c.getGreen()) + ","
                + Integer.toString(c.getBlue());
        return color;
    }

    public static Color stringToColor(String c) {

        int red, green, blue;

        String[] rgb = Tools.fromComma(c);
        /* on parse error return light light grey */
        if (rgb == null) {
            new Color(232, 231, 231);
        }
        red = Integer.valueOf(rgb[0]);
        green = Integer.valueOf(rgb[1]);
        blue = Integer.valueOf(rgb[2]);

        /*
            lt cyan  191, 236, 238
            lt green 209, 254, 207
            lt red   249, 176, 184
            lt yell  248, 253, 142
            lt blue  198, 198, 253
         */
        Color color = new Color(red, green, blue);

        return color;
    }

    public static Dimension parseDimension(String s) {

        String[] dim = Tools.fromComma(s);

        if (dim == null || dim.length < 2 || dim[0] == null) {
            return null;
        }
        return new Dimension(Integer.valueOf(dim[0]), Integer.valueOf(dim[1]));
    }

    public static Point parsePoint(String s) {

        String[] dim = Tools.fromComma(s);

        if (dim == null || dim.length < 2 || dim[0] == null) {
            return null;
        }
        return new Point(Integer.valueOf(dim[0]), Integer.valueOf(dim[1]));
    }

    public static String[] fromComma(String csv) {

        char[] line = csv.toCharArray();
        String[] stringRecord = new String[3];

        StringBuilder temp = new StringBuilder(line.length);

        /* This loop grabs each comma separated value */
        int index = 0;
        try {
            for (int i = 0; i < stringRecord.length; i++) {

                do {

                    if (index >= line.length) {
                        return stringRecord;
                    }

                    if (line[index] == ',') {
                        break;

                    }
                    if (line[index] != '"') {
                        temp.append(line[index]);  //this took a while to straighten out
                        if (index == line.length) {
                            break;
                        }
                    }
                    index++;

                } while (index < line.length && line[index] != ',');

                temp.trimToSize();
                stringRecord[i] = temp.toString();
                //System.out.println("Tools.fromComma: "+stringRecord[i]);
                temp.delete(0, temp.length());
                index++;

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return stringRecord;

    }

    public static String[] formatAddress(Contact contact) {

        String nl = System.getProperty("line.separator");
        String[] address = new String[5];

        String code = contact.getCountryCode();  //country code

        address[0] = contact.getCompanyName() + nl;
        address[1] = contact.getContactName() + nl;
        address[2] = contact.getAddress1() + nl;
        address[3] = contact.getAddress2() + nl;

        if (code.equalsIgnoreCase("US")
                || code.equalsIgnoreCase("CA")
                || code.equalsIgnoreCase("AU")) {
            address[4] = contact.getCity()
                    + "  "
                    + contact.getState()
                    + "  "
                    + contact.getPostalCode()
                    + nl;
        }

        if (code.equalsIgnoreCase("GB")
                || code.equalsIgnoreCase("ZA")
                || code.equalsIgnoreCase("IN")
                || code.equalsIgnoreCase("PH")) {

            address[4] = contact.getCity()
                    + nl
                    + contact.getPostalCode()
                    + nl;

        }
       
        return address;

    }
    
    public static String[] formatAddress(ContactAddress contactAddress) {

        String nl = System.getProperty("line.separator");
        String[] address = new String[5];

        String code = contactAddress.getCountryCode();  //country code

        address[0] = contactAddress.getCompanyName() + nl;
        address[1] = contactAddress.getContactName() + nl;
        address[2] = contactAddress.getAddress1() + nl;
        address[3] = contactAddress.getAddress2() + nl;

        if (code.equalsIgnoreCase("US")
                || code.equalsIgnoreCase("CA")
                || code.equalsIgnoreCase("AU")) {
            address[4] = contactAddress.getCity()
                    + "  "
                    + contactAddress.getState()
                    + "  "
                    + contactAddress.getPostalCode()
                    + nl;
        }

        if (code.equalsIgnoreCase("GB")
                || code.equalsIgnoreCase("ZA")
                || code.equalsIgnoreCase("IN")
                || code.equalsIgnoreCase("PH")) {

            address[4] = contactAddress.getCity()
                    + nl
                    + contactAddress.getPostalCode()
                    + nl;

        }
       
        return address;

    }
    
    public static String arrayToString(String [] array) {
        var builder = new StringBuilder();
        for(String element : array) {
            builder.append(element);
        }
        return builder.toString();
    }
    

    public static void exportTable(TableModel tm, String filename, boolean header) {
        Object value;
        Class objId;
        StringBuilder sb = new StringBuilder();
        int lastCol = tm.getColumnCount() - 1;
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        String nl = System.getProperty("line.separator");
        DV.writeFile(filename, header + nl, false);

        if (header) {
            for (int c = 0; c < tm.getColumnCount(); c++) {

                sb.append("\"" + tm.getColumnName(c) + "\"");
                if (c != lastCol) {
                    sb.append(",");
                }

            }
            DV.writeFile(filename, sb.toString() + nl, false);
        }
        sb = new StringBuilder();

        for (int r = 0; r < tm.getRowCount(); r++) {

            for (int c = 0; c < tm.getColumnCount(); c++) {

                value = tm.getValueAt(r, c);
                objId = DV.idObject(value);

                if (objId.equals(String.class)) {
                    sb.append("\"" + (String) value + "\"");
                }
                if (objId.equals(Float.class)) {
                    sb.append("\"" + Float.toString((Float) value) + "\"");
                }
                if (objId.equals(Integer.class)) {
                    sb.append("\"" + Integer.toString((Integer) value) + "\"");
                }
                if (objId.equals(Boolean.class)) {
                    sb.append("\"" + Boolean.toString((Boolean) value) + "\"");
                }
                if (objId.equals(Date.class)) {
                    sb.append("\"" + df.format(new Date((Long) value)) + "\"");
                }

                if (c != lastCol) {
                    sb.append(",");
                }

            }
            DV.writeFile(filename, sb.toString() + nl, true);
            sb = new StringBuilder();
        }

    }

    public static boolean verifyEmailAddress(String email) {

        if (email.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(null, "There is no email address for this invoice/quote: "
                    + '(' + email + ')');
            return false;
        }

        if (!WebTools.isEmailValid(email)) {
            javax.swing.JOptionPane.showMessageDialog(null, "The contact does not have a valid email address: "
                    + '(' + email + ')');
            return false;
        }
        return true;

    }

    public static boolean verifyElevatedPermissions(String f) {

        File file = new File(f);
        String nl = System.getProperty("line.separator");

        /* Test is skipped if the file doesn't exsist */
        if (file.exists() && !file.canWrite()) {
            javax.swing.JOptionPane.showMessageDialog(null, "It appears you do not have permission to write certain data files owned by Axiom." + nl
                    + "Axiom needs elevated permissions to work properly." + nl
                    + "Shut down Axiom & Contact technical support.");
            return false;
        }

        return true;
    }

    public static String getBoolString(boolean val) {
        if (val) {
            return "true";
        }
        return "false";
    }

    public static boolean getStringBool(String val) {

        if (val.equalsIgnoreCase("true")) {
            return true;
        }
        return false;

    }

    public static int getStringInt(String val, int def) {

        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }

    }

    public static boolean isFolderEmpty(String folder) {

        File f = new File(folder);

        if (!f.exists()) {
            return false;
        }

        java.io.File[] files = f.listFiles();

        /* TODO: check for permissions here as well */
        if (files.length < 1 && f.isDirectory()) {
            return true;
        }
        return false;

    }

}
