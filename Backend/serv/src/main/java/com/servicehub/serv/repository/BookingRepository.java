package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
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

        List<Booking> findByStatusAndWorkerConfirmedCompletionTrueAndCustomerConfirmedCompletionFalseAndWorkerCompletedAtBefore(
                        BookingStatus status,
                        LocalDateTime time);
}