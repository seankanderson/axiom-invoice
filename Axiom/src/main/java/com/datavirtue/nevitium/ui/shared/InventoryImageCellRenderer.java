package com.datavirtue.nevitium.ui.shared;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
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
        imageLabel.setIcon((Icon) value);
        descriptionLabel.setText("Description");
        return this;
    }
}
