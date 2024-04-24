package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.*;
import com.example.web_ban_sach.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class DonHangServiceImpl implements DonHangService{
private final ObjectMapper objectMapper;
@Autowired
private DonHangRepository donHangRepository;
@Autowired
private ChiTietDonHangRepository chiTietDonHangRepository;
@Autowired
private NguoiDungRepository nguoiDungRepository;
@Autowired
private GioHangRepository gioHangRepository;
@Autowired
private SachRepository sachRepository;
@Autowired
private HinhThucThanhToanRepository hinhThucThanhToanRepository;

public DonHangServiceImpl(ObjectMapper objectMapper){
    this.objectMapper  = objectMapper;
}
    private String formatStringByJson(String json) {
        return json.replaceAll("\"", "");
    }
    @Override
    @Transactional
    public ResponseEntity<?> save(JsonNode jsonNode) {
try {
    DonHang donHangData = objectMapper.treeToValue(jsonNode, DonHang.class);

    donHangData.setNgayTao(Date.valueOf(LocalDate.now()));
    donHangData.setTinhTrangDonHang("Đang xử lý");

    int maNguoiDung = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maNguoiDung"))));
    NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);
    donHangData.setNguoiDung(nguoiDung);

    int maHinhThucThanhToan = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maHinhThucThanhToan"))));
    HinhThucThanhToan hinhThucThanhToan = hinhThucThanhToanRepository.findByMaHinhThucThanhToan(maHinhThucThanhToan);
    donHangData.setHinhThucThanhToan(hinhThucThanhToan);
donHangData.setChiPhiThanhToan(hinhThucThanhToan.getChiPhiThanhToan());
    donHangData.setTongTien(donHangData.getTongTien()+hinhThucThanhToan.getChiPhiThanhToan());
    DonHang newDonHang = donHangRepository.save(donHangData);
    JsonNode jsonNode1 = jsonNode.get("sach");
    for (JsonNode node: jsonNode1){
        int soLuong = Integer.parseInt(formatStringByJson(String.valueOf(node.get("soLuong"))));
        Sach sachResponse = objectMapper.treeToValue(node.get("sach"), Sach.class);
        Sach sach = sachRepository.findByMaSach(sachResponse.getMaSach());
        sach.setSoLuong(sach.getSoLuong()-soLuong);
        sach.setSoLuongDaBan(sach.getSoLuongDaBan()+soLuong);

        ChiTietDonHang chiTietDonHang = new ChiTietDonHang();
        chiTietDonHang.setSach(sach);
        chiTietDonHang.setSoLuong(soLuong);
        chiTietDonHang.setDonHang(newDonHang);
        chiTietDonHang.setGiaBan(soLuong*sach.getGiaBan());
        chiTietDonHangRepository.save(chiTietDonHang);
        sachRepository.save(sach);
    }
    gioHangRepository.deleteGioHangByNguoiDung(nguoiDung);
}catch (Exception e){
    e.printStackTrace();
    return ResponseEntity.badRequest().build();

}
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> update(JsonNode jsonNode){
    try {
        int maDonHang = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maDonHang"))));

        DonHang donHang = donHangRepository.findByMaDonHang(maDonHang);
        String tinhTrangDonHang = formatStringByJson(String.valueOf((jsonNode.get("tinhTrangDonHang"))));
        donHang.setTinhTrangDonHang(tinhTrangDonHang);

        List<ChiTietDonHang> chiTietDonHang = chiTietDonHangRepository.findChiTietDonHangByDonHang(donHang);
        for(ChiTietDonHang c: chiTietDonHang){
            Sach sach = c.getSach();
            sach.setSoLuongDaBan(sach.getSoLuongDaBan()-c.getSoLuong());
            sach.setSoLuong(sach.getSoLuong()+c.getSoLuong());
            sachRepository.save(sach);
        }
        donHangRepository.save(donHang);

    }catch (Exception e){
        e.printStackTrace();
        ResponseEntity.badRequest().build();
    }

    return ResponseEntity.ok().build();
    }
}
