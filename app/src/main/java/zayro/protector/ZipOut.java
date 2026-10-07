package com.xuan.pokemonz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipOut {
    
    ZipFile inputZipFile;
    String outPath;
    ZipOutputStream zos;
    List<String> saveList = new ArrayList<>();
    List<String> removeList = new ArrayList<>();
    List<String> entries = new ArrayList<>();

    public ZipOut(String outPath) throws Exception {
        this.outPath = outPath;
        if (new File(outPath).exists()) {
            File file = new File(outPath);
            file.delete();
        }
        this.zos = new ZipOutputStream(new FileOutputStream(outPath));
    }

    public ZipOut setInput(ZipFile zipFile) throws Exception {
        this.inputZipFile = zipFile;
        readZip(inputZipFile);
        return this;
    }

    // Normal file addition
    public void addFile(String entry, byte[] data) throws IOException {
        // Special handling for resources.arsc for Android R+
        if (entry.equals("resources.arsc")) {
            addFileUncompressed(entry, data, true); // 4-byte aligned
        } else {
            ZipEntry zipEntry = new ZipEntry(entry);
            zos.putNextEntry(zipEntry);
            zos.write(data);
            zos.closeEntry();
        }
        saveList.add(entry);
    }

    // Method for adding uncompressed files with 4-byte alignment
    private void addFileUncompressed(String entry, byte[] data, boolean align4Byte) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entry);
        
        // Set as STORED (uncompressed)
        zipEntry.setMethod(ZipEntry.STORED);
        
        byte[] finalData = data;
        
        // 4-byte alignment for Android R+
        if (align4Byte) {
            finalData = alignTo4Bytes(data);
        }
        
        // Set required fields for STORED method
        zipEntry.setSize(finalData.length);
        zipEntry.setCompressedSize(finalData.length);
        
        // Calculate CRC32
        CRC32 crc = new CRC32();
        crc.update(finalData);
        zipEntry.setCrc(crc.getValue());
        
        // Add to zip
        zos.putNextEntry(zipEntry);
        zos.write(finalData);
        zos.closeEntry();
    }

    // Align data to 4-byte boundary
    private byte[] alignTo4Bytes(byte[] data) {
        int remainder = data.length % 4;
        if (remainder == 0) {
            return data; // Already aligned
        }
        
        // Pad with zeros to make it 4-byte aligned
        int alignedLength = data.length + (4 - remainder);
        byte[] alignedData = new byte[alignedLength];
        System.arraycopy(data, 0, alignedData, 0, data.length);
        
        return alignedData;
    }

    public void removeFile(String entry) {
        removeList.add(entry);
    }

    public void save() throws Exception {
        if (inputZipFile == null) {
            throw new FileNotFoundException("The input file was not found!");
        }

        Iterator<String> entryIterator = entries.iterator();
        while (entryIterator.hasNext()) {
            String key = entryIterator.next();

            if (removeList.contains(key) || saveList.contains(key)) {
                continue;
            }

            ZipEntry originalEntry = inputZipFile.getEntry(key);
            if (originalEntry != null) {
                byte[] data = FileUtils.toByteArray(inputZipFile.getInputStream(originalEntry));
                
                // Special handling for resources.arsc from original APK
                if (key.equals("resources.arsc")) {
                    // For resources.arsc, use uncompressed with 4-byte alignment
                    addFileUncompressed(key, data, true);
                } else {
                    // Check if this is a compressed entry
                    boolean shouldCompress = shouldCompressFile(key);
                    
                    if (!shouldCompress && isImageOrResourceFile(key)) {
                        // Keep images uncompressed
                        addFileUncompressed(key, data, false);
                    } else {
                        // Normal compressed file
                        ZipEntry newEntry = new ZipEntry(key);
                        
                        // Preserve original compression method
                        if (originalEntry.getMethod() == ZipEntry.STORED) {
                            newEntry.setMethod(ZipEntry.STORED);
                            newEntry.setSize(data.length);
                            newEntry.setCompressedSize(data.length);
                            
                            CRC32 crc = new CRC32();
                            crc.update(data);
                            newEntry.setCrc(crc.getValue());
                        }
                        
                        zos.putNextEntry(newEntry);
                        zos.write(data);
                        zos.closeEntry();
                    }
                }
            }
        }
        
        zos.close();
        inputZipFile.close();
        
        // Try to zipalign the APK
        try {
            zipalignApk();
        } catch (Exception e) {
            // If zipalign fails, continue anyway
            e.printStackTrace();
        }
    }

    // Check if file should be compressed
    private boolean shouldCompressFile(String fileName) {
        // These file types should remain uncompressed
        String[] uncompressedExtensions = {
            ".jpg", ".jpeg", ".png", ".gif", ".webp",  // Images
            ".mp3", ".wav", ".ogg", ".aac",            // Audio
            ".mp4", ".avi", ".mkv", ".webm",           // Video
            ".zip", ".apk", ".jar"                     // Archives
        };
        
        for (String ext : uncompressedExtensions) {
            if (fileName.toLowerCase().endsWith(ext)) {
                return false;
            }
        }
        
        return true;
    }

    // Check if file is image or resource file
    private boolean isImageOrResourceFile(String fileName) {
        return fileName.startsWith("res/") || 
               fileName.endsWith(".png") || 
               fileName.endsWith(".jpg") || 
               fileName.endsWith(".jpeg") ||
               fileName.endsWith(".webp");
    }

    // Apply zipalign to the output APK
    private void zipalignApk() throws Exception {
        // Create temp aligned file path
        String alignedPath = outPath.replace(".apk", "_aligned.apk");
        
        // Check if zipalign is available
        try {
            Process process = Runtime.getRuntime().exec("zipalign");
            process.waitFor();
        } catch (IOException e) {
            // zipalign not available, skip alignment
            return;
        }
        
        try {
            // Execute zipalign command
            Process process = Runtime.getRuntime().exec(new String[] {
                "zipalign",
                "-f",  // Overwrite existing file
                "-p",  // Memory page aligned
                "4",   // 4-byte alignment
                outPath,
                alignedPath
            });
            
            // Wait for process to complete
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Replace original with aligned version
                File originalFile = new File(outPath);
                File alignedFile = new File(alignedPath);
                
                if (alignedFile.exists() && alignedFile.length() > 0) {
                    originalFile.delete();
                    alignedFile.renameTo(originalFile);
                }
            }
            
        } catch (Exception e) {
            throw new IOException("Failed to zipalign: " + e.getMessage());
        }
    }

    private void readZip(ZipFile zip) throws Exception {
        Enumeration<? extends ZipEntry> enums = zip.entries();
        while (enums.hasMoreElements()) {
            ZipEntry entry = enums.nextElement();
            String entryName = entry.getName();
            entries.add(entryName);
        }
    }
}