package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.*;
import com.example.web_ban_sach.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class SuDanhGiaServiceImpl implements SuDanhGiaService {
@Autowired
    private SuDanhGiaRepository suDanhGiaRepository;
@Autowired
private NguoiDungRepository nguoiDungRepository;
@Autowired
private ChiTietDonHangRepository chiTietDonHangRepository;
@Autowired
private SachRepository sachRepository;
@Autowired
private DonHangRepository donHangRepository;
private final ObjectMapper objectMapper;
public SuDanhGiaServiceImpl(ObjectMapper objectMapper){
    this.objectMapper = objectMapper;
}
    private String formatStringByJson(String json){
        return json.replaceAll("\"","");
    }
    @Override
    public ResponseEntity<?> save(JsonNode jsonNode) {
    try {
        int maNguoiDung = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maNguoiDung"))));
        int maDonHang = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maDonHang"))));
        int maSach = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maSach"))));
        float diemXepHang = Float.parseFloat(formatStringByJson(String.valueOf(jsonNode.get("diemXepHang"))));
        String nhanXet = formatStringByJson(String.valueOf(jsonNode.get("nhanXet")));
        Sach sach = sachRepository.findByMaSach(maSach);

        NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);
        DonHang donHang = donHangRepository.findByMaDonHang(maDonHang);
        List<ChiTietDonHang> chiTietDonHangList = chiTietDonHangRepository.findChiTietDonHangByDonHang(donHang);
        for (ChiTietDonHang c : chiTietDonHangList) {
            if (c.getSach().getMaSach() == maSach) {
                c.setReview(true);
                SuDanhGia suDanhGia = new SuDanhGia();
                suDanhGia.setSach(sach);
                suDanhGia.setNguoiDung(nguoiDung);
                suDanhGia.setDiemXepHang(diemXepHang);
                suDanhGia.setNhanXet(nhanXet);
                suDanhGia.setChiTietDonHang(c);
                // Lấy thời gian hiện tại
                Instant instant = Instant.now();
                // Chuyển đổi thành timestamp
                Timestamp timestamp = Timestamp.from(instant);
                suDanhGia.setTimestamp(timestamp);
                chiTietDonHangRepository.save(c);
                suDanhGiaRepository.save(suDanhGia);
                break;
            }
        }
        List<SuDanhGia> suDanhGiaList = suDanhGiaRepository.findAll();
        int n = 0;
        double sum = 0;
        for (SuDanhGia suDanhGia : suDanhGiaList) {
            if (suDanhGia.getSach().getMaSach() == maSach) {
                n++;
                sum = sum + suDanhGia.getDiemXepHang();
            }
        }
        double trungBinhDiemDanhGia = sum / n;
        sach.setTrungBinhXepHang(trungBinhDiemDanhGia);
        sachRepository.save(sach);
    }catch (Exception e){
        e.printStackTrace();
        return ResponseEntity.badRequest().build();
    }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<?> update(JsonNode jsonNode) {
        try {
            SuDanhGia suDanhGiaResponse = objectMapper.treeToValue(jsonNode, SuDanhGia.class);
            SuDanhGia suDanhGia = suDanhGiaRepository.findByMaDanhGia(suDanhGiaResponse.getMaDanhGia());
            suDanhGia.setDiemXepHang(suDanhGiaResponse.getDiemXepHang());
            suDanhGia.setNhanXet(suDanhGiaResponse.getNhanXet());
            suDanhGiaRepository.save(suDanhGia);
        }catch (Exception e){
            e.printStackTrace();
            ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<?> get(JsonNode jsonNode) {
        try {
            int maDonHang = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maDonHang"))));
            int maSach = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maSach"))));

            DonHang donHang = donHangRepository.findByMaDonHang(maDonHang);
            Sach sach = sachRepository.findByMaSach(maSach);
            List<ChiTietDonHang> chiTietDonHangList = chiTietDonHangRepository.findChiTietDonHangByDonHang(donHang);
            for(ChiTietDonHang c : chiTietDonHangList){
                if(c.getSach().getMaSach()== maSach){
                    SuDanhGia suDanhGia = suDanhGiaRepository.findByChiTietDonHang(c);
                    SuDanhGia suDanhGiaResponse = new SuDanhGia();
                    suDanhGiaResponse.setMaDanhGia(suDanhGia.getMaDanhGia());
                    suDanhGiaResponse.setTimestamp(suDanhGia.getTimestamp());
                    suDanhGiaResponse.setNhanXet(suDanhGia.getNhanXet());
                    suDanhGiaResponse.setDiemXepHang(suDanhGia.getDiemXepHang());
                    return ResponseEntity.status(HttpStatus.OK).body(suDanhGiaResponse);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return null;
    }
}
