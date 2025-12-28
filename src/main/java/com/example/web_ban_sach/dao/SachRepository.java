package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.Sach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "sach")
public interface SachRepository extends JpaRepository<Sach, Integer> {
    public Page<Sach> findByTenSachContaining(@RequestParam("tenSach") String tenSach, Pageable pageable);
    public Page<Sach> findByDanhSachTheLoai_MaTheLoai(@RequestParam("maTheLoai") int maTheLoai, Pageable pageable);
    public Page<Sach> findByTenSachContainingAndDanhSachTheLoai_MaTheLoai(@RequestParam("tenSach") String tenSach, @RequestParam("maTheLoai") int maTheLoai, Pageable pageable);
    public Sach findByMaSach(int maSach);

    public long countBy();
    
    // Query để lấy sách với thông tin seller
    @Query("SELECT DISTINCT s FROM Sach s " +
           "LEFT JOIN FETCH s.danhSachHinhAnh " +
           "WHERE s.maSach = :maSach")
    Sach findByMaSachWithDetails(@Param("maSach") int maSach);
    
    // Query để lấy IDs của sách (pagination)
    @Query("SELECT s.maSach FROM Sach s")
    List<Integer> findAllSachIds();
    
    // Query để lấy IDs của sách với sorting
    @Query("SELECT s.maSach FROM Sach s ORDER BY " +
           "CASE WHEN :sortBy = 'maSach' AND :sortDir = 'asc' THEN s.maSach END ASC, " +
           "CASE WHEN :sortBy = 'maSach' AND :sortDir = 'desc' THEN s.maSach END DESC, " +
           "CASE WHEN :sortBy = 'soLuongDaBan' AND :sortDir = 'asc' THEN s.soLuongDaBan END ASC, " +
           "CASE WHEN :sortBy = 'soLuongDaBan' AND :sortDir = 'desc' THEN s.soLuongDaBan END DESC, " +
           "CASE WHEN :sortBy = 'trungBinhXepHang' AND :sortDir = 'asc' THEN s.trungBinhXepHang END ASC, " +
           "CASE WHEN :sortBy = 'trungBinhXepHang' AND :sortDir = 'desc' THEN s.trungBinhXepHang END DESC, " +
           "CASE WHEN :sortBy = 'giaBan' AND :sortDir = 'asc' THEN s.giaBan END ASC, " +
           "CASE WHEN :sortBy = 'giaBan' AND :sortDir = 'desc' THEN s.giaBan END DESC, " +
           "s.maSach DESC")
    List<Integer> findAllSachIdsWithSort(@Param("sortBy") String sortBy, @Param("sortDir") String sortDir);
    
    // Query để lấy sách với hình ảnh theo IDs
    @Query("SELECT DISTINCT s FROM Sach s " +
           "LEFT JOIN FETCH s.danhSachHinhAnh " +
           "WHERE s.maSach IN :maSachList")
    List<Sach> findByMaSachListWithImages(@Param("maSachList") List<Integer> maSachList);
    
    // Query để lấy sách với thể loại theo IDs
    @Query("SELECT DISTINCT s FROM Sach s " +
           "LEFT JOIN FETCH s.danhSachTheLoai " +
           "WHERE s.maSach IN :maSachList")
    List<Sach> findByMaSachListWithCategories(@Param("maSachList") List<Integer> maSachList);
    
    // Query để lấy sách với thể loại và hình ảnh theo ID (cho cache)
    @Query("SELECT s FROM Sach s " +
           "LEFT JOIN FETCH s.danhSachTheLoai " +
           "LEFT JOIN FETCH s.danhSachHinhAnh " +
           "WHERE s.maSach = :maSach")
    Optional<Sach> findByIdWithCollections(@Param("maSach") int maSach);
    
    // Query để lấy IDs của sách theo thể loại
    @Query("SELECT s.maSach FROM Sach s " +
           "JOIN s.danhSachTheLoai tl " +
           "WHERE tl.maTheLoai = :maTheLoai")
    List<Integer> findSachIdsByTheLoai(@Param("maTheLoai") int maTheLoai);
}

