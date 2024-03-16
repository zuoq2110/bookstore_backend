package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.GioHangRepository;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.entity.GioHang;
import com.example.web_ban_sach.entity.NguoiDung;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GioHangServiceImpl implements GioHangService{
    private final ObjectMapper objectMapper;
    @Autowired
    private GioHangRepository gioHangRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public GioHangServiceImpl(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    private String formatStringByJson(String json){
        return json.replaceAll("\"","");
    }
    @Override
    public ResponseEntity<?> save(JsonNode jsonData) {
        try{
            int idUser = 0;
            // Danh sách item của data vừa truyền
            List<GioHang> GioHangDataList = new ArrayList<>();
            for (JsonNode jsonDatum : jsonData) {
                GioHang GioHangData = objectMapper.treeToValue(jsonDatum, GioHang.class);
                idUser = Integer.parseInt(formatStringByJson(String.valueOf(jsonDatum.get("maNguoiDung"))));
                GioHangDataList.add(GioHangData);
            }
            Optional<NguoiDung> user = nguoiDungRepository.findById(idUser);
            // Danh sách item của user
            List<GioHang> GioHangList = user.get().getDanhSachGioHang();

            // Lặp qua từng item và xử lý
            for (GioHang GioHangData : GioHangDataList) {
                boolean isHad = false;
                for (GioHang GioHang : GioHangList) {
                    // Nếu trong cart của user có item đó rồi thì sẽ update lại quantity
                    if (GioHang.getSach().getMaSach() == GioHangData.getSach().getMaSach()) {
                        GioHang.setSoLuong(GioHang.getSoLuong() + GioHangData.getSoLuong());
                        isHad = true;
                        break;
                    }
                }
                // Nếu chưa có thì thêm mới item đó
                if (!isHad) {
                    GioHang GioHang = new GioHang();
                    GioHang.setNguoiDung(user.get());
                    GioHang.setSoLuong(GioHangData.getSoLuong());
                    GioHang.setSach(GioHangData.getSach());
                    GioHangList.add(GioHang);
                }
            }
            user.get().setDanhSachGioHang(GioHangList);
            NguoiDung newUser = nguoiDungRepository.save(user.get());


            if (GioHangDataList.size() == 1) {
                List<GioHang> GioHangListTemp = newUser.getDanhSachGioHang();
                return ResponseEntity.ok(GioHangListTemp.get(GioHangList.size() - 1).getMaGioHang());
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<?> update(JsonNode jsonNode) {
        try {
            int maGioHang = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maGioHang"))));
            int soLuong = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("soLuong"))));
            GioHang gioHang = gioHangRepository.findByMaGioHang(maGioHang);
            gioHang.setSoLuong(soLuong);
            gioHangRepository.save(gioHang);
            return ResponseEntity.ok().build();
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}
