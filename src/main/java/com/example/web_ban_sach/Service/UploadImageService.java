package com.example.web_ban_sach.Service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadImageService {
    String uploadImage(MultipartFile multipartFile, String name);
    void deleteImage(String imgUrl);
}
