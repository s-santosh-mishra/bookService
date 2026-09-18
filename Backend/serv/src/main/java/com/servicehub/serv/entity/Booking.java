package com.servicehub.serv.entity;

import com.servicehub.serv.enums.BookingStatus;
import jakarta.persistence.*;
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
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "worker_confirmed_completion", nullable = false)
    private boolean workerConfirmedCompletion = false;

    @Column(name = "customer_confirmed_completion", nullable = false)
    private boolean customerConfirmedCompletion = false;

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
}
