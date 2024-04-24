package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.NguoiDung;
import com.example.web_ban_sach.entity.Sach;
import com.example.web_ban_sach.entity.SachYeuThich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@RepositoryRestResource(path = "sach-yeu-thich")
public interface SachYeuThichRepository extends JpaRepository<SachYeuThich, Integer> {
    public SachYeuThich findSachYeuThichBySachAndNguoiDung(Sach sach, NguoiDung nguoiDung);
    public SachYeuThich findByMaSachYeuThich(int maSachYeuThich);
    public List<SachYeuThich> findSachYeuThichByNguoiDung(NguoiDung nguoiDung);
}
