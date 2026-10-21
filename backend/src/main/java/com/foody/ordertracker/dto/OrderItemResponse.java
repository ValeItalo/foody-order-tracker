package com.foody.ordertracker.dto;

import com.foody.ordertracker.entity.OrderItem;

public record OrderItemResponse(String productName, int quantity) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getProductName(), item.getQuantity());
    }
}