package com.example.web_ban_sach.controller;

import com.example.web_ban_sach.Service.DonHangService;
import com.example.web_ban_sach.dao.DonHangRepository;
import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.SellerBooksRepository;
import com.example.web_ban_sach.entity.DonHang;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.SellerBooks;
import com.example.web_ban_sach.entity.SellerOrderResponse;
import com.example.web_ban_sach.entity.ThongBao;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/don-hang")
@CrossOrigin(origins = "http://localhost:3000")
public class DonHangController {
    @Autowired
    private DonHangService donHangService;
    
    @Autowired
    private DonHangRepository donHangRepository;
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private SellerBooksRepository sellerBooksRepository;

    @PostMapping("/them")
    public ResponseEntity<?> save(@RequestBody JsonNode jsonNode){
        try {
             donHangService.save(jsonNode);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/cap-nhat")
    public ResponseEntity<?> update(@RequestBody JsonNode jsonNode){
        try {
            donHangService.update(jsonNode);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
    
    /**
     * Lấy danh sách đơn hàng của người dùng với phân trang
     * 
     * @param maNguoiDung ID của người dùng
     * @param page Trang hiện tại (bắt đầu từ 0)
     * @param size Số lượng items mỗi trang (mặc định 10)
     * @param sortBy Trường để sắp xếp (mặc định: ngayTao)
     * @param sortDir Hướng sắp xếp: asc hoặc desc (mặc định: desc)
     * @return Page<DonHang> với thông tin phân trang
     * 
     * Example: GET /don-hang/nguoi-dung/1?page=0&size=10&sortBy=ngayTao&sortDir=desc
     */
    @Transactional
    @GetMapping("/nguoi-dung/{maNguoiDung}")
    public ResponseEntity<Page<DonHang>> getDonHangByNguoiDung(
            @PathVariable int maNguoiDung,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<DonHang> donHangPage = donHangRepository.findByNguoiDungMaNguoiDung(maNguoiDung, pageable);
            
            // Force lazy loading trước khi return
            donHangPage.getContent().forEach(donHang -> {
                donHang.getDanhSachChiTietDonHang().forEach(chiTiet -> {
                    if (chiTiet.getSach() != null) {
                        chiTiet.getSach().getDanhSachHinhAnh().size(); // Trigger lazy load
                        chiTiet.getSach().getDanhSachTheLoai().size(); // Trigger lazy load
                    }
                });
            });
            
            return ResponseEntity.ok(donHangPage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Đếm tổng số đơn hàng của người dùng
     * 
     * @param maNguoiDung ID của người dùng
     * @return Số lượng đơn hàng
     * 
     * Example: GET /don-hang/nguoi-dung/1/count
     */
    @GetMapping("/nguoi-dung/{maNguoiDung}/count")
    public ResponseEntity<Long> countDonHangByNguoiDung(@PathVariable int maNguoiDung) {
        try {
            long count = donHangRepository.countByNguoiDungMaNguoiDung(maNguoiDung);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Lấy danh sách đơn hàng có chứa sách của seller
     * API này chỉ trả về các đơn hàng có ít nhất 1 sản phẩm thuộc seller
     * Mỗi đơn hàng sẽ chỉ chứa sellerItems - các sản phẩm của seller đó
     * 
     * @param sellerId ID của seller
     * @param page Trang hiện tại (bắt đầu từ 0)
     * @param size Số lượng items mỗi trang (mặc định 10)
     * @param sortBy Trường để sắp xếp (mặc định: ngayTao)
     * @param sortDir Hướng sắp xếp: asc hoặc desc (mặc định: desc)
     * @return Page<SellerOrderResponse> với thông tin phân trang
     * 
     * Example: GET /don-hang/seller/50?page=0&size=10&sortBy=ngayTao&sortDir=desc
     */
    @Transactional
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getOrdersBySeller(
            @PathVariable int sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        try {
            // TODO: Authorization check - Chỉ seller có thể xem đơn hàng của mình
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            NguoiDung currentUser = nguoiDungRepository.findByTenDangNhap(username);
            
            if (currentUser == null) {
                return ResponseEntity.badRequest()
                    .body(new ThongBao("Người dùng không tồn tại"));
            }
            
            // Check authorization: Chỉ seller có thể xem đơn hàng của mình
            // hoặc admin có thể xem tất cả
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
            
            if (!isAdmin && currentUser.getMaNguoiDung() != sellerId) {
                return ResponseEntity.status(403)
                    .body(new ThongBao("Bạn không có quyền truy cập đơn hàng này"));
            }
            
            // Tạo pageable với sorting
            Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Lấy đơn hàng của seller
            Page<DonHang> donHangPage = donHangRepository.findOrdersBySeller(sellerId, pageable);
            
            // Convert sang SellerOrderResponse và filter items
            List<SellerOrderResponse> sellerOrders = donHangPage.getContent().stream()
                .map(donHang -> convertToSellerOrderResponse(donHang, sellerId))
                .collect(Collectors.toList());
            
            // Tạo Page response
            Page<SellerOrderResponse> responsePage = new PageImpl<>(
                sellerOrders, 
                pageable, 
                donHangPage.getTotalElements()
            );
            
            return ResponseEntity.ok(responsePage);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(new ThongBao("Lỗi: " + e.getMessage()));
        }
    }
    
    /**
     * Đếm số đơn hàng của seller
     * 
     * @param sellerId ID của seller
     * @return Số lượng đơn hàng
     * 
     * Example: GET /don-hang/seller/50/count
     */
    @GetMapping("/seller/{sellerId}/count")
    public ResponseEntity<?> countOrdersBySeller(@PathVariable int sellerId) {
        try {
            // TODO: Authorization check tương tự như trên
            long count = donHangRepository.countOrdersBySeller(sellerId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Helper method: Convert DonHang sang SellerOrderResponse
     * Chỉ bao gồm items của seller
     */
    private SellerOrderResponse convertToSellerOrderResponse(DonHang donHang, int sellerId) {
        SellerOrderResponse response = new SellerOrderResponse();
        
        // Copy thông tin đơn hàng
        response.setMaDonHang(donHang.getMaDonHang());
        response.setNgayTao(donHang.getNgayTao());
        response.setDiaChiGiaoHang(donHang.getDiaChiGiaoHang());
        response.setTinhTrangDonHang(donHang.getTinhTrangDonHang());
        response.setMaVanDon(donHang.getMaVanDon());
        response.setDonViVanChuyen(donHang.getDonViVanChuyen());
        response.setSdtVanChuyen(donHang.getSdtVanChuyen());
        response.setLinkTracking(donHang.getLinkTracking());
        response.setThoiGianGiaoDuKien(donHang.getThoiGianGiaoDuKien());
        response.setGhiChu(donHang.getGhiChu());
        response.setTongTienSanPham(donHang.getTongTienSanPham());
        response.setChiPhiGiaoHang(donHang.getChiPhiGiaoHang());
        response.setChiPhiThanhToan(donHang.getChiPhiThanhToan());
        response.setTongTien(donHang.getTongTien());
        response.setSoDienThoai(donHang.getSoDienThoai());
        response.setHoVaTen(donHang.getHoVaTen());
        
        // Lấy danh sách mã sách của seller để filter
        List<SellerBooks> sellerBooksList = sellerBooksRepository.findBySellerMaNguoiDungWithSach(sellerId);
        Set<Integer> sellerBookIds = sellerBooksList.stream()
            .map(sb -> sb.getSach().getMaSach())
            .collect(Collectors.toSet());
        
        // Filter và map seller items
        List<SellerOrderResponse.SellerItemInfo> sellerItems = donHang.getDanhSachChiTietDonHang()
            .stream()
            .filter(chiTiet -> sellerBookIds.contains(chiTiet.getSach().getMaSach()))
            .map(chiTiet -> {
                SellerOrderResponse.SellerItemInfo item = new SellerOrderResponse.SellerItemInfo();
                item.setChiTietDonHang((int) chiTiet.getChiTietDonHang());
                item.setSoLuong(chiTiet.getSoLuong());
                item.setGiaBan(chiTiet.getGiaBan());
                
                // Map sách info
                SellerOrderResponse.SachInfo sachInfo = new SellerOrderResponse.SachInfo();
                sachInfo.setMaSach(chiTiet.getSach().getMaSach());
                sachInfo.setTenSach(chiTiet.getSach().getTenSach());
                sachInfo.setGiaBan(chiTiet.getSach().getGiaBan());
                sachInfo.setTenTacGia(chiTiet.getSach().getTenTacGia());
                
                // Lấy thumbnail (ảnh đầu tiên)
                if (chiTiet.getSach().getDanhSachHinhAnh() != null && 
                    !chiTiet.getSach().getDanhSachHinhAnh().isEmpty()) {
                    sachInfo.setThumbnail(chiTiet.getSach().getDanhSachHinhAnh().get(0).getTenHinhAnh());
                }
                
                item.setSach(sachInfo);
                return item;
            })
            .collect(Collectors.toList());
        
        response.setSellerItems(sellerItems);
        
        // Map người dùng info
        if (donHang.getNguoiDung() != null) {
            SellerOrderResponse.NguoiDungInfo nguoiDungInfo = new SellerOrderResponse.NguoiDungInfo();
            nguoiDungInfo.setMaNguoiDung(donHang.getNguoiDung().getMaNguoiDung());
            nguoiDungInfo.setEmail(donHang.getNguoiDung().getEmail());
            nguoiDungInfo.setTenDangNhap(donHang.getNguoiDung().getTenDangNhap());
            response.setNguoiDung(nguoiDungInfo);
        }
        
        // Map hình thức thanh toán
        if (donHang.getHinhThucThanhToan() != null) {
            SellerOrderResponse.HinhThucThanhToanInfo ttInfo = new SellerOrderResponse.HinhThucThanhToanInfo();
            ttInfo.setMaHinhThucThanhToan(donHang.getHinhThucThanhToan().getMaHinhThucThanhToan());
            ttInfo.setTenHinhThucThanhToan(donHang.getHinhThucThanhToan().getTenHinhThucThanhToan());
            response.setHinhThucThanhToan(ttInfo);
        }
        
        // Map hình thức giao hàng
        if (donHang.getHinhThucGiaoHang() != null) {
            SellerOrderResponse.HinhThucGiaoHangInfo ghInfo = new SellerOrderResponse.HinhThucGiaoHangInfo();
            ghInfo.setMaHinhThucGiaoHang(donHang.getHinhThucGiaoHang().getMaHinhThucGiaoHang());
            ghInfo.setTenHinhThucGiaoHang(donHang.getHinhThucGiaoHang().getTenHinhThucGiaoHang());
            response.setHinhThucGiaoHang(ghInfo);
        }
        
        return response;
    }
}
