package com.example.web_ban_sach.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class Base64MultipartFile implements MultipartFile {
    
    private final byte[] fileContent;
    private final String fileName;
    private final String contentType;
    
    public Base64MultipartFile(String base64Data) {
        // Xử lý base64 string có thể có prefix "data:image/png;base64," hoặc không
        String base64 = base64Data;
        String extractedContentType = "image/jpeg"; // default
        
        if (base64Data.contains(",")) {
            String[] parts = base64Data.split(",");
            if (parts[0].contains(":") && parts[0].contains(";")) {
                // Extract content type from data:image/png;base64
                String contentTypePart = parts[0].substring(parts[0].indexOf(":") + 1, parts[0].indexOf(";"));
                extractedContentType = contentTypePart;
            }
            base64 = parts[1];
        }
        
        this.fileContent = Base64.getDecoder().decode(base64);
        this.contentType = extractedContentType;
        
        // Generate filename based on content type
        String extension = "jpg";
        if (extractedContentType.contains("png")) {
            extension = "png";
        } else if (extractedContentType.contains("jpeg")) {
            extension = "jpeg";
        } else if (extractedContentType.contains("gif")) {
            extension = "gif";
        }
        
        this.fileName = "cover." + extension;
    }
    
    @Override
    public String getName() {
        return fileName;
    }
    
    @Override
    public String getOriginalFilename() {
        return fileName;
    }
    
    @Override
    public String getContentType() {
        return contentType;
    }
    
    @Override
    public boolean isEmpty() {
        return fileContent == null || fileContent.length == 0;
    }
    
    @Override
    public long getSize() {
        return fileContent.length;
    }
    
    @Override
    public byte[] getBytes() throws IOException {
        return fileContent;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(fileContent);
    }
    
    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(fileContent);
        }
    }
}
