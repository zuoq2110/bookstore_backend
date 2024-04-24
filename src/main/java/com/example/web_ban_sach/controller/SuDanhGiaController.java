package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SuDanhGiaService;
import com.example.web_ban_sach.Service.SuDanhGiaServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/su-danh-gia")
public class SuDanhGiaController {
@Autowired
    private SuDanhGiaService suDanhGiaService;

@PostMapping("/them")
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
    try {
        suDanhGiaService.save(jsonNode);
        return ResponseEntity.ok().build();
    }catch (Exception e){
        e.printStackTrace();
        return ResponseEntity.badRequest().build();
    }
}

@PostMapping("/xem-danh-gia")
    public ResponseEntity<?> get(@RequestBody JsonNode jsonNode){
    try {
        return suDanhGiaService.get(jsonNode);
    }catch (Exception e){
        e.printStackTrace();
        return ResponseEntity.badRequest().build();
    }
}

@PutMapping("/cap-nhat")
    public ResponseEntity<?> update(@RequestBody JsonNode jsonNode){
    return suDanhGiaService.update(jsonNode);
}

}
