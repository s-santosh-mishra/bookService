
package com.servicehub.serv.controller;

import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.CreateBookingDto;
import com.servicehub.serv.dto.SubmitWorkerReviewDto;
import com.servicehub.serv.service.BookingService;
import com.servicehub.serv.service.WorkerReviewService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/bookings")
public class CustomerBookingController {

    private final BookingService bookingService;
    private final WorkerReviewService workerReviewService;

    public CustomerBookingController(
            BookingService bookingService,
            WorkerReviewService workerReviewService) {
        this.bookingService = bookingService;
        this.workerReviewService = workerReviewService;
    }

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(
            @Valid @RequestBody CreateBookingDto request,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(customerId, request));
    }

    @GetMapping
    public ResponseEntity<List<BookingDto>> getCustomerBookings(
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingService.getCustomerBookings(customerId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        BookingDto booking = bookingService.cancelBooking(
                customerId,
                bookingId);

        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/review")
    public ResponseEntity<Void> submitReview(
            @PathVariable UUID bookingId,
            @RequestBody SubmitWorkerReviewDto request,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        workerReviewService.submitReview(
                customerId,
                bookingId,
                request.getRating(),
                request.getFeedbackTags());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{bookingId}/review/skip")
    public ResponseEntity<Void> skipReview(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        workerReviewService.skipReview(customerId, bookingId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/review-decisions")
    public ResponseEntity<Map<UUID, String>> getReviewDecisions(
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                workerReviewService.getCustomerReviewDecisions(customerId));
    }
}
