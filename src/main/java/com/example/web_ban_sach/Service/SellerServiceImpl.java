package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.*;
import com.example.web_ban_sach.dto.SellerBookResponse;
import com.example.web_ban_sach.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SellerServiceImpl implements SellerService {
    
    @Autowired
    private NguoiDungRepository nguoiDungRepository;
    
    @Autowired
    private SellerBooksRepository sellerBooksRepository;
    
    @Autowired
    private SellerOrdersRepository sellerOrdersRepository;
    
    @Autowired
    private SachRepository sachRepository;
    
    @Autowired
    private SuDanhGiaRepository suDanhGiaRepository;
    
    @Autowired
    private HinhAnhRepository hinhAnhRepository;
    
    @Autowired
    private UploadImageService uploadImageService;
    
    @Override
    public ResponseEntity<?> registerSeller(SellerRegisterRequest request) {
        try {
            // Validation
            if (request.getTenGianHang() == null || request.getTenGianHang().trim().length() < 3) {
                return ResponseEntity.badRequest().body(new ThongBao("Tên gian hàng phải có ít nhất 3 ký tự"));
            }
            
            if (request.getMoTaGianHang() == null || request.getMoTaGianHang().trim().length() < 20) {
                return ResponseEntity.badRequest().body(new ThongBao("Mô tả gian hàng phải có ít nhất 20 ký tự"));
            }
            
            // Tìm user
            Optional<NguoiDung> nguoiDungOpt = nguoiDungRepository.findById(request.getMaNguoiDung());
            if (!nguoiDungOpt.isPresent()) {
                return ResponseEntity.badRequest().body(new ThongBao("Không tìm thấy người dùng"));
            }
            
            NguoiDung nguoiDung = nguoiDungOpt.get();
            
            // Kiểm tra đã là seller chưa
            if (nguoiDung.isSeller()) {
                return ResponseEntity.badRequest().body(new ThongBao("Người dùng đã là seller"));
            }
            
            // Cập nhật thông tin seller
            nguoiDung.setSeller(true);
            nguoiDung.setTenGianHang(request.getTenGianHang());
            nguoiDung.setMoTaGianHang(request.getMoTaGianHang());
            
            nguoiDungRepository.save(nguoiDung);
            
            return ResponseEntity.ok(new ThongBao("Đăng ký seller thành công! Vui lòng đăng nhập lại để cập nhật quyền."));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThongBao("Lỗi khi đăng ký seller: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> getDashboard(int sellerId) {
        try {
            // Kiểm tra seller tồn tại
            Optional<NguoiDung> sellerOpt = nguoiDungRepository.findById(sellerId);
            if (!sellerOpt.isPresent() || !sellerOpt.get().isSeller()) {
                return ResponseEntity.badRequest().body(new ThongBao("Seller không tồn tại"));
            }
            
            // Lấy thống kê
            long totalBooks = sellerBooksRepository.countBySellerMaNguoiDung(sellerId);
            long totalOrders = sellerOrdersRepository.countBySellerMaNguoiDung(sellerId);
            Double totalRevenue = sellerOrdersRepository.sumTongTienBySellerMaNguoiDung(sellerId);
            if (totalRevenue == null) totalRevenue = 0.0;
            
            // Tính trung bình rating từ đánh giá của khách hàng
            Double averageRating = suDanhGiaRepository.getAverageRatingBySeller(sellerId);
            if (averageRating == null) averageRating = 0.0;
            
            SellerDashboardResponse response = new SellerDashboardResponse(
                    (int) totalBooks,
                    totalOrders,
                    totalRevenue,
                    averageRating
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThongBao("Lỗi khi lấy dashboard: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> getBooks(int sellerId, int page, int limit, String status) {
        try {
            List<SellerBooks> allBooks;
            if (status != null && !status.isEmpty()) {
                allBooks = sellerBooksRepository.findBySellerMaNguoiDungAndTrangThaiWithSach(sellerId, status);
            } else {
                allBooks = sellerBooksRepository.findBySellerMaNguoiDungWithSach(sellerId);
            }
            
            // Check if empty
            if (allBooks.isEmpty()) {
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("content", new ArrayList<>());
                emptyResponse.put("totalElements", 0);
                emptyResponse.put("totalPages", 0);
                emptyResponse.put("currentPage", page);
                emptyResponse.put("pageSize", limit);
                return ResponseEntity.ok(emptyResponse);
            }
            
            // Convert to DTO to avoid lazy loading issues
            List<SellerBookResponse> allBooksDTO = allBooks.stream()
                    .map(SellerBookResponse::new)
                    .collect(java.util.stream.Collectors.toList());
            
            // Manual pagination with bounds check
            int start = (page - 1) * limit;
            if (start >= allBooksDTO.size()) {
                start = 0; // Reset to first page if out of bounds
            }
            int end = Math.min(start + limit, allBooksDTO.size());
            
            List<SellerBookResponse> pagedBooks = allBooksDTO.subList(start, end);
            
            // Create response with pagination info
            Map<String, Object> response = new HashMap<>();
            response.put("content", pagedBooks);
            response.put("totalElements", allBooksDTO.size());
            response.put("totalPages", (int) Math.ceil((double) allBooksDTO.size() / limit));
            response.put("currentPage", page);
            response.put("pageSize", limit);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThongBao("Lỗi khi lấy danh sách sách: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> addBook(int sellerId, AddBookWithImageRequest request) {
        try {
            // Validate request wrapper
            if (request == null || request.getBookData() == null) {
                return ResponseEntity.badRequest().body(new ThongBao("Dữ liệu sách không được để trống"));
            }
            
            AddBookRequest bookData = request.getBookData();
            
            // Validation
            if (bookData.getTenSach() == null || bookData.getTenSach().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ThongBao("Tên sách không được để trống"));
            }
            if (bookData.getTacGia() == null || bookData.getTacGia().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ThongBao("Tác giả không được để trống"));
            }
            if (bookData.getMoTa() == null || bookData.getMoTa().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ThongBao("Mô tả không được để trống"));
            }
            if (bookData.getGiaBan() <= 0) {
                return ResponseEntity.badRequest().body(new ThongBao("Giá bán phải lớn hơn 0"));
            }
            if (bookData.getGiaNhap() <= 0) {
                return ResponseEntity.badRequest().body(new ThongBao("Giá nhập phải lớn hơn 0"));
            }
            if (bookData.getSoLuong() < 0) {
                return ResponseEntity.badRequest().body(new ThongBao("Số lượng không được âm"));
            }
            
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (bookData.getNamXuatBan() < 1900 || bookData.getNamXuatBan() > currentYear) {
                return ResponseEntity.badRequest().body(new ThongBao("Năm xuất bản không hợp lệ"));
            }
            
            // Kiểm tra seller tồn tại
            Optional<NguoiDung> sellerOpt = nguoiDungRepository.findById(sellerId);
            if (!sellerOpt.isPresent() || !sellerOpt.get().isSeller()) {
                return ResponseEntity.badRequest().body(new ThongBao("Seller không tồn tại"));
            }
            
            // Tạo sách mới
            Sach sach = new Sach();
            sach.setTenSach(bookData.getTenSach());
            sach.setTenTacGia(bookData.getTacGia());
            
            // Tạo mô tả đầy đủ bao gồm thông tin NXB và năm xuất bản
            String moTaFull = bookData.getMoTa();
            if (bookData.getNhaXuatBan() != null && !bookData.getNhaXuatBan().isEmpty()) {
                moTaFull += "\n\nNhà xuất bản: " + bookData.getNhaXuatBan();
            }
            if (bookData.getNamXuatBan() > 0) {
                moTaFull += "\nNăm xuất bản: " + bookData.getNamXuatBan();
            }
            sach.setMoTa(moTaFull);
            
            sach.setGiaBan(bookData.getGiaBan());
            sach.setSoLuong(bookData.getSoLuong());
            sach.setGiaNiemYet(bookData.getGiaBan()); // Đặt giá niêm yết = giá bán
            
            Sach savedSach = sachRepository.save(sach);
            
            // Upload cover image nếu có
            if (request.getCoverImage() != null && !request.getCoverImage().trim().isEmpty()) {
                try {
                    // Convert base64 to MultipartFile
                    MultipartFile coverImageFile = new com.example.web_ban_sach.util.Base64MultipartFile(request.getCoverImage());
                    
                    // Upload lên Cloudinary với tên unique
                    String imageName = "Book_Cover_" + savedSach.getMaSach() + "_" + System.currentTimeMillis();
                    String imageUrl = uploadImageService.uploadImage(coverImageFile, imageName);
                    
                    // Kiểm tra upload thành công
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // Tạo bản ghi HinhAnh
                        HinhAnh hinhAnh = new HinhAnh();
                        hinhAnh.setTenHinhAnh("Cover_Book_" + savedSach.getMaSach());
                        hinhAnh.setLaIcon(true); // Ảnh bìa là icon (thumbnail)
                        hinhAnh.setDuongDan(imageUrl);
                        hinhAnh.setSach(savedSach);
                        
                        hinhAnhRepository.save(hinhAnh);
                        
                        System.out.println("✅ Upload ảnh bìa thành công: " + imageUrl);
                    } else {
                        System.err.println("⚠️ Upload ảnh trả về URL rỗng");
                    }
                    
                } catch (Exception e) {
                    // Log lỗi nhưng vẫn tiếp tục (sách đã được tạo)
                    System.err.println("❌ Lỗi khi upload ảnh bìa: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Tạo seller_books record
            SellerBooks sellerBook = new SellerBooks();
            sellerBook.setSach(savedSach);
            sellerBook.setSeller(sellerOpt.get());
            sellerBook.setGiaNhap(bookData.getGiaNhap());
            sellerBook.setSoLuongKho(bookData.getSoLuong());
            sellerBook.setTrangThai("active");
            
            sellerBooksRepository.save(sellerBook);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(new ThongBao("Thêm sách thành công"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThongBao("Lỗi khi thêm sách: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> addBookWithFile(int sellerId, AddBookRequest bookData, MultipartFile coverImage) {
        try {
            // Validation
            if (bookData.getTenSach() == null || bookData.getTenSach().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ThongBao("Tên sách không được để trống"));
            }
            if (bookData.getTacGia() == null || bookData.getTacGia().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ThongBao("Tác giả không được để trống"));
            }
            if (bookData.getMoTa() == null || bookData.getMoTa().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ThongBao("Mô tả không được để trống"));
            }
            if (bookData.getGiaBan() <= 0) {
                return ResponseEntity.badRequest().body(new ThongBao("Giá bán phải lớn hơn 0"));
            }
            if (bookData.getGiaNhap() <= 0) {
                return ResponseEntity.badRequest().body(new ThongBao("Giá nhập phải lớn hơn 0"));
            }
            if (bookData.getSoLuong() < 0) {
                return ResponseEntity.badRequest().body(new ThongBao("Số lượng không được âm"));
            }
            
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if (bookData.getNamXuatBan() < 1900 || bookData.getNamXuatBan() > currentYear) {
                return ResponseEntity.badRequest().body(new ThongBao("Năm xuất bản không hợp lệ"));
            }
            
            // Kiểm tra seller tồn tại
            Optional<NguoiDung> sellerOpt = nguoiDungRepository.findById(sellerId);
            if (!sellerOpt.isPresent() || !sellerOpt.get().isSeller()) {
                return ResponseEntity.badRequest().body(new ThongBao("Seller không tồn tại"));
            }
            
            // Tạo sách mới
            Sach sach = new Sach();
            sach.setTenSach(bookData.getTenSach());
            sach.setTenTacGia(bookData.getTacGia());
            
            // Tạo mô tả đầy đủ bao gồm thông tin NXB và năm xuất bản
            String moTaFull = bookData.getMoTa();
            if (bookData.getNhaXuatBan() != null && !bookData.getNhaXuatBan().isEmpty()) {
                moTaFull += "\n\nNhà xuất bản: " + bookData.getNhaXuatBan();
            }
            if (bookData.getNamXuatBan() > 0) {
                moTaFull += "\nNăm xuất bản: " + bookData.getNamXuatBan();
            }
            sach.setMoTa(moTaFull);
            
            sach.setGiaBan(bookData.getGiaBan());
            sach.setSoLuong(bookData.getSoLuong());
            sach.setGiaNiemYet(bookData.getGiaBan()); // Đặt giá niêm yết = giá bán
            
            Sach savedSach = sachRepository.save(sach);
            
            // Upload cover image nếu có (nhận MultipartFile trực tiếp)
            if (coverImage != null && !coverImage.isEmpty()) {
                try {
                    // Upload lên Cloudinary với tên unique
                    String imageName = "Book_Cover_" + savedSach.getMaSach() + "_" + System.currentTimeMillis();
                    String imageUrl = uploadImageService.uploadImage(coverImage, imageName);
                    
                    // Kiểm tra upload thành công
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // Tạo bản ghi HinhAnh
                        HinhAnh hinhAnh = new HinhAnh();
                        hinhAnh.setTenHinhAnh("Cover_Book_" + savedSach.getMaSach());
                        hinhAnh.setLaIcon(true); // Ảnh bìa là icon (thumbnail)
                        hinhAnh.setDuongDan(imageUrl);
                        hinhAnh.setSach(savedSach);
                        
                        hinhAnhRepository.save(hinhAnh);
                        
                        System.out.println("✅ Upload ảnh bìa thành công: " + imageUrl);
                    } else {
                        System.err.println("⚠️ Upload ảnh trả về URL rỗng");
                    }
                    
                } catch (Exception e) {
                    // Log lỗi nhưng vẫn tiếp tục (sách đã được tạo)
                    System.err.println("❌ Lỗi khi upload ảnh bìa: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Tạo seller_books record
            SellerBooks sellerBook = new SellerBooks();
            sellerBook.setSach(savedSach);
            sellerBook.setSeller(sellerOpt.get());
            sellerBook.setGiaNhap(bookData.getGiaNhap());
            sellerBook.setSoLuongKho(bookData.getSoLuong());
            sellerBook.setTrangThai("active");
            
            sellerBooksRepository.save(sellerBook);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(new ThongBao("Thêm sách thành công"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThongBao("Lỗi khi thêm sách: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> deleteBook(int sellerId, int bookId) {
        try {
            // Tìm sách của seller
            Optional<SellerBooks> sellerBookOpt = sellerBooksRepository.findBySellerAndId(sellerId, bookId);
            
            if (!sellerBookOpt.isPresent()) {
                return ResponseEntity.badRequest().body(new ThongBao("Không tìm thấy sách hoặc bạn không có quyền xóa"));
            }
            
            SellerBooks sellerBook = sellerBookOpt.get();
            
            // Soft delete - chỉ update status
            sellerBook.setTrangThai("deleted");
            sellerBooksRepository.save(sellerBook);
            
            return ResponseEntity.ok(new ThongBao("Xóa sách thành công"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThongBao("Lỗi khi xóa sách: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> getStatistics(int sellerId, String period) {
        try {
            // Kiểm tra seller
            Optional<NguoiDung> sellerOpt = nguoiDungRepository.findById(sellerId);
            if (!sellerOpt.isPresent() || !sellerOpt.get().isSeller()) {
                return ResponseEntity.badRequest().body(new ThongBao("Seller không tồn tại"));
            }
            
            // Tính thời gian
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate;
            int days;
            
            switch (period) {
                case "7days":
                    days = 7;
                    startDate = now.minusDays(6);  // 6 ngày trước + hôm nay = 7 ngày
                    break;
                case "30days":
                    days = 30;
                    startDate = now.minusDays(29); // 29 ngày trước + hôm nay = 30 ngày
                    break;
                case "3months":
                    days = 90;
                    startDate = now.minusDays(89); // 89 ngày trước + hôm nay = 90 ngày
                    break;
                case "1year":
                    days = 365;
                    startDate = now.minusDays(364); // 364 ngày trước + hôm nay = 365 ngày
                    break;
                default:
                    days = 30;
                    startDate = now.minusDays(29);
            }
            
            Timestamp startTimestamp = Timestamp.valueOf(startDate);
            Timestamp endTimestamp = Timestamp.valueOf(now);
            
            // Lấy thống kê
            Double totalRevenue = sellerOrdersRepository.sumTongTienBySellerAndDateRange(sellerId, startTimestamp, endTimestamp);
            if (totalRevenue == null) totalRevenue = 0.0;
            
            long totalOrders = sellerOrdersRepository.countBySellerAndDateRange(sellerId, startTimestamp, endTimestamp);
            long totalBooksSold = sellerOrdersRepository.sumTotalBooksSold(sellerId);
            long totalCustomers = sellerOrdersRepository.countUniqueCustomers(sellerId);
            
            // Revenue data theo từng ngày (dữ liệu thực tế)
            List<Object[]> revenueByDateData = sellerOrdersRepository.getRevenueByDate(sellerId, startTimestamp, endTimestamp);
            List<Double> revenueData = new ArrayList<>();
            
            // Tạo map để tra cứu doanh thu theo ngày
            Map<String, Double> revenueMap = new HashMap<>();
            for (Object[] row : revenueByDateData) {
                java.sql.Date date = (java.sql.Date) row[0];
                Double revenue = ((Number) row[1]).doubleValue();
                revenueMap.put(date.toString(), revenue);
            }
            
            // Fill dữ liệu cho tất cả các ngày trong khoảng thời gian (bao gồm cả ngày không có doanh thu)
            LocalDate currentDate = startDate.toLocalDate();
            LocalDate endLocalDate = now.toLocalDate();
            
            // Loop qua tất cả các ngày từ startDate đến hôm nay (inclusive)
            while (!currentDate.isAfter(endLocalDate)) {
                String dateStr = currentDate.toString();
                Double revenue = revenueMap.getOrDefault(dateStr, 0.0);
                revenueData.add(revenue);
                currentDate = currentDate.plusDays(1);
            }
            
            // Top selling books
            List<SellerStatisticsResponse.TopSellingBook> topBooks = new ArrayList<>();
            List<Object[]> topBooksData = sellerOrdersRepository.findTopSellingBooks(sellerId);
            
            for (Object[] data : topBooksData) {
                if (topBooks.size() >= 10) break; // Lấy top 10
                
                Integer maSach = (Integer) data[0];
                String tenSach = (String) data[1];
                Long totalSold = ((Number) data[2]).longValue();
                Double revenue = ((Number) data[3]).doubleValue();
                
                topBooks.add(new SellerStatisticsResponse.TopSellingBook(maSach, tenSach, totalSold, revenue));
            }
            
            SellerStatisticsResponse response = new SellerStatisticsResponse(
                    totalRevenue,
                    totalOrders,
                    totalBooksSold,
                    totalCustomers,
                    revenueData,
                    topBooks
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThongBao("Lỗi khi lấy thống kê: " + e.getMessage()));
        }
    }
}
