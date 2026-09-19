package com.servicehub.serv.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddBookingPartDto {

    @NotNull
    private UUID partId;

    @NotNull
    @Min(1)
    private Integer quantity;

    public UUID getPartId() {
        return partId;
    }

    public void setPartId(UUID partId) {
        this.partId = partId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}