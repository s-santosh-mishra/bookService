package com.servicehub.serv.controller;

import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.CreateBookingDto;
import com.servicehub.serv.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/bookings")
public class CustomerBookingController {
        private final BookingService bookingService;

        public CustomerBookingController(BookingService bookingService) {
                this.bookingService = bookingService;
        }

        @PostMapping
        public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreateBookingDto request,
                        Authentication authentication) {
                UUID customerId = UUID.fromString(authentication.getName());
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(bookingService.createBooking(customerId, request));
        }

        @GetMapping
        public ResponseEntity<List<BookingDto>> getCustomerBookings(Authentication authentication) {
                UUID customerId = UUID.fromString(authentication.getName());
                return ResponseEntity.ok(bookingService.getCustomerBookings(customerId));
        }

        @PostMapping("/{bookingId}/complete")
        public ResponseEntity<BookingDto> customerConfirmCompletion(@PathVariable UUID bookingId,
                        Authentication authentication) {
                UUID customerId = UUID.fromString(authentication.getName());
                return ResponseEntity.ok(bookingService.customerConfirmCompletion(customerId, bookingId));
        }
}
