package com.datavirtue.axiom.services;

import com.datavirtue.axiom.services.util.Scalr;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 *
 * @author SeanAnderson
 */
public class ImageService {

    public String convertImageToBase64Png(BufferedImage buffered) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "png", baos);
        var byteArray = baos.toByteArray();
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public BufferedImage convertBase64PngToBufferedImage(String base64Image) throws IOException {
        var imageBytes = Base64.getDecoder().decode(base64Image);
        var inputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
        return ImageIO.read(inputStream);
    }

    public BufferedImage scaleImage(BufferedImage image, Dimension newSize) {
        return Scalr.resize(image, Scalr.Method.QUALITY, newSize.width, newSize.height);
    }

    public void resizeAndSetImageToJLabel(BufferedImage image, JLabel label) {
        Dimension newMaxSize = new Dimension(label.getWidth(), label.getHeight());
        var resizedImg = scaleImage(image, newMaxSize);
        label.setIcon(new ImageIcon(resizedImg));
    }

    public String createHtmlImgSrc(String base64PngImage) {
        return "\"data:image/png;base64," + base64PngImage + "\"";
    }

    public BufferedImage getImageFromUrl(URL url) throws IOException {
        return ImageIO.read(url);
    }

    public BufferedImage getImageFromDisk(File file) throws IOException {
        return ImageIO.read(file);
    }

    //invoiceItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ViewInvoice.png"))); // NOI18N
}
