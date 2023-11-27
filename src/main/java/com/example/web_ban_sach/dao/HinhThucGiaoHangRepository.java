package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.HinhThucGiaoHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HinhThucGiaoHangRepository extends JpaRepository<HinhThucGiaoHang, Integer> {
}
