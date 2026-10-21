package com.foody.ordertracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 150) String customerName,
        @NotBlank @Size(max = 255) String deliveryAddress,
        @NotEmpty @Valid List<OrderItemRequest> items
) {

    public CreateOrderRequest {
        // O null é preservado para a validação (@NotEmpty) responder 400 como antes.
        items = items == null ? null : List.copyOf(items);
    }
}