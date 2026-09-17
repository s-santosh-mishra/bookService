package com.servicehub.serv.dto;

import com.servicehub.serv.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerBookingRequestDto {

    private UUID bookingId;
    private UUID customerId;
    private UUID serviceId;
    private String serviceName;
    private String customerNote;
    private BookingStatus status;
    private LocalDateTime createdAt;

    public WorkerBookingRequestDto(
            UUID bookingId,
            UUID customerId,
            UUID serviceId,
            String serviceName,
            String customerNote,
            BookingStatus status,
            LocalDateTime createdAt) {

        this.bookingId = bookingId;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.customerNote = customerNote;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}