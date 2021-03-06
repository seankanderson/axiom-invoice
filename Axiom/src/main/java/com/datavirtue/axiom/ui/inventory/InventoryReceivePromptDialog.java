/*
 * InventoryReceivePromptDialog.java
 *
 * Created on December 9, 2008, 1:26 AM
 */

package com.datavirtue.axiom.ui.inventory;
import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.ui.util.JTextFieldFilter;



/**
 *
 * @author  Data Virtue
 */
public class InventoryReceivePromptDialog extends javax.swing.JDialog {
    
    /** Creates new form InventoryReceivePromptDialog */
    public InventoryReceivePromptDialog(java.awt.Frame parent, boolean modal, String item) {
        super(parent, modal);
        initComponents();
        itemTextField.setText(item);
        amountTextField.requestFocus();
        amountTextField.setDocument(new JTextFieldFilter(JTextFieldFilter.FLOAT));
        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
        this.setLocation(dim.width, dim.height-100);
        
        this.setVisible(true);
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        itemTextField = new javax.swing.JTextField();
        amountTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Receive Inventory");
        setAlwaysOnTop(true);

        itemTextField.setEditable(false);
        itemTextField.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        itemTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        amountTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        amountTextField.setText("0.00");
        amountTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                amountTextFieldFocusGained(evt);
            }
        });
        amountTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                amountTextFieldKeyPressed(evt);
            }
        });

        jLabel1.setText("Receive Amount:");

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("(Press ENTER to Update Quantity)");

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Treat Current Negative Quantities as Zero");
        jCheckBox1.setToolTipText("If the inventory record currently shows a negative value checking this box will treat it as zero.");
        jCheckBox1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox1.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(itemTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(amountTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 225, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jCheckBox1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(itemTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(amountTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jCheckBox1)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void amountTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_amountTextFieldKeyPressed
        
         
        if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) this.dispose();
        
        if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER){
            
             try {
                
                inQty = Float.parseFloat(amountTextField.getText().trim());
                if (inQty < 0) inQty = 0;
                negZero = jCheckBox1.isSelected();
                this.setVisible(false);
                
            } catch (NumberFormatException ex) {
                
                amountTextField.setBackground(new java.awt.Color(255,51,51));
                amountTextField.selectAll();
                
                return;
                
            }
             
             
         }
        
    }//GEN-LAST:event_amountTextFieldKeyPressed

    private void amountTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_amountTextFieldFocusGained
        
        amountTextField.selectAll();
        
    }//GEN-LAST:event_amountTextFieldFocusGained
    
    public float getInAmount() {
        
        return inQty;
        
    }
    
    public boolean isNegZero() {
        
        return negZero;
        
    }
    
    private float inQty = 0;
    private boolean negZero = true;
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField amountTextField;
    private javax.swing.JTextField itemTextField;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables
    
}
