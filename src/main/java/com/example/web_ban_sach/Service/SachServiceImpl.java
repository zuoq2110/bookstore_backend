package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.Service.util.Base64ToMultipartFileConverter;
import com.example.web_ban_sach.dao.HinhAnhRepository;
import com.example.web_ban_sach.dao.SachRepository;
import com.example.web_ban_sach.dao.TheLoaiRepository;
import com.example.web_ban_sach.entity.HinhAnh;
import com.example.web_ban_sach.entity.Sach;
import com.example.web_ban_sach.entity.TheLoai;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SachServiceImpl implements SachService{
    private final ObjectMapper objectMapper;
    @Autowired
    private SachRepository sachRepository;
    @Autowired
    private TheLoaiRepository theLoaiRepository;
    @Autowired
    private HinhAnhRepository hinhAnhRepository;
    @Autowired
    private UploadImageService uploadImageService;

    public SachServiceImpl(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    @Override
    @Transactional
    public ResponseEntity<?> save(JsonNode jsonNode) {
        try{
            Sach sach = objectMapper.treeToValue(jsonNode, Sach.class);
            List<Integer> danhSachMaTheLoai = objectMapper.readValue(jsonNode.get("maTheLoai").traverse(),  new TypeReference<List<Integer>>() {
            });
            List<TheLoai> danhSachTheLoai = new ArrayList<>();
            for(int maTheLoai: danhSachMaTheLoai){
                Optional<TheLoai> theLoai = theLoaiRepository.findById(maTheLoai);
                danhSachTheLoai.add(theLoai.get());
            }
            sach.setDanhSachTheLoai(danhSachTheLoai);

            Sach sachMoi = sachRepository.save(sach);


            String dataThumbnail = formatStringByJson(String.valueOf(jsonNode.get("thumbnail")));

            HinhAnh thumbnail = new HinhAnh();
            thumbnail.setSach(sachMoi);
            thumbnail.setLaIcon(true);
            MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(dataThumbnail);
            String thumbnailUrl = uploadImageService.uploadImage(multipartFile, "Sach_" + sachMoi.getMaSach());
            thumbnail.setDuongDan(thumbnailUrl);

            List<HinhAnh> danhSachHinhAnh = new ArrayList<>();
            danhSachHinhAnh.add(thumbnail);

            String dataRelatedImg = formatStringByJson(String.valueOf((jsonNode.get("anhLienQuan"))));
            List<String> arrDataRelatedImg = objectMapper.readValue(jsonNode.get("anhLienQuan").traverse(), new TypeReference<List<String>>() {
            });

            for (int i = 0; i < arrDataRelatedImg.size(); i++) {
                String img = arrDataRelatedImg.get(i);
                HinhAnh image = new HinhAnh();
                image.setSach(sachMoi);
                image.setLaIcon(false);
                MultipartFile relatedImgFile = Base64ToMultipartFileConverter.convert(img);
                String imgURL = uploadImageService.uploadImage(relatedImgFile, "Book_" + sachMoi.getMaSach() + "." + i);
                image.setDuongDan(imgURL);
                danhSachHinhAnh.add(image);
            }

            sachMoi.setDanhSachHinhAnh(danhSachHinhAnh);
            // Cập nhật lại ảnh
            sachRepository.save(sachMoi);


            return ResponseEntity.ok("Success!");

        } catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<?> update(JsonNode jsonNode) {
        try{
            Sach sach = objectMapper.treeToValue(jsonNode, Sach.class);
            List<HinhAnh> danhSachHinhAnh = hinhAnhRepository.findHinhAnhsBySach(sach);

            List<Integer> maTheLoaiList = objectMapper.readValue(jsonNode.get("maTheLoai").traverse(), new TypeReference<List<Integer>>() {
            });
            List<TheLoai> theLoaiList = new ArrayList<>();
            for(int maTheLoai: maTheLoaiList){
                Optional<TheLoai> theLoai = theLoaiRepository.findById(maTheLoai);
                theLoaiList.add(theLoai.get());
            }
            sach.setDanhSachTheLoai(theLoaiList);

            String dataThumbnail = formatStringByJson(String.valueOf(jsonNode.get("thumbnail")));
            if(Base64ToMultipartFileConverter.isBase64(dataThumbnail)){
                for(HinhAnh hinhAnh: danhSachHinhAnh){
                    if(hinhAnh.isLaIcon()){
                        MultipartFile multipartFile = Base64ToMultipartFileConverter.convert(dataThumbnail);
                        String thumbnailUrl = uploadImageService.uploadImage(multipartFile, "Book_" + sach.getMaSach());
                        hinhAnh.setDuongDan(thumbnailUrl);
                        hinhAnhRepository.save(hinhAnh);
                        break;
                    }
                }
            }

            Sach sachMoi = sachRepository.save(sach);

            List<String> danhSachAnhLienQuan = objectMapper.readValue(jsonNode.get("anhLienQuan").traverse(), new TypeReference<List<String>>() {
            });
            boolean kiemTraXoa = true;

            for (String img : danhSachAnhLienQuan) {
                if (!Base64ToMultipartFileConverter.isBase64(img)) {
                    kiemTraXoa = false;
                }
            }

            // Nếu xoá hết tất cả
            if (kiemTraXoa) {
                hinhAnhRepository.deleteHinhAnhsWithFalseThumbnailByMaSach(sachMoi.getMaSach());
                HinhAnh thumbnailTemp = danhSachHinhAnh.get(0);
                danhSachHinhAnh.clear();
                danhSachHinhAnh.add(thumbnailTemp);
                for (int i = 0; i < danhSachAnhLienQuan.size(); i++) {
                    String img = danhSachAnhLienQuan.get(i);
                    HinhAnh image = new HinhAnh();
                    image.setSach(sachMoi);
//                    image.setDataImage(img);
                    image.setLaIcon(false);
                    MultipartFile relatedImgFile = Base64ToMultipartFileConverter.convert(img);
                    String imgURL = uploadImageService.uploadImage(relatedImgFile, "Book_" + sachMoi.getMaSach() + "." + i);
                    image.setDuongDan(imgURL);
                    danhSachHinhAnh.add(image);
                }
            } else {
                // Nếu không xoá hết tất cả (Giữ nguyên ảnh hoặc thêm ảnh vào)
                for (int i = 0; i < danhSachAnhLienQuan.size(); i++) {
                    String img = danhSachAnhLienQuan.get(i);
                    if (Base64ToMultipartFileConverter.isBase64(img)) {
                        HinhAnh image = new HinhAnh();
                        image.setSach(sachMoi);
//                        image.setDataImage(img);
                        image.setLaIcon(false);
                        MultipartFile relatedImgFile = Base64ToMultipartFileConverter.convert(img);
                        String imgURL = uploadImageService.uploadImage(relatedImgFile, "Book_" + sachMoi.getMaSach() + "." + i);
                        image.setDuongDan(imgURL);
                        hinhAnhRepository.save(image);
                    }
                }
            }

            sachMoi.setDanhSachHinhAnh(danhSachHinhAnh);
            sachRepository.save(sachMoi);

            return ResponseEntity.ok("Success");

        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    private String formatStringByJson(String json) {
        return json.replaceAll("\"", "");
    }
}
