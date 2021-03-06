/*
 * OpenCompanyDialog.java
 *
 * Created on October 20, 2007, 1:30 AM
 */

package com.datavirtue.axiom.ui;
import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.ui.util.Tools;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.*;

/**
 *
 * @author  Data Virtue
 */
public class OpenCompanyDialog extends javax.swing.JDialog {
    private Image winIcon;
    /** Creates new form OpenCompanyDialog */
    public OpenCompanyDialog(java.awt.Frame parent, boolean modal, String inf) {
        super(parent, modal);
        initComponents();
        Toolkit tools = Toolkit.getDefaultToolkit();
        winIcon = tools.getImage(getClass().getResource("/Orange.png"));
        this.setIconImage(winIcon);
        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
        this.setLocation(dim.width, Tools.YSCR);
        this.populateFolderList(inf);
        infFile = inf;
        
        this.setVisible(true);
        if (jList1.getModel().getSize() > 0) jList1.setSelectedIndex(0);
        
    }
    
    private String infFile;
    private String status = "cancel";
    
    public String getStatus() {
        
        return status;
        
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        openButton = new javax.swing.JButton();
        newButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList();
        openPreviousButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Change Folder");
        setAlwaysOnTop(true);

        openButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Folder tree.png"))); // NOI18N
        openButton.setText("Browse To Existing Company Folder");
        openButton.setMargin(new java.awt.Insets(14, 14, 14, 14));
        openButton.setNextFocusableComponent(newButton);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });

        newButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        newButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-16/enabled/Create.png"))); // NOI18N
        newButton.setText("Create New Company (F2)");
        newButton.setMargin(new java.awt.Insets(14, 14, 14, 14));
        newButton.setNextFocusableComponent(jList1);
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });

        cancelButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        cancelButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/No.png"))); // NOI18N
        cancelButton.setText("Cancel");
        cancelButton.setNextFocusableComponent(openButton);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jList1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1.setToolTipText("Select from previously opened folders.");
        jList1.setSelectedIndex(0);
        jList1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jList1KeyPressed(evt);
            }
        });
        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jList1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jList1);

        openPreviousButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        openPreviousButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Aha-24/enabled/Open.png"))); // NOI18N
        openPreviousButton.setText("Open Previous");
        openPreviousButton.setEnabled(false);
        openPreviousButton.setNextFocusableComponent(cancelButton);
        openPreviousButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openPreviousButtonActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Previously Opened Folders");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, newButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, openButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(jSeparator2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, cancelButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(jSeparator3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                    .add(openPreviousButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(openButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(newButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 157, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(openPreviousButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(cancelButton)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
private void deleteItem() {
    
             
            int listAmount = jList1.getModel().getSize();
            int listSelection = jList1.getSelectedIndex();
            if (listSelection < 0) return;
            
            try {
                
            File data = new File (infFile);
            
            PrintWriter out = new PrintWriter(
                    new BufferedWriter( 
                     new FileWriter (data, false ) ) );
                
            String line;
            String nl = System.getProperty("line.separator");
            
            for (int r = 0; r < listAmount; r++){
                line = (String)jList1.getModel().getElementAt(r);
                if (r != listSelection && line != null) out.write(line + nl);
                
            }
            out.close();
            out = null;           
            
        } catch (Exception e) {
            
            javax.swing.JOptionPane.showMessageDialog(null, "Update to "+infFile+" failed.");
            
            }
            
            this.populateFolderList(infFile);
        
}
    private void jList1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jList1KeyPressed
        
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE){
           
            deleteItem();
            
            }
        
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER){
           
            this.openAction();
            
            }
        
       if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_F2){
           
            this.newAction();
            
            } 
        
    }//GEN-LAST:event_jList1KeyPressed

    private void jList1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jList1MouseClicked
        
        if (jList1.getSelectedIndex() > -1){
            
            openPreviousButton.setEnabled(true);
            
            
        }
        
        
    }//GEN-LAST:event_jList1MouseClicked
private String previousPath="";

private void openAction() {
    
    if (jList1.getSelectedIndex() > -1){
            
            
            previousPath = (String)jList1.getModel().getElementAt(jList1.getSelectedIndex());
            status = "previous";
            this.dispose();
            
        }
    
}
    private void openPreviousButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPreviousButtonActionPerformed
        
        
        openAction();
        
        
    }//GEN-LAST:event_openPreviousButtonActionPerformed

    public String getPath() {
        
        return previousPath;
        
    }
    
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        status = "cancel";
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
        
        this.setAlwaysOnTop(false);
        newAction();
        
    }//GEN-LAST:event_newButtonActionPerformed
private void newAction() {
   
    status = "create";
        this.dispose();
    
}
    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        
        status = "open";
        this.dispose();
        
    }//GEN-LAST:event_openButtonActionPerformed
    
    private void populateFolderList(String file) {
        
       if (!new File(file).exists()) {
           //if no file then start one 
           DV.writeFile(file, "data" + System.getProperty("line.separator"), false);
           
       }
        
        BufferedReader in=null;
    try {
                in = new BufferedReader(new FileReader(file));
                                
                
                String line="";
                java.util.ArrayList al = new java.util.ArrayList();
                java.util.Vector v = new java.util.Vector();
                                                              
               while (line != null)    {
                    
                   
                    line = in.readLine();
                    //if (!v.contains(line)) 
                    v.add(line);
                    
                    }
                
               
                v.trimToSize();
                jList1.setListData(v);
                   
                in.close();
                        in = null;    //clean up
                        line = null;
                        return;
                        


    }catch (Exception e) {
             
        e.printStackTrace();
        
    }finally {
        
        
        try {
                     
                if (in != null) in.close();
                in = null;
                                               
            } catch (IOException ex) {
                
                ex.printStackTrace();
                
            }}
        
        
        
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JButton newButton;
    private javax.swing.JButton openButton;
    private javax.swing.JButton openPreviousButton;
    // End of variables declaration//GEN-END:variables
    
}
