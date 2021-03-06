/*
 * FileDialog.java
 *
 * Created on November 9, 2006, 9:42 AM
 */

/**
 *
 * @author  Sean K Anderson - Data Virtue 2006
 */
package com.datavirtue.axiom.ui;

import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.ui.util.Tools;
import javax.swing.JFileChooser;
import java.io.*;


public class NewCoFileDialog extends javax.swing.JDialog {
    
    /** Creates new form FileDialog */
    public NewCoFileDialog(java.awt.Frame parent, boolean modal, String folder, String file) {
        super(parent, modal);
        initComponents();
        
        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
        this.setLocation(dim.width, Tools.YSCR);
        
        // need to check for exsistance of folder and ask to create
        // need to check if dir or file and respond
        // if no create then cancel
        
        //if no file do not display file name box
        if (file.equals("")) filePanel.setVisible(false);
        folderField.setText(folder);
        fileField.setText(file);
        updatePath();
        jTextPane1.setCaretPosition(0);
        jTextPane2.setCaretPosition(0);
        this.setVisible(true);
        
    }
    
    
    private void updatePath () {
        
    
        this.setTitle(folderField.getText() + fileField.getText() + '/');
        //pathLabel.setText(folderField.getText()+fileField.getText());    
        
        
    }

    public String getFolder() {        
        return folder;        
        
    }
    
    public String getFileName () {
        
        return file_name;
        
    }
    public String getPath() {        
        
        return return_value;
        
        
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        folderPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        folderField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        filePanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        fileField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        okButton = new javax.swing.JButton();

        setAlwaysOnTop(true);
        setResizable(false);

        folderPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel1.setText("New Company Folder Location");

        folderField.setText("C:/");
        folderField.setToolTipText("Type or browse to the desired folder/directory");
        folderField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                folderFieldFocusLost(evt);
            }
        });
        folderField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                folderFieldKeyReleased(evt);
            }
        });

        browseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Find in folder.png"))); // NOI18N
        browseButton.setText("Browse");
        browseButton.setToolTipText("Select the destination folder");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        jTextPane1.setEditable(false);
        jTextPane1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jTextPane1.setText("Axiom company data is stored in folders (as opposed to a single file).  Below you can choose which folder or drive you would like to place the New Company Folder in.  The Browse window will also let you create folders if you wish.");
        jScrollPane1.setViewportView(jTextPane1);

        org.jdesktop.layout.GroupLayout folderPanelLayout = new org.jdesktop.layout.GroupLayout(folderPanel);
        folderPanel.setLayout(folderPanelLayout);
        folderPanelLayout.setHorizontalGroup(
            folderPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, folderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(folderPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, folderField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .add(browseButton))
                .addContainerGap())
        );
        folderPanelLayout.setVerticalGroup(
            folderPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(folderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 62, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(folderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(browseButton)
                .addContainerGap(34, Short.MAX_VALUE))
        );

        filePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setText("New Company Data Folder");

        fileField.setText("My Company");
        fileField.setToolTipText("Type the desired file name");
        fileField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileFieldActionPerformed(evt);
            }
        });
        fileField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                fileFieldFocusGained(evt);
            }
        });
        fileField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                fileFieldKeyReleased(evt);
            }
        });

        jTextPane2.setEditable(false);
        jTextPane2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jTextPane2.setText("Supply the name of the folder you would like to store the data in (Company Name). Although it is good to use a company name for the data folder, it is purely optional. This folder will be created inside the one you selected above.  You can see the exact folder path in the title bar at the top of this window.");
        jScrollPane2.setViewportView(jTextPane2);

        okButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Create.png"))); // NOI18N
        okButton.setText("Create");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout filePanelLayout = new org.jdesktop.layout.GroupLayout(filePanel);
        filePanel.setLayout(filePanelLayout);
        filePanelLayout.setHorizontalGroup(
            filePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, filePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(filePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .add(okButton)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .add(fileField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
                .addContainerGap())
        );
        filePanelLayout.setVerticalGroup(
            filePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(filePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(fileField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(okButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(folderPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, filePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(folderPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(filePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fileFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fileFieldFocusGained

        folderField.setText(DV.verifyPath(folderField.getText()));
        updatePath();
        
    }//GEN-LAST:event_fileFieldFocusGained

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        
        this.setTitle(DV.verifyPath(folderField.getText()+fileField.getText()+'/'));
        folder = folderField.getText();
        file_name = fileField.getText();
        
        //updatePath();
        return_value = this.getTitle();
        this.setVisible(false);
        
    }//GEN-LAST:event_okButtonActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        
         try {
        
            java.io.File file;
            String f;
        
            file = new File(folderField.getText());
            JFileChooser fileChooser = new JFileChooser(file);
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fileChooser.showOpenDialog(this);
            java.io.File curFile = fileChooser.getSelectedFile();
           
            if (returnVal == JFileChooser.CANCEL_OPTION) return;
            
            if (curFile == null ) return  ;
            
            folderField.setText(curFile.getPath());
            
            folderField.setText(DV.verifyPath(folderField.getText()));
            updatePath();
            
        }catch (Exception e) {
            
            
            DV.writeFile("fDialog.err",e.toString() + System.getProperty("line.separator") + DV.getShortDate(), true );
            javax.swing.JOptionPane.showMessageDialog(this, "There was a problem with the file system.");
            
        }
        
         
    }//GEN-LAST:event_browseButtonActionPerformed

    private void folderFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_folderFieldFocusLost
        folderField.setText(DV.verifyPath(folderField.getText()));
        updatePath();// TODO add your handling code here:
    }//GEN-LAST:event_folderFieldFocusLost

    private void folderFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_folderFieldKeyReleased
        updatePath();// TODO add your handling code here:
    }//GEN-LAST:event_folderFieldKeyReleased

    private void fileFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_fileFieldKeyReleased
        folderField.setText(DV.verifyPath(folderField.getText()));
        updatePath();// TODO add your handling code here:
    }//GEN-LAST:event_fileFieldKeyReleased

    private void fileFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fileFieldActionPerformed
    
   
    private String return_value = "";
    private String folder = "";
    private String file_name = "";
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JTextField fileField;
    private javax.swing.JPanel filePanel;
    private javax.swing.JTextField folderField;
    private javax.swing.JPanel folderPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables
    
}
