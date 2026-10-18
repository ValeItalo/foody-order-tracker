package com.foody.ordertracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        @NotBlank @Size(max = 150) String customerName,
        @NotBlank @Size(max = 255) String deliveryAddress,
        @NotEmpty @Valid List<OrderItemRequest> items
) { }