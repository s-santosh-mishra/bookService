package com.servicehub.serv.repository;

import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.enums.PartApprovalStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingPartRepository
        extends JpaRepository<BookingPart, UUID> {

    List<BookingPart> findByBookingBookingId(UUID bookingId);

    List<BookingPart>
    findByBookingCustomerUserIdAndStatusOrderByCreatedAtAsc(
            UUID customerId,
            PartApprovalStatus status);
}