/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * VATCalculator.java
 *
 * Created on Jan 21, 2011, 12:55:23 AM
 */

package com.datavirtue.axiom.ui;

import com.datavirtue.axiom.services.util.CurrencyUtil;
import com.datavirtue.axiom.services.util.DV;
import com.datavirtue.axiom.ui.util.JTextFieldFilter;


/**
 *
 * @author dataVirtue
 */
public class VATCalculator extends javax.swing.JDialog {
private double tax_rate = .15d;
    /** Creates new form VATCalculator */
    public VATCalculator(java.awt.Frame parent, boolean modal,double tax_rate) {
        super(parent, modal);
        initComponents();
        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
        this.setLocation(dim.width, dim.height);
        this.entryField.setDocument(new JTextFieldFilter(JTextFieldFilter.FLOAT));
        this.tax_rate = tax_rate;
        this.setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        entryField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        baseField = new javax.swing.JTextField();
        VATAddedField = new javax.swing.JTextField();

        setTitle("VAT - GST Decalculator");
        setAlwaysOnTop(true);
        setResizable(false);

        entryField.setFont(new java.awt.Font("Tahoma", 0, 48)); // NOI18N
        entryField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                entryFieldMouseClicked(evt);
            }
        });
        entryField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                entryFieldKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                entryFieldKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                entryFieldKeyTyped(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Base Amount");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("VAT Added");
        jLabel2.setFocusable(false);
        jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        baseField.setEditable(false);
        baseField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        baseField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

        VATAddedField.setEditable(false);
        VATAddedField.setPreferredSize(new java.awt.Dimension(6, 21));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(entryField, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(baseField, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE))
                        .addGap(123, 123, 123)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(VATAddedField, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(entryField, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(baseField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(VATAddedField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void entryFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_entryFieldKeyTyped

        
        

    }//GEN-LAST:event_entryFieldKeyTyped

    private void entryFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_entryFieldKeyReleased

        double entry = DV.parseDouble(entryField.getText());
        double base = (entry * (100/(100+(tax_rate*100)) ) );
        double with = (entry + (entry * tax_rate));

        baseField.setText(CurrencyUtil.money(base));
        VATAddedField.setText(CurrencyUtil.money(with));

    }//GEN-LAST:event_entryFieldKeyReleased

    private void entryFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_entryFieldMouseClicked
        if (evt.getClickCount() == 2){
            entryField.selectAll();
            entryField.copy();
            this.dispose();
        }
    }//GEN-LAST:event_entryFieldMouseClicked

    private void entryFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_entryFieldKeyPressed

        if (evt.isControlDown() && evt.getKeyCode() == evt.VK_ENTER){

            baseField.selectAll();
            baseField.copy();
            this.dispose();

        }

        if (evt.getKeyCode() == evt.VK_ENTER){
            VATAddedField.selectAll();
            VATAddedField.copy();
            this.dispose();
        }

    }//GEN-LAST:event_entryFieldKeyPressed

    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField VATAddedField;
    private javax.swing.JTextField baseField;
    private javax.swing.JTextField entryField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables

}
