package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.ChiTietDonHang;
import com.example.web_ban_sach.entity.SuDanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@RepositoryRestResource(path = "su-danh-gia")
public interface SuDanhGiaRepository extends JpaRepository<SuDanhGia, Long> {
    public SuDanhGia findByChiTietDonHang(ChiTietDonHang chiTietDonHang);
    public SuDanhGia findByMaDanhGia(long maDanhGia);

    public long countBy();
}
