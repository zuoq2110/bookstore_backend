package com.example.web_ban_sach.Service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;

public interface SachService {
    public ResponseEntity<?> save(JsonNode jsonNode);
    public ResponseEntity<?> update(JsonNode jsonNode);
}
