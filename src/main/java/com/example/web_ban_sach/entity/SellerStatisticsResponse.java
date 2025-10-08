package com.example.web_ban_sach.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerStatisticsResponse {
    private double totalRevenue;
    private long totalOrders;
    private long totalBooksSold;
    private long totalCustomers;
    private List<Double> revenueData;
    private List<TopSellingBook> topSellingBooks;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellingBook {
        private int maSach;
        private String tenSach;
        private long totalSold;
        private double totalRevenue;
    }
}
