package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.SachService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sach")
public class SachController {
    @Autowired
    private SachService sachService;
    @PostMapping("/them-sach")
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
       return sachService.save(jsonNode);
    }

    @PutMapping("/cap-nhat")
    public ResponseEntity<?> update(@RequestBody JsonNode jsonNode){
        return sachService.update(jsonNode);
    }
}
