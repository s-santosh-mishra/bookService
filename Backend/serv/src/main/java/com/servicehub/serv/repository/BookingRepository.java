package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

        List<Booking> findByCustomerUserId(UUID customerId);

        List<Booking> findByWorkerUserId(UUID workerId);

        long countByCustomerUserIdAndStatusIn(
                        UUID customerId,
                        List<BookingStatus> statuses);

        List<Booking> findByCustomerUserIdAndStatusIn(
                        UUID customerId,
                        List<BookingStatus> statuses);

        List<Booking> findByWorkerUserIdAndStatusIn(
                        UUID workerId,
                        List<BookingStatus> statuses);

        boolean existsByCustomerUserIdAndServiceServiceIdAndStatusIn(
                        UUID customerId,
                        UUID serviceId,
                        List<BookingStatus> statuses);

        List<Booking> findByServiceServiceIdInAndStatus(
                        List<UUID> serviceIds,
                        BookingStatus status);

        boolean existsByWorkerUserIdAndStatusIn(
                        UUID workerId,
                        List<BookingStatus> statuses);

        List<Booking> findByStatusAndCreatedAtBefore(
                        BookingStatus status,
                        LocalDateTime createdAt);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId")
        Optional<Booking> findByBookingIdForUpdate(
                        @Param("bookingId") UUID bookingId);

}