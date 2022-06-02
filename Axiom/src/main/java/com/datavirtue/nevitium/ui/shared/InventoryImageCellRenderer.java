package com.datavirtue.nevitium.ui.shared;

import com.datavirtue.nevitium.models.inventory.InventoryImage;
import com.datavirtue.nevitium.services.util.Scalr;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;

/**
 *
 * @author SeanAnderson
 */
public class InventoryImageCellRenderer extends JPanel implements ListCellRenderer {

    DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
    JLabel imageLabel = new JLabel();
    JLabel descriptionLabel = new JLabel();

    public InventoryImageCellRenderer() {
        setLayout(new BorderLayout());
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        imageLabel.setBorder(emptyBorder);
        descriptionLabel.setBorder(emptyBorder);
        add(imageLabel, BorderLayout.CENTER);
        add(descriptionLabel, BorderLayout.SOUTH);
               
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        defaultListCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        setBorder(defaultListCellRenderer.getBorder());
        setBackground(defaultListCellRenderer.getBackground());
        var inventoryImage = (InventoryImage)value;
        ByteArrayInputStream bis = new ByteArrayInputStream(inventoryImage.getImage());
        try {
            BufferedImage wPic = ImageIO.read(bis);
            var scaled = Scalr.resize(wPic, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, 100);
            imageLabel.setIcon(new ImageIcon(scaled));
            descriptionLabel.setText(inventoryImage.getCaption()); 
            
        } catch (IOException ex) {
            Logger.getLogger(InventoryImageCellRenderer.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        return this;
    }
}
