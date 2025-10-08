package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SellerService;
import com.example.web_ban_sach.Service.UserService;
import com.example.web_ban_sach.dao.QuyenRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.SellerRegisterRequest;
import com.example.web_ban_sach.entity.ThongBao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nguoi-dung")
public class NguoiDungController {

    @Autowired
    private UserService userService;
    @Autowired
    private SellerService sellerService;

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody JsonNode jsonNode){
        try {
            return userService.updateProfile(jsonNode);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }



    @PutMapping("/doi-mat-khau")
    public ResponseEntity<?> doiMatKhau(@RequestBody JsonNode jsonNode){
        try{
             return userService.doiMatKhau(jsonNode);
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

    @PostMapping("/them")
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
        try {
            return userService.save(jsonNode, "add");
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/cap-nhat")
    public ResponseEntity<?> update(@RequestBody JsonNode jsonNode){
        try {
            return userService.save(jsonNode, "update");
        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/xoa/{id}")
    public ResponseEntity<?> delete(@PathVariable int id){
        return userService.delete(id);
    }

    /**
     * Đăng ký seller
     * POST /nguoi-dung/register-seller
     */
    @PostMapping("/register-seller")
    public ResponseEntity<?> registerSeller(@RequestBody SellerRegisterRequest request) {
        try {
            return sellerService.registerSeller(request);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
}
