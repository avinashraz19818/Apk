package com.xuan.pokemonz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class FileEncryptor {
    private String key;
    
    public FileEncryptor(String key) {
        this.key = key;
    }
    
    // Simple XOR encryption (Sketchware မှာအဆင်ပြေတယ်)
    public void encryptFile(String inputPath, String outputPath) throws Exception {
        java.io.File inputFile = new java.io.File(inputPath);
        java.io.File outputFile = new java.io.File(outputPath);
        
        // Read input file
        byte[] data = readFileToBytes(inputFile);
        byte[] keyBytes = key.getBytes("UTF-8");
        
        // Simple XOR encryption
        byte[] encrypted = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            encrypted[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        }
        
        // Write output file
        writeBytesToFile(outputFile, encrypted);
    }
    
    // Helper method to read file to bytes
    private byte[] readFileToBytes(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        fis.read(buffer);
        fis.close();
        return buffer;
    }
    
    // Helper method to write bytes to file
    private void writeBytesToFile(File file, byte[] data) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }
    
    // For more secure encryption (if Sketchware supports crypto libs)
    public void encryptFileAES(String inputPath, String outputPath) throws Exception {
        try {
            File inputFile = new File(inputPath);
            File outputFile = new File(outputPath);
            
            byte[] data = readFileToBytes(inputFile);
            
            // Generate key from string using SHA-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(key.getBytes("UTF-8"));
            
            // Generate IV
            byte[] iv = new byte[16];
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();
            secureRandom.nextBytes(iv);
            
            // Setup cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            // Encrypt
            byte[] encrypted = cipher.doFinal(data);
            
            // Write IV + encrypted data
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(iv);
            fos.write(encrypted);
            fos.close();
            
        } catch (Exception e) {
            // Fallback to XOR if AES fails
            encryptFile(inputPath, outputPath);
        }
    }
}