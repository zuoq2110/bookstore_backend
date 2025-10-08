package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RepositoryRestResource(excerptProjection = NguoiDung.class,path = "nguoi-dung")
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Integer> {
    boolean existsByTenDangNhap( String tenDangNhap);

    boolean existsByEmail(@RequestParam String email);

    public NguoiDung findByTenDangNhap(String tenDangNhap);
    public NguoiDung findByEmail(String email);
    public NguoiDung findByMaNguoiDung(int maNguoiDung);
    
    // Tìm tất cả admin (users có quyền = 2)
    @Query("SELECT DISTINCT n FROM NguoiDung n JOIN n.danhSachQuyen q WHERE q.maQuyen = :roleId")
    List<NguoiDung> findUsersByRoleId(@Param("roleId") int roleId);
}
