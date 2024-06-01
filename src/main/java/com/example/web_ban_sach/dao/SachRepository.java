package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.Sach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

@RepositoryRestResource(path = "sach")
public interface SachRepository extends JpaRepository<Sach, Integer> {
    public Page<Sach> findByTenSachContaining(@RequestParam("tenSach") String tenSach, Pageable pageable);
    public Page<Sach> findByDanhSachTheLoai_MaTheLoai(@RequestParam("maTheLoai") int maTheLoai, Pageable pageable);
    public Page<Sach> findByTenSachContainingAndDanhSachTheLoai_MaTheLoai(@RequestParam("tenSach") String tenSach, @RequestParam("maTheLoai") int maTheLoai, Pageable pageable);
public Sach findByMaSach(int maSach);

    public long countBy();
}

