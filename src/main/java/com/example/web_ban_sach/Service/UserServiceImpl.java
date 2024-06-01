package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.Service.util.Base64ToMultipartFileConverter;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.QuyenRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Quyen;
import com.example.web_ban_sach.entity.ThongBao;
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
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
   @Autowired
    private QuyenRepository quyenRepository;
@Autowired
private UploadImageService uploadImageService;
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
    public ResponseEntity<?> save(JsonNode jsonNode, String option) {
        try {
            NguoiDung nguoiDung = objectMapper.treeToValue(jsonNode, NguoiDung.class);
            System.out.println(nguoiDung);
            if(!option.equals("update")){
                if(nguoiDungRepository.existsByTenDangNhap(nguoiDung.getTenDangNhap())){
                    return ResponseEntity.badRequest().body(new ThongBao("Tên đăng nhập đã tồn tại"));
                }
                if(nguoiDungRepository.existsByEmail(nguoiDung.getEmail())){
                    return ResponseEntity.badRequest().body(new ThongBao("Email đã tồn tại"));
                }

            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            Instant instant = Instant.from(formatter.parse(formatStringByJson(String.valueOf(jsonNode.get("ngaySinh")))));
            java.sql.Date ngaySinh = new java.sql.Date(Date.from(instant).getTime());
            nguoiDung.setNgaySinh(ngaySinh);

            int maQuyenResponse = Integer.parseInt(String.valueOf(jsonNode.get("quyen")));
            Optional<Quyen> quyen = quyenRepository.findById(maQuyenResponse);
            List<Quyen> danhSachQuyen = new ArrayList<>();
            danhSachQuyen.add(quyen.get());
            nguoiDung.setDanhSachQuyen(danhSachQuyen);

            if(!(nguoiDung.getMatKhau() == null)){
                String maHoaMatKhau = bCryptPasswordEncoder.encode(nguoiDung.getMatKhau());
                nguoiDung.setMatKhau(maHoaMatKhau);
            }

            String avatar = formatStringByJson(String.valueOf((jsonNode.get("avatar"))));
            if(avatar.length() > 500){
                MultipartFile avatarFile = Base64ToMultipartFileConverter.convert(avatar);
                String avatarURL = uploadImageService.uploadImage(avatarFile, "User_" + nguoiDung.getMaNguoiDung());
                nguoiDung.setAvatar(avatarURL);
            }
            nguoiDungRepository.save(nguoiDung);


}catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok("Thành công");
    }

    @Override
    public ResponseEntity<?> delete(int id) {
        try {

            Optional<NguoiDung> nguoiDung = nguoiDungRepository.findById(id);

            if (nguoiDung.isPresent()) {
                String avt = nguoiDung.get().getAvatar();

                if (avt != null) {
                    uploadImageService.deleteImage(avt);
                }
                nguoiDungRepository.delete(nguoiDung.get());
            }
        }
        catch (Exception e){
            e.printStackTrace();
            ResponseEntity.badRequest().build();
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
