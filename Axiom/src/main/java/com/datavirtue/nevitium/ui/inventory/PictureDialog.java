/*
 * PictureDialog.java
 *
 * Created on February 5, 2007, 10:53 PM
 */

package com.datavirtue.nevitium.ui.inventory;
import com.datavirtue.nevitium.services.util.DV;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;
import javax.swing.*;
        
/**
 *
 * @author  Sean K Anderson - Data Virtue
 * Copyright Data Virtue 2007
 */
public class PictureDialog extends javax.swing.JDialog {
    
    /** Creates new form PictureDialog */
    public PictureDialog(java.awt.Frame parent, boolean modal, String filename, boolean fit) {
        super(parent, modal);
        initComponents();
        
        this.fit = fit;
        
        
        
       /* Close dialog on escape */
        ActionMap am = getRootPane().getActionMap();
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        Object windowCloseKey = new Object();
        KeyStroke windowCloseStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        Action windowCloseAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        im.put(windowCloseStroke, windowCloseKey);
        am.put(windowCloseKey, windowCloseAction);
        /**/
        
        java.awt.Dimension dim = DV.computeCenter((java.awt.Window) this);
                this.setLocation(dim.width, dim.height);
        
        small = this.getSize();
        
        if (new java.io.File(filename).exists()){
            
            filename = filename.replace('\\','/');
            //picPanel1.setImage(filename, true);
              loadImage(filename);  
            /*picLabel.setIcon(new javax.swing.ImageIcon(filename));*/
            
            this.setVisible(true);
            
        }else {
            
            javax.swing.JOptionPane.showMessageDialog(null, "The picture (file) you tried to load was not found!");
            this.dispose();
                        
        }
        
        
        
    }
    
    public boolean loadImage(String filename) {
                                
        file = filename;
        
        image = Toolkit.getDefaultToolkit().getImage(file);
        
        IMAGE_HEIGHT = image.getHeight(null);  //RAW
        IMAGE_WIDTH = image.getWidth(null);
        
        MediaTracker tracker = new MediaTracker(this);
        
        tracker.addImage(image, 0);
        
        
        try {
                        
            tracker.waitForID(0);
            
        } catch (InterruptedException ex) {
            return false;
            //ex.printStackTrace();
        }
        
        try {
            /* How do you catch out of memory errors? */
            image = ImageIO.read(new File (file));
            
        } catch (IOException ex) {
            return false;

            //ex.printStackTrace();
        }        
        
        defaultSize = new ImageIcon(image);
        
        if (image.getWidth(null) < (int)jScrollPane1.getViewport().getSize().getWidth()){
            
            fitBox.setSelected(false);
            fit = false;
            
        }
        
        picLabel.setIcon(resizeImage());      
        
        return true;        
        
    }
    
    protected ImageIcon resizeImage() {
        
        /*resize image to scrollpane viewport size */
        /* add image to picLabel */
        
        if (fit){
            
                      
           return new ImageIcon(image.getScaledInstance((int)jScrollPane1.getViewport().getSize().getWidth(),
                    (int)jScrollPane1.getViewport().getSize().getHeight(), image.SCALE_FAST ));
                                  
            
        }else {
            
            /* not scaled */
            return defaultSize;            
            
        }
    }
    
public void setFit (boolean v){
    
    fit = v;
        
}
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        picLabel = new javax.swing.JLabel();
        fitBox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Pic Viewer");
        setAlwaysOnTop(true);
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 3));
        jPanel1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jPanel1KeyPressed(evt);
            }
        });

        jScrollPane1.setPreferredSize(new java.awt.Dimension(400, 300));

        picLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        picLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                picLabelMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(picLabel);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 578, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
        );

        fitBox.setSelected(true);
        fitBox.setText("Fit To Window");
        fitBox.setToolTipText("Scale the Image");
        fitBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        fitBox.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        fitBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        fitBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fitBoxActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Esc - Close Window   /   Click Image Area to Maximize or Minimize Window");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
                    .add(fitBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 99, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(fitBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel1)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void picLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_picLabelMouseClicked
        
        max();
        
    }//GEN-LAST:event_picLabelMouseClicked

    private void fitBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fitBoxActionPerformed
        
        fit = fitBox.isSelected();
        
        
        picLabel.setIcon(resizeImage());
       
    }//GEN-LAST:event_fitBoxActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        
        
        picLabel.setIcon(resizeImage());
        
        /* Scale image to window unless maximized ???  */
        
        
    }//GEN-LAST:event_formComponentResized

    private void jPanel1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jPanel1KeyPressed
        
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE){
            
            this.dispose();
            
        }
        
        
    }//GEN-LAST:event_jPanel1KeyPressed

    private void max () {
        
        picLabel.setIcon(new ImageIcon());
        
        if (max) {
           
            max = false;
            
            
        }else {
            
            small = this.getSize();
            spot = this.getLocation();
            max = true;
            
        }
        
        
        java.awt.Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                if (!max) this.setSize(small);
                    else this.setSize(dim);
                
        dim = DV.computeCenter((java.awt.Window) this);
                if (!max) this.setLocation(spot);
                    else this.setLocation(dim.width, dim.height);
        
        picLabel.setIcon(resizeImage());
        
        this.validateTree();
        
        
    }
    
    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        
        
        
    }//GEN-LAST:event_formKeyPressed
    
  

    private boolean max=false;
    private java.awt.Dimension small;
    private java.awt.Point spot;
    
    /* image formatting */
    protected boolean fit;
    
    protected int IMAGE_WIDTH;
    protected int IMAGE_HEIGHT;
    
    protected int PANEL_WIDTH;
    protected int PANEL_HEIGHT;
    
    protected java.awt.Image image;
    protected ImageIcon defaultSize;
    
    protected String file;
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox fitBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel picLabel;
    // End of variables declaration//GEN-END:variables
    
}