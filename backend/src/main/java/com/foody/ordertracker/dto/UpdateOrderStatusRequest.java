package com.foody.ordertracker.dto;

import com.foody.ordertracker.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}