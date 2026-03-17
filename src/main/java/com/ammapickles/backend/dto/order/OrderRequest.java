package com.ammapickles.backend.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull(message = "Delivery address ID is required")
    private Long addressId;
}