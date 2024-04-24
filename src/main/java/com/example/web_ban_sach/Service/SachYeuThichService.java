package com.example.web_ban_sach.Service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

public interface SachYeuThichService {
    public ResponseEntity<?> save(JsonNode jsonNode);

    public ResponseEntity<?> delete(JsonNode jsonNode);

    public ResponseEntity<?> get(int maNguoiDung);
}
