package com.example.web_ban_sach.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddBookWithImageRequest {
    private AddBookRequest bookData;
    private String coverImage; // Base64 image string
}
