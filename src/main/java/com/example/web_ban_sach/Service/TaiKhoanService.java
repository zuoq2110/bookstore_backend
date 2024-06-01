package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.QuyenRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Quyen;
import com.example.web_ban_sach.entity.ThongBao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TaiKhoanService {
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private QuyenRepository quyenRepository;

    public ResponseEntity<?> dangKyNguoiDung( NguoiDung nguoiDung){
        if(nguoiDungRepository.existsByTenDangNhap(nguoiDung.getTenDangNhap())){
            return ResponseEntity.badRequest().body(new ThongBao("Tên đăng nhập đã tồn tại"));
        }
        if(nguoiDungRepository.existsByEmail(nguoiDung.getEmail())){
            return ResponseEntity.badRequest().body(new ThongBao("Email đã tồn tại."));
        }
        String encryptPassword = passwordEncoder.encode(nguoiDung.getMatKhau());
        nguoiDung.setMatKhau(encryptPassword);
        nguoiDung.setMaKichHoat(taoMaKichHoat());
        nguoiDung.setDaKichHoat(false);
        List<Quyen> roleList = new ArrayList<>();
        roleList.add(quyenRepository.findByTenQuyen("CUSTOMER"));
        nguoiDung.setDanhSachQuyen(roleList);
        NguoiDung nguoiDung_daDangKy = nguoiDungRepository.save(nguoiDung);

        //Gửi email cho ng dùng
        guiEmailKichHoat(nguoiDung.getEmail(), nguoiDung.getMaKichHoat());
        return ResponseEntity.ok("Đăng ký thành công");
    }

    private String taoMaKichHoat() {
        return UUID.randomUUID().toString();
    }

    private void guiEmailKichHoat(String email, String maKichHoat){
        String subject = "Kích hoạt tài khoản của bạn tại BookStore";
        String text = "Vui lòng sử dụng mã sau để kích hoạt cho tài khoản <"+
                email+">: <html><body><br/><h1>"+maKichHoat+"</h1></body></html>";
        String url = "http://localhost:3000/kich-hoat/"+email+"/"+maKichHoat;
        text+="<br/>Nhấn vào đường link sau để kích hoạt tài khoản" +
                "<br/> <a href="+url+">"+url+"</a>";
        emailService.sendMessage("zuoq2110@gmail.com",email, subject, text);
    }

    public ResponseEntity<?> kichHoatTaiKhoan(String email, String maKichHoat){
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email);
        if(nguoiDung==null){
            return ResponseEntity.badRequest().body(new ThongBao("Người dùng không tồn tài"));
        }
        if(nguoiDung.isDaKichHoat()){
            return ResponseEntity.badRequest().body(new ThongBao("Tài khoản được kích hoạt"));
        }
        if(nguoiDung.getMaKichHoat().equals(maKichHoat)){
            nguoiDung.setDaKichHoat(true);
            nguoiDungRepository.save(nguoiDung);
            return ResponseEntity.ok("Kích hoạt tài khoản thành công!");
        }else{
            return ResponseEntity.badRequest().body(new ThongBao("Mã kích hoạt không chính xác!"));
        }

    }
}
