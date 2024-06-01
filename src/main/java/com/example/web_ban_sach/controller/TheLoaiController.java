package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.dao.TheLoaiRepository;
import com.example.web_ban_sach.entity.TheLoai;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/theLoai")
public class TheLoaiController {
    @Autowired
    private TheLoaiRepository theLoaiRepository;
    private final ObjectMapper objectMapper;

    public TheLoaiController(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    @GetMapping
    public ResponseEntity<?> get(){
        try{
            List<TheLoai> danhSachTheLoai = theLoaiRepository.findAll();
return ResponseEntity.ok().body(danhSachTheLoai);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }
    @PostMapping
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
        try{
            TheLoai theLoai = objectMapper.treeToValue(jsonNode, TheLoai.class);
            theLoaiRepository.save(theLoai);
            return ResponseEntity.ok().build();
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody JsonNode jsonNode){
        try{
            Optional<TheLoai> theLoai = theLoaiRepository.findById(id);
            TheLoai theLoai1 = objectMapper.treeToValue(jsonNode, TheLoai.class);
            theLoai.get().setTenTheLoai(theLoai1.getTenTheLoai());
            theLoaiRepository.save(theLoai.get());
            return ResponseEntity.ok().body(theLoai);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id){
        try{
            Optional<TheLoai> theLoai = theLoaiRepository.findById(id);
            theLoaiRepository.delete(theLoai.get());
            return ResponseEntity.ok().build();
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }
}
