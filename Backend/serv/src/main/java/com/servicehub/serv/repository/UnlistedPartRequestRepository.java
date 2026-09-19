package com.servicehub.serv.repository;

import com.servicehub.serv.entity.UnlistedPartRequest;
import com.servicehub.serv.enums.UnlistedPartRequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnlistedPartRequestRepository
        extends JpaRepository<UnlistedPartRequest, UUID> {

    List<UnlistedPartRequest> findByBookingBookingIdOrderByCreatedAtAsc(UUID bookingId);

    List<UnlistedPartRequest> findByBookingCustomerUserIdAndStatusOrderByCreatedAtAsc(
            UUID customerId,
            UnlistedPartRequestStatus status);
}