package com.servicehub.serv.repository;

import com.servicehub.serv.entity.BookingPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingPartRepository extends JpaRepository<BookingPart, UUID> {

    List<BookingPart> findByBookingBookingId(UUID bookingId);
}