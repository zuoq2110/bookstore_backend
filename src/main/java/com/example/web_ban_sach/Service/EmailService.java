package com.example.web_ban_sach.Service;

public interface EmailService {
    public void sendMessage(String from, String to, String subject, String text);
}
