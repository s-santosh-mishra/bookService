package com.servicehub.serv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "booking_offers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_offer_booking_worker",
                columnNames = {"booking_id", "worker_id"}
        ),
        indexes = {
                @Index(name = "idx_booking_offers_worker", columnList = "worker_id, booking_id")
        }
)
public class BookingOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "offer_id", nullable = false, updatable = false)
    private UUID offerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "offered_at", nullable = false, updatable = false)
    private LocalDateTime offeredAt;

    public BookingOffer() {
    }

    public BookingOffer(Booking booking, Worker worker) {
        this.booking = booking;
        this.worker = worker;
    }

    @PrePersist
    protected void onCreate() {
        offeredAt = LocalDateTime.now();
    }

    public UUID getOfferId() {
        return offerId;
    }

    public Booking getBooking() {
        return booking;
    }

    public Worker getWorker() {
        return worker;
    }

    public LocalDateTime getOfferedAt() {
        return offeredAt;
    }
}
