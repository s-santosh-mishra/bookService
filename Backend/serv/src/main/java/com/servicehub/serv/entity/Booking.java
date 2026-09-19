package com.servicehub.serv.entity;

import com.servicehub.serv.enums.BookingStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "customer_note")
    private String customerNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "worker_completed_at")
    private LocalDateTime workerCompletedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "worker_cancelled_at")
    private LocalDateTime workerCancelledAt;

    @Column(name = "worker_cancellation_reason", length = 100)
    private String workerCancellationReason;

    @Column(name = "worker_cancellation_message", length = 1000)
    private String workerCancellationMessage;

    @Column(name = "worker_confirmed_completion", nullable = false)
    private boolean workerConfirmedCompletion = false;

    @Column(name = "customer_confirmed_completion", nullable = false)
    private boolean customerConfirmedCompletion = false;

    @Column(name = "service_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal serviceLatitude;

    @Column(name = "service_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal serviceLongitude;

    @Column(name = "worker_acceptance_latitude", nullable = false, precision = 10, scale = 7)
private BigDecimal workerAcceptanceLatitude;

@Column(name = "worker_acceptance_longitude", nullable = false, precision = 10, scale = 7)
private BigDecimal workerAcceptanceLongitude;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null)
            status = BookingStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID v) {
        bookingId = v;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer v) {
        customer = v;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker v) {
        worker = v;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service v) {
        service = v;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    public LocalDateTime getWorkerCompletedAt() {
        return workerCompletedAt;
    }

    public void setWorkerCompletedAt(LocalDateTime v) {
        workerCompletedAt = v;
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

    public BigDecimal getServiceLatitude() {
        return serviceLatitude;
    }

    public void setServiceLatitude(BigDecimal serviceLatitude) {
        this.serviceLatitude = serviceLatitude;
    }

    public BigDecimal getServiceLongitude() {
        return serviceLongitude;
    }

    public void setServiceLongitude(BigDecimal serviceLongitude) {
        this.serviceLongitude = serviceLongitude;
    }

    public BigDecimal getWorkerAcceptanceLatitude() {
    return workerAcceptanceLatitude;
}

public void setWorkerAcceptanceLatitude(BigDecimal workerAcceptanceLatitude) {
    this.workerAcceptanceLatitude = workerAcceptanceLatitude;
}

public BigDecimal getWorkerAcceptanceLongitude() {
    return workerAcceptanceLongitude;
}

public void setWorkerAcceptanceLongitude(BigDecimal workerAcceptanceLongitude) {
    this.workerAcceptanceLongitude = workerAcceptanceLongitude;
}
}
