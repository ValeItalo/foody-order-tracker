package com.foody.ordertracker.dto;

import com.foody.ordertracker.entity.Order;
import com.foody.ordertracker.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String deliveryAddress,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {

    public OrderResponse {
        // Cópia imutável da lista (SpotBugs EI_EXPOSE_REP).
        items = List.copyOf(items);
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getDeliveryAddress(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}