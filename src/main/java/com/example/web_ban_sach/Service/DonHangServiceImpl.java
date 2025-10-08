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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
@Autowired
private SellerBooksRepository sellerBooksRepository;
@Autowired
private SellerOrdersRepository sellerOrdersRepository;

public DonHangServiceImpl(ObjectMapper objectMapper){
    this.objectMapper  = objectMapper;
}
    private String formatStringByJson(String json) {
        return json.replaceAll("\"", "");
    }
    @Override
    @Transactional
//    public ResponseEntity<?> save(JsonNode jsonNode) {
//try {
//    DonHang donHangData = objectMapper.treeToValue(jsonNode, DonHang.class);
//
//    donHangData.setNgayTao(Date.valueOf(LocalDate.now()));
//    donHangData.setTinhTrangDonHang("Đang xử lý");
//
//    int maNguoiDung = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maNguoiDung"))));
//    NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);
//    donHangData.setNguoiDung(nguoiDung);
//
//    int maHinhThucThanhToan = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maHinhThucThanhToan"))));
//    HinhThucThanhToan hinhThucThanhToan = hinhThucThanhToanRepository.findByMaHinhThucThanhToan(maHinhThucThanhToan);
//    donHangData.setHinhThucThanhToan(hinhThucThanhToan);
//donHangData.setChiPhiThanhToan(hinhThucThanhToan.getChiPhiThanhToan());
//    donHangData.setTongTien(donHangData.getTongTien()+hinhThucThanhToan.getChiPhiThanhToan());
//    DonHang newDonHang = donHangRepository.save(donHangData);
//    JsonNode jsonNode1 = jsonNode.get("sach");
//    for (JsonNode node: jsonNode1){
//        int soLuong = Integer.parseInt(formatStringByJson(String.valueOf(node.get("soLuong"))));
//        Sach sachResponse = objectMapper.treeToValue(node.get("sach"), Sach.class);
//        Sach sach = sachRepository.findByMaSach(sachResponse.getMaSach());
//        sach.setSoLuong(sach.getSoLuong()-soLuong);
//        sach.setSoLuongDaBan(sach.getSoLuongDaBan()+soLuong);
//
//        ChiTietDonHang chiTietDonHang = new ChiTietDonHang();
//        chiTietDonHang.setSach(sach);
//        chiTietDonHang.setSoLuong(soLuong);
//        chiTietDonHang.setDonHang(newDonHang);
//        chiTietDonHang.setGiaBan(soLuong*sach.getGiaBan());
//        chiTietDonHangRepository.save(chiTietDonHang);
//        sachRepository.save(sach);
//    }
//    gioHangRepository.deleteGioHangByNguoiDung(nguoiDung);
//}catch (Exception e){
//    e.printStackTrace();
//    return ResponseEntity.badRequest().build();
//
//}
//        return ResponseEntity.ok().build();
//    }
    public ResponseEntity<?> save(JsonNode jsonNode) {
        try {
            DonHang donHangData = objectMapper.treeToValue(jsonNode, DonHang.class);

            donHangData.setNgayTao(Date.valueOf(LocalDate.now()));
            donHangData.setTinhTrangDonHang("Đang xử lý");

            JsonNode maNguoiDungNode = jsonNode.get("maNguoiDung");
            if (maNguoiDungNode == null) {
                return ResponseEntity.badRequest().body("maNguoiDung is required");
            }
            int maNguoiDung = Integer.parseInt(formatStringByJson(maNguoiDungNode.toString()));
            NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);
            if (nguoiDung == null) {
                return ResponseEntity.badRequest().body("NguoiDung with maNguoiDung " + maNguoiDung + " not found");
            }
            donHangData.setNguoiDung(nguoiDung);

            JsonNode maHinhThucThanhToanNode = jsonNode.get("maHinhThucThanhToan");
            if (maHinhThucThanhToanNode == null) {
                return ResponseEntity.badRequest().body("maHinhThucThanhToan is required");
            }
            int maHinhThucThanhToan = Integer.parseInt(formatStringByJson(maHinhThucThanhToanNode.toString()));
            HinhThucThanhToan hinhThucThanhToan = hinhThucThanhToanRepository.findByMaHinhThucThanhToan(maHinhThucThanhToan);
            if (hinhThucThanhToan == null) {
                return ResponseEntity.badRequest().body("HinhThucThanhToan with maHinhThucThanhToan " + maHinhThucThanhToan + " not found");
            }
            donHangData.setHinhThucThanhToan(hinhThucThanhToan);
            donHangData.setChiPhiThanhToan(hinhThucThanhToan.getChiPhiThanhToan());
            donHangData.setTongTien(donHangData.getTongTien() + hinhThucThanhToan.getChiPhiThanhToan());

            DonHang newDonHang = donHangRepository.save(donHangData);

            JsonNode sachNode = jsonNode.get("sach");
            if (sachNode == null || !sachNode.isArray()) {
                return ResponseEntity.badRequest().body("sach must be a non-empty array");
            }
            for (JsonNode node : sachNode) {
                int soLuong = Integer.parseInt(formatStringByJson(node.get("soLuong").toString()));
                Sach sachResponse = objectMapper.treeToValue(node.get("sach"), Sach.class);
                Sach sach = sachRepository.findByMaSach(sachResponse.getMaSach());
                if (sach == null) {
                    return ResponseEntity.badRequest().body("Sach with maSach " + sachResponse.getMaSach() + " not found");
                }
                if (sach.getSoLuong() < soLuong) {
                    return ResponseEntity.badRequest().body("Not enough stock for sach " + sach.getMaSach());
                }
                sach.setSoLuong(sach.getSoLuong() - soLuong);
                sach.setSoLuongDaBan(sach.getSoLuongDaBan() + soLuong);

                ChiTietDonHang chiTietDonHang = new ChiTietDonHang();
                chiTietDonHang.setSach(sach);
                chiTietDonHang.setSoLuong(soLuong);
                chiTietDonHang.setDonHang(newDonHang);
                chiTietDonHang.setGiaBan(soLuong * sach.getGiaBan());
                chiTietDonHangRepository.save(chiTietDonHang);
                sachRepository.save(sach);

                // Tạo bản ghi SellerOrders để cập nhật doanh thu cho shop
                Optional<SellerBooks> sellerBookOpt = sellerBooksRepository.findFirstBySachMaSachAndActive(sach.getMaSach());
                if (sellerBookOpt.isPresent()) {
                    SellerBooks sellerBook = sellerBookOpt.get();
                    SellerOrders sellerOrder = new SellerOrders();
                    sellerOrder.setMaDonHang(newDonHang.getMaDonHang());
                    sellerOrder.setSeller(sellerBook.getSeller());
                    sellerOrder.setSach(sach);
                    sellerOrder.setSoLuong(soLuong);
                    sellerOrder.setGiaBan(sach.getGiaBan());
                    sellerOrder.setTongTien(soLuong * sach.getGiaBan());
                    sellerOrder.setNgayDat(new Timestamp(System.currentTimeMillis()));
                    sellerOrder.setTrangThai("Đang xử lý");
                    sellerOrdersRepository.save(sellerOrder);
                }
            }

            gioHangRepository.deleteGioHangByNguoiDung(nguoiDung);

            Map<String, Object> response = new HashMap<>();
            response.put("maDonHang", newDonHang.getMaDonHang());
            response.put("tinhTrangDonHang", newDonHang.getTinhTrangDonHang());
            response.put("tongTien", newDonHang.getTongTien());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi khi tạo đơn hàng: " + e.getMessage());
        }
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
