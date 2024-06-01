package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.HinhAnh;
import com.example.web_ban_sach.entity.Sach;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@RepositoryRestResource(path = "hinh-anh")
public interface HinhAnhRepository extends JpaRepository<HinhAnh, Integer> {
    public List<HinhAnh> findHinhAnhsBySach(Sach sach);

    @Modifying
    @Transactional
    @Query("DELETE FROM HinhAnh i WHERE i.laIcon = false AND i.sach.maSach = :maSach")
    public void deleteHinhAnhsWithFalseThumbnailByMaSach(@Param("maSach") int maSach);
}
