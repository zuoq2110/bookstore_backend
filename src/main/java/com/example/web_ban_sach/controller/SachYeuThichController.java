package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SachYeuThichService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sach-yeu-thich")
public class SachYeuThichController {
    @Autowired
    private SachYeuThichService sachYeuThichService;
    @GetMapping("/lay-sach/{maNguoiDung}")
    public ResponseEntity<?> get(@PathVariable Integer maNguoiDung){

        try {
           return sachYeuThichService.get(maNguoiDung);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }
    @PostMapping("/them")
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
        try {
            sachYeuThichService.save(jsonNode);
            return ResponseEntity.ok().build();
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/xoa")
    public ResponseEntity<?> delete(@RequestBody JsonNode jsonNode){
        try {
            sachYeuThichService.delete(jsonNode);

        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}
