package com.example.web_ban_sach.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {
    
    private boolean success;
    private OrderTrackingData data;
    private ErrorInfo error;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderTrackingData {
        private int orderId;
        private String currentStatus;
        private String trackingNumber;
        private String estimatedDelivery;
        private String carrier;
        private String carrierPhone;
        private String trackingUrl;
        private List<TimelineEntry> timeline;
        private ShippingAddress shippingAddress;
        private OrderSummary orderSummary;
        private SellerInfo sellerInfo;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEntry {
        private String status;
        private String title;
        private String description;
        private String timestamp;  // ISO 8601 format
        private boolean isCompleted;
        private String location;
        private String updatedBy;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        private String fullName;
        private String phone;
        private String address;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {
        private int totalItems;
        private double totalAmount;
        private String paymentMethod;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerInfo {
        private String tenGianHang;
        private String diaChiGianHang;
        private Double viDoGianHang;
        private Double kinhDoGianHang;
        private String soDienThoaiGianHang;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private String code;
        private String message;
    }
    
    // Helper methods để tạo response
    public static OrderTrackingResponse success(OrderTrackingData data) {
        OrderTrackingResponse response = new OrderTrackingResponse();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
    
    public static OrderTrackingResponse error(String code, String message) {
        OrderTrackingResponse response = new OrderTrackingResponse();
        response.setSuccess(false);
        response.setError(new ErrorInfo(code, message));
        return response;
    }
}
