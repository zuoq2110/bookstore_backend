package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.JWTService;
import com.example.web_ban_sach.Service.TaiKhoanService;
import com.example.web_ban_sach.Service.UserService;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.ThongBao;
import com.example.web_ban_sach.security.JWTResponse;
import com.example.web_ban_sach.security.LoginRequest;
import lombok.extern.flogger.Flogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/tai-khoan")
@CrossOrigin(origins = "*") // Allow requests from 'http://localhost:3000'
public class TaiKhoanController {
    @Autowired
    private TaiKhoanService taiKhoanService;

    @Autowired
    private JWTService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;


    @PostMapping("/dang-ky")
    public ResponseEntity<?> dangKyNguoiDung(@Validated @RequestBody NguoiDung nguoiDung){
        ResponseEntity<?> response = taiKhoanService.dangKyNguoiDung(nguoiDung);
        return response;

    }
    @GetMapping("/kich-hoat")
    public ResponseEntity<?> kichHoatTaiKhoan(@RequestParam String email, @RequestParam String maKichHoat){
        ResponseEntity<?> response = taiKhoanService.kichHoatTaiKhoan(email, maKichHoat);
        return response;

    }

    @PostMapping("/dang-nhap")
    public ResponseEntity<?> dangNhap(@RequestBody LoginRequest loginRequest){
       try {
           Authentication authentication = authenticationManager.authenticate(
                   new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
           );
           if(authentication.isAuthenticated()){
               final String jwt = jwtService.generateToken(loginRequest.getUsername());
               
               // Lấy thông tin user từ database
               NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(loginRequest.getUsername());
               if (nguoiDung == null) {
                   nguoiDung = nguoiDungRepository.findByEmail(loginRequest.getUsername());
               }
               
               if (nguoiDung != null) {
                   // Tạo response với đầy đủ thông tin
                   JWTResponse response = new JWTResponse();
                   response.setJwt(jwt);
                   response.setId(nguoiDung.getMaNguoiDung());
                   response.setEmail(nguoiDung.getEmail());
                   response.setAdmin(nguoiDung.getDanhSachQuyen() != null && 
                           nguoiDung.getDanhSachQuyen().stream()
                           .anyMatch(q -> q.getTenQuyen().equals("ADMIN")));
                   response.setSeller(nguoiDung.isSeller());
                   response.setTenGianHang(nguoiDung.getTenGianHang());
                   
                   return ResponseEntity.ok(response);
               }
               
               return ResponseEntity.ok(new JWTResponse(jwt));
           }
       }catch (AuthenticationException e){
           return ResponseEntity.badRequest().body(new ThongBao("Tên đăng nhập hoặc mật khẩu không đúng"));
       }

        return ResponseEntity.badRequest().body(new ThongBao("Tên đăng nhập hoặc mật khẩu không đúng"));
    }

}
