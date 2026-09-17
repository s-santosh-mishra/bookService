package com.servicehub.serv.controller;

import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.WorkerBookingRequestDto;
import com.servicehub.serv.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/worker/bookings")
public class WorkerBookingController {

    private final BookingService bookingService;

    public WorkerBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/requests")
    public ResponseEntity<List<WorkerBookingRequestDto>> getBookingRequests(
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        List<WorkerBookingRequestDto> requests = bookingService.getWorkerBookingRequests(workerId);

        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{bookingId}/accept")
    public ResponseEntity<BookingDto> acceptBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        BookingDto booking = bookingService.acceptBooking(
                workerId,
                bookingId);

        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<Void> rejectBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        bookingService.rejectBooking(
                workerId,
                bookingId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BookingDto>> getCustomerBookings(
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        List<BookingDto> bookings = bookingService.getCustomerBookings(customerId);

        return ResponseEntity.ok(bookings);
    }
}