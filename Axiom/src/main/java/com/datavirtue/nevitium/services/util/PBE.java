package com.datavirtue.nevitium.services.util;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

public class PBE {

    private static int ITERATIONS = 1000;

    public static String encrypt(char[] password, String plaintext)
            throws Exception {
        
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // Begin by creating a random salt of 64 bits (8 bytes)
        byte[] salt = new byte[8];
        Random random = new Random();
        random.nextBytes(salt);

        // Create the PBEKeySpec with the given password
        PBEKeySpec keySpec = new PBEKeySpec(password);

        // Get a SecretKeyFactory for PBEWithSHAAndTwofish
        SecretKeyFactory keyFactory
                = SecretKeyFactory.getInstance("PBEWithSHAAndTwofish-CBC");
        
        // Create our key
        SecretKey key = keyFactory.generateSecret(keySpec);

        // Now create a parameter spec for our salt and iterations
        PBEParameterSpec paramSpec = new PBEParameterSpec(salt, ITERATIONS);

        // Create a cipher and initialize it for encrypting
        Cipher cipher = Cipher.getInstance("PBEWithSHAAndTwofish-CBC");
        cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

        org.bouncycastle.util.encoders.Base64 encoder = new Base64();

        String saltString = new String(encoder.encode(salt));
        String ciphertextString = new String(encoder.encode(ciphertext));

        return saltString + ciphertextString;
    }

    public static String decrypt(char[] password, String text)
            throws Exception {
        // Below we split the text into salt and text strings.
        Security.addProvider(new BouncyCastleProvider());

        String salt = null;
        String ciphertext = null;
        try {
            salt = text.substring(0, 12);

            ciphertext = text.substring(12, text.length());
        } catch (Exception e) {

            javax.swing.JOptionPane.showMessageDialog(null,
                    "Restart Nevitium and reset the password for the attempted user before retrying.");
            return "";
        }

        // BASE64Decode the bytes for the salt and the ciphertext
        org.bouncycastle.util.encoders.Base64 decoder = new org.bouncycastle.util.encoders.Base64();
        byte[] saltArray = decoder.decode(salt);
        byte[] ciphertextArray = decoder.decode(ciphertext);

        // Create the PBEKeySpec with the given password
        PBEKeySpec keySpec = new PBEKeySpec(password);

        // Get a SecretKeyFactory for PBEWithSHAAndTwofish
        SecretKeyFactory keyFactory
                = SecretKeyFactory.getInstance("PBEWithSHAAndTwofish-CBC");

        // Create our key
        SecretKey key = keyFactory.generateSecret(keySpec);

        // Now create a parameter spec for our salt and iterations
        PBEParameterSpec paramSpec
                = new PBEParameterSpec(saltArray, ITERATIONS);

        // Create a cipher and initialize it for encrypting
        Cipher cipher = Cipher.getInstance("PBEWithSHAAndTwofish-CBC");
        cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        byte[] plaintextArray = null;
        try {
            plaintextArray = cipher.doFinal(ciphertextArray);
        } catch (IllegalBlockSizeException ex) {

            return null;

        } catch (BadPaddingException ex) {

            return null;

        }

        return new String(plaintextArray);
    }
}
