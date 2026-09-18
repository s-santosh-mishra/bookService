package com.servicehub.serv.dto;

import com.servicehub.serv.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class WorkerBookingRequestDto {
    private UUID bookingId, customerId, serviceId;
    private String customerName, serviceName, customerAddressLine1, customerCity, customerNote;
    private BookingStatus status;
    private LocalDateTime createdAt;

    public WorkerBookingRequestDto() {
    }

    public WorkerBookingRequestDto(UUID bookingId, UUID customerId, UUID serviceId, String customerName,
            String serviceName,
            String customerNote, BookingStatus status, LocalDateTime createdAt,
            String customerAddressLine1, String customerCity) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.customerName = customerName;
        this.serviceName = serviceName;
        this.customerNote = customerNote;
        this.status = status;
        this.createdAt = createdAt;
        this.customerAddressLine1 = customerAddressLine1;
        this.customerCity = customerCity;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID v) {
        bookingId = v;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID v) {
        customerId = v;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID v) {
        serviceId = v;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String v) {
        customerName = v;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String v) {
        serviceName = v;
    }

    public String getCustomerAddressLine1() {
        return customerAddressLine1;
    }

    public void setCustomerAddressLine1(String v) {
        customerAddressLine1 = v;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String v) {
        customerCity = v;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String v) {
        customerNote = v;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
