package com.example.web_ban_sach.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardResponse {
    private int totalBooks;
    private long totalOrders;
    private double totalRevenue;
    private double averageRating;
}
