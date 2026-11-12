package com.servicehub.serv.repository;

import com.servicehub.serv.entity.BookingOffer;
import com.servicehub.serv.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingOfferRepository extends JpaRepository<BookingOffer, UUID> {

    boolean existsByBooking_BookingIdAndWorker_UserId(UUID bookingId, UUID workerId);

    @Query("""
            SELECT offer
            FROM BookingOffer offer
            JOIN FETCH offer.booking booking
            JOIN FETCH booking.customer customer
            JOIN FETCH customer.user customerUser
            JOIN FETCH booking.service service
            WHERE offer.worker.userId = :workerId
              AND booking.status = :status
              AND NOT EXISTS (
                  SELECT rejection.rejectionId
                  FROM WorkerBookingRejection rejection
                  WHERE rejection.booking = booking
                    AND rejection.worker = offer.worker
              )
            ORDER BY offer.offeredAt
            """)
    List<BookingOffer> findPendingOffersForWorker(
            @Param("workerId") UUID workerId,
            @Param("status") BookingStatus status);
}
