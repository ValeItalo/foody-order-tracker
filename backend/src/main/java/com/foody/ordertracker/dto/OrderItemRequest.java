package com.foody.ordertracker.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderItemRequest(
        @NotBlank @Size(max = 150) String productName,
        @NotNull @Min(1) Integer quantity
) { }