package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.ChiTietDonHang;
import com.example.web_ban_sach.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@RepositoryRestResource(path = "chi-tiet-don-hang")
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Long> {
    public List<ChiTietDonHang> findChiTietDonHangByDonHang(DonHang donHang);
}
