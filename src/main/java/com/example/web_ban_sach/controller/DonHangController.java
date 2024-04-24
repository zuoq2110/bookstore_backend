package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.DonHangService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/don-hang")
public class DonHangController {
    @Autowired
    private DonHangService donHangService;

    @PostMapping("/them")
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
        try {
             donHangService.save(jsonNode);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/cap-nhat")
    public ResponseEntity<?> update(@RequestBody JsonNode jsonNode){
        try {
            donHangService.update(jsonNode);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
