package com.servicehub.serv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "worker_booking_rejections",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_worker_booking_rejection",
                        columnNames = {"worker_id", "booking_id"}
                )
        }
)
public class WorkerBookingRejection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rejection_id", nullable = false, updatable = false)
    private UUID rejectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "rejected_at", nullable = false)
    private LocalDateTime rejectedAt;

    @PrePersist
    protected void onCreate() {
        rejectedAt = LocalDateTime.now();
    }

    public UUID getRejectionId() {
        return rejectionId;
    }

    public void setRejectionId(UUID rejectionId) {
        this.rejectionId = rejectionId;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }
}