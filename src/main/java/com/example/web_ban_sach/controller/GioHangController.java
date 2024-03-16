package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.GioHangService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gio-hang")
public class GioHangController {
    @Autowired
    private GioHangService gioHangService;
    @PostMapping("/them")
    public ResponseEntity<?> them(@RequestBody JsonNode jsonNode){
        try {
            return gioHangService.save(jsonNode);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @PutMapping("/cap-nhat")
    public ResponseEntity<?> capNhat(@RequestBody JsonNode jsonNode){
        try {
            gioHangService.update(jsonNode);
            return ResponseEntity.ok("success");
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

}
