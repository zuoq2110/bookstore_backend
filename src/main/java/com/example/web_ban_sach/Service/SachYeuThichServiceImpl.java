package com.example.web_ban_sach.Service;

import com.example.web_ban_sach.dao.NguoiDungRepository;
import com.example.web_ban_sach.dao.SachRepository;
import com.example.web_ban_sach.dao.SachYeuThichRepository;
import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Sach;
import com.example.web_ban_sach.entity.SachYeuThich;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SachYeuThichServiceImpl implements SachYeuThichService{
    private final ObjectMapper objectMapper;
    @Autowired
    private SachYeuThichRepository sachYeuThichRepository;
    @Autowired
    private SachRepository sachRepository;
    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    public SachYeuThichServiceImpl(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    private String formatStringByJson(String json) {
        return json.replaceAll("\"", "");
    }
    @Override
    public ResponseEntity<?> save(JsonNode jsonNode) {
        try {
            SachYeuThich sachYeuThichResponse = objectMapper.treeToValue(jsonNode, SachYeuThich.class);
            int maSach = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maSach"))));
            Sach sach = sachRepository.findByMaSach(maSach);
        sachYeuThichResponse.setSach(sach);
            int maNguoiDung = Integer.parseInt(formatStringByJson(String.valueOf(jsonNode.get("maNguoiDung"))));
            NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);
        sachYeuThichResponse.setNguoiDung(nguoiDung);
        sachYeuThichRepository.save(sachYeuThichResponse);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(JsonNode jsonNode) {
        try {
            int maSach = Integer.parseInt(formatStringByJson(jsonNode.get("maSach").toString()));
            int maNguoiDung = Integer.parseInt(formatStringByJson(jsonNode.get("maNguoiDung").toString()));

            Sach sach = sachRepository.findByMaSach(maSach);
            NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);

            SachYeuThich sachYeuThich = sachYeuThichRepository.findSachYeuThichBySachAndNguoiDung(sach, nguoiDung);

            sachYeuThichRepository.delete(sachYeuThich);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> get(int maNguoiDung){
        try {
            NguoiDung nguoiDung = nguoiDungRepository.findByMaNguoiDung(maNguoiDung);
            List<SachYeuThich> list= sachYeuThichRepository.findSachYeuThichByNguoiDung(nguoiDung);
            List<Integer> sachYeuThichList = new ArrayList<>();
            for(SachYeuThich sach: list){
                sachYeuThichList.add(sach.getSach().getMaSach());
            }
            return ResponseEntity.ok().body(sachYeuThichList);

        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

    }
}
