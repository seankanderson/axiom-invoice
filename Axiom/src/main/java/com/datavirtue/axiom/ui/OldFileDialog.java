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
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.JFileChooser;
import java.io.*;


public class OldFileDialog extends javax.swing.JDialog {
    
    /** Creates new form FileDialog */
    public OldFileDialog(java.awt.Frame parent, boolean modal, String folder, String file) {
        super(parent, modal);
        initComponents();
        Toolkit tools = Toolkit.getDefaultToolkit();
        Image winIcon = tools.getImage(getClass().getResource("/Orange.png"));
        this.setIconImage(winIcon);
        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
        this.setLocation(dim.width, dim.height);
        
        // need to check for exsistance of folder and ask to create
        // need to check if dir or file and respond
        // if no create then cancel
        
        //if no file do not display file name box
        if (file.equals("")) filePanel.setVisible(false);
        folderField.setText(folder);
        fileField.setText(file);
        updatePath();
        
    }
    
    
    private void updatePath () {
        
    
        this.setTitle(folderField.getText()+fileField.getText());
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
        filePanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        fileField = new javax.swing.JTextField();
        jToolBar1 = new javax.swing.JToolBar();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);

        folderPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setText("FOLDER");

        folderField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
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
        browseButton.setIconTextGap(8);
        browseButton.setMargin(new java.awt.Insets(2, 7, 2, 7));
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout folderPanelLayout = new org.jdesktop.layout.GroupLayout(folderPanel);
        folderPanel.setLayout(folderPanelLayout);
        folderPanelLayout.setHorizontalGroup(
            folderPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(folderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(folderPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(folderField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .add(jLabel1)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, browseButton))
                .addContainerGap())
        );
        folderPanelLayout.setVerticalGroup(
            folderPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(folderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(folderField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(browseButton)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        filePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel2.setText("FILE NAME");

        fileField.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        fileField.setToolTipText("Type the desired file name");
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

        org.jdesktop.layout.GroupLayout filePanelLayout = new org.jdesktop.layout.GroupLayout(filePanel);
        filePanel.setLayout(filePanelLayout);
        filePanelLayout.setHorizontalGroup(
            filePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(filePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(filePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(fileField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                    .add(jLabel2))
                .addContainerGap())
        );
        filePanelLayout.setVerticalGroup(
            filePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(filePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        cancelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/No.png"))); // NOI18N
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(cancelButton);

        okButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Next.png"))); // NOI18N
        okButton.setText("Go");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(okButton);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(folderPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, filePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(folderPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(filePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(2, 2, 2)
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fileFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fileFieldFocusGained

        folderField.setText(DV.verifyPath(folderField.getText()));
        updatePath();
        
    }//GEN-LAST:event_fileFieldFocusGained

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        return_value = "";
        this.setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        
        folderField.setText(DV.verifyPath(folderField.getText()));
        folder = folderField.getText();
        file_name = fileField.getText();
        
        updatePath();
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
    
   
    private String return_value = "";
    private String folder = "";
    private String file_name = "";
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField fileField;
    private javax.swing.JPanel filePanel;
    private javax.swing.JTextField folderField;
    private javax.swing.JPanel folderPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables
    
}
