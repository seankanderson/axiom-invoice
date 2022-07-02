/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * CreditsDialog.java
 *
 * Created on Aug 18, 2009, 1:08:28 AM
 */

package com.datavirtue.axiom.ui;
import java.io.*;
import javax.swing.*;
import java.awt.*;
/**
 *
 * @author Data Virtue
 */
public class CreditsDialog extends javax.swing.JDialog {

    /** Creates new form CreditsDialog */
    public CreditsDialog(java.awt.Frame parent, boolean modal, String file) {
        super(parent, modal);
        initComponents();
        getCredits(file);
        this.setVisible(true);
    }

    private void getCredits(String filename){

        String line ="";
        String nl = System.getProperty("line.separator");
        try {

            boolean exists = (new File(filename)).exists();

                if (exists) {

                    File data = new File (filename);
                    BufferedReader in = new BufferedReader(
                                    new FileReader(data));
                 while (line != null) {

                  line = in.readLine();
                  if (line == null) break;
                  textPane.setText(textPane.getText() + line + nl);

                }

                in.close();
                textPane.setCaretPosition(0);
            }else textPane.setText("credits.txt was not found, visit: http://www.datavirtue.com/axiom.html");


        } catch (Exception e) {
            textPane.setText("There was an error accessing credits.txt, visit: http://www.datavirtue.com/axiom.html");
        }


    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        textPane = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Axiom Art and Technology Credits");
        setAlwaysOnTop(true);

        textPane.setEditable(false);
        textPane.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        textPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                textPaneMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                textPaneMouseExited(evt);
            }
        });
        jScrollPane1.setViewportView(textPane);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void textPaneMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textPaneMouseEntered
        changeTextCursor();
    }//GEN-LAST:event_textPaneMouseEntered

    private void textPaneMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_textPaneMouseExited
        revertCursor();
    }//GEN-LAST:event_textPaneMouseExited

    private void changeTextCursor() {
        saveCursor();
        Cursor c = new Cursor ( Cursor.TEXT_CURSOR );
        textPane.setCursor(c);
    }

    private void revertCursor(){

        if (defaultCursor == null){
            defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        }
        textPane.setCursor(defaultCursor);
    }

    private void saveCursor(){
       defaultCursor = textPane.getCursor();
    }
  
private Cursor defaultCursor;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane textPane;
    // End of variables declaration//GEN-END:variables

}
