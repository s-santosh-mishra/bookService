package com.servicehub.serv.dto;

import com.servicehub.serv.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingDto {
    private UUID bookingId, customerId, workerId, serviceId;

    private String serviceName, workerName;

    private BookingStatus status;

    private String customerNote;

    private LocalDateTime createdAt, updatedAt, acceptedAt, startedAt, completedAt, cancelledAt, failedAt;

    private LocalDateTime workerCancelledAt;

    private LocalDateTime workerCompletedAt;

    private String workerCancellationReason;

    private String workerCancellationMessage;

    private boolean workerConfirmedCompletion, customerConfirmedCompletion;

    private String customerName, customerPhone, customerAddressLine1, customerAddressLine2, customerLandmark,
            customerCity, customerState, customerPinCode;

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

    public UUID getWorkerId() {
        return workerId;
    }

    public void setWorkerId(UUID v) {
        workerId = v;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID v) {
        serviceId = v;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String v) {
        serviceName = v;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String v) {
        workerName = v;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus v) {
        status = v;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String v) {
        customerNote = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime v) {
        acceptedAt = v;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime v) {
        startedAt = v;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime v) {
        completedAt = v;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime v) {
        cancelledAt = v;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime v) {
        failedAt = v;
    }

    public LocalDateTime getWorkerCancelledAt() {
        return workerCancelledAt;
    }

    public void setWorkerCancelledAt(LocalDateTime v) {
        workerCancelledAt = v;
    }

    public LocalDateTime getWorkerCompletedAt() {
        return workerCompletedAt;
    }

    public void setWorkerCompletedAt(LocalDateTime workerCompletedAt) {
        this.workerCompletedAt = workerCompletedAt;
    }

    public String getWorkerCancellationReason() {
        return workerCancellationReason;
    }

    public void setWorkerCancellationReason(String v) {
        workerCancellationReason = v;
    }

    public String getWorkerCancellationMessage() {
        return workerCancellationMessage;
    }

    public void setWorkerCancellationMessage(String v) {
        workerCancellationMessage = v;
    }

    public boolean isWorkerConfirmedCompletion() {
        return workerConfirmedCompletion;
    }

    public void setWorkerConfirmedCompletion(boolean v) {
        workerConfirmedCompletion = v;
    }

    public boolean isCustomerConfirmedCompletion() {
        return customerConfirmedCompletion;
    }

    public void setCustomerConfirmedCompletion(boolean v) {
        customerConfirmedCompletion = v;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String v) {
        customerName = v;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String v) {
        customerPhone = v;
    }

    public String getCustomerAddressLine1() {
        return customerAddressLine1;
    }

    public void setCustomerAddressLine1(String v) {
        customerAddressLine1 = v;
    }

    public String getCustomerAddressLine2() {
        return customerAddressLine2;
    }

    public void setCustomerAddressLine2(String v) {
        customerAddressLine2 = v;
    }

    public String getCustomerLandmark() {
        return customerLandmark;
    }

    public void setCustomerLandmark(String v) {
        customerLandmark = v;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String v) {
        customerCity = v;
    }

    public String getCustomerState() {
        return customerState;
    }

    public void setCustomerState(String v) {
        customerState = v;
    }

    public String getCustomerPinCode() {
        return customerPinCode;
    }

    public void setCustomerPinCode(String v) {
        customerPinCode = v;
    }
}
