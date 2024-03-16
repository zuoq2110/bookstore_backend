package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nguoi-dung")
public class NguoiDungController {
    @Autowired
    private UserService userService;
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody JsonNode jsonNode){
        try {
            return userService.updateProfile(jsonNode);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }

    @PutMapping("/doi-avatar")
    public ResponseEntity<?> changeAvatar(@RequestBody JsonNode jsonNode){
       try {
           return userService.changeAvatar(jsonNode);
       }catch (Exception e){
           e.printStackTrace();
           return ResponseEntity.badRequest().build();
       }
    }
}
