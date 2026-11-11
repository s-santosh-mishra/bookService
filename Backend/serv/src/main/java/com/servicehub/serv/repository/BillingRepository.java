package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Billing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingRepository extends JpaRepository<Billing, UUID> {

    Optional<Billing> findByBookingBookingId(UUID bookingId);

    List<Billing> findByBookingBookingIdIn(Collection<UUID> bookingIds);

    boolean existsByBookingBookingId(UUID bookingId);
}