package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.QuyenRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Quyen;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
   @Autowired
    private QuyenRepository quyenRepository;

   private final ObjectMapper objectMapper;

    public UserServiceImpl(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    private String formatStringByJson(String json) {
        return json.replaceAll("\"", "");
    }

    @Override
    public ResponseEntity<?> doiMatKhau(JsonNode jsonNode){
        try {
            int maNguoiDung = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maNguoiDung"))));
            String matKhauMoi = formatStringByJson(String.valueOf(jsonNode.get("matKhauMoi")));
            String matKhauCu = formatStringByJson(String.valueOf(jsonNode.get("matKhauCu")));
            NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);

                nguoiDung.setMatKhau(bCryptPasswordEncoder.encode(matKhauMoi));
                nguoiDungRepository.save(nguoiDung);

        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
    @Override
    public ResponseEntity<?> updateProfile(JsonNode jsonNode) {
       try {
           NguoiDung nguoiDungRequest = objectMapper.treeToValue(jsonNode, NguoiDung.class);
           NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(nguoiDungRequest.getMaNguoiDung());

           nguoiDung.setTen(nguoiDungRequest.getTen());
           nguoiDung.setSoDienThoai(nguoiDungRequest.getSoDienThoai());
           nguoiDung.setHoDem(nguoiDungRequest.getHoDem());
           nguoiDung.setDiaChiGiaoHang(nguoiDungRequest.getDiaChiGiaoHang());
           nguoiDung.setGioiTinh(nguoiDungRequest.getGioiTinh());
           DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
           Instant instant = Instant.from(formatter.parse(formatStringByJson(String.valueOf(jsonNode.get("ngaySinh")))));
           java.sql.Date ngaySinh = new java.sql.Date(Date.from(instant).getTime());
           nguoiDung.setNgaySinh(ngaySinh);

           nguoiDungRepository.save(nguoiDung);
       }catch (Exception e){
           e.printStackTrace();
           ResponseEntity.badRequest().build();
       }

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<?> changeAvatar(JsonNode jsonNode) {
        try {
            NguoiDung nguoiDungRequest = objectMapper.treeToValue(jsonNode, NguoiDung.class);
            NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(nguoiDungRequest.getMaNguoiDung());
            nguoiDung.setAvatar(nguoiDungRequest.getAvatar());
            nguoiDungRepository.save(nguoiDung);

        }catch (Exception e){
            e.printStackTrace();
            ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }


}
