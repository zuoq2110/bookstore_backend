package com.example.web_ban_sach.dao;

import com.example.web_ban_sach.entity.Quyen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@RepositoryRestResource(path = "quyen")
public interface QuyenRepository extends JpaRepository<Quyen, Integer> {
    public Quyen findByTenQuyen(String tenQuyen);
}
