package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.GioHang;
import com.example.web_ban_sach.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "gio-hang")
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {
public GioHang findByMaGioHang(int maGioHang);

public void deleteGioHangByNguoiDung(NguoiDung nguoiDung);
}
