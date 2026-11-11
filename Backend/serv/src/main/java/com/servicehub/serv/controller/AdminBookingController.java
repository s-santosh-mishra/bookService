package com.servicehub.serv.controller;

import com.servicehub.serv.dto.AdminBookingCancellationRequestDto;
import com.servicehub.serv.dto.AdminBookingDetailsDto;
import com.servicehub.serv.dto.AdminBookingSummaryDto;
import com.servicehub.serv.service.AdminBookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBookingSummaryDto>> getBookings() {
        return ResponseEntity.ok(adminBookingService.getBookings());
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<AdminBookingDetailsDto> getBooking(
            @PathVariable UUID bookingId) {
        return ResponseEntity.ok(adminBookingService.getBooking(bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<AdminBookingDetailsDto> cancelBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody AdminBookingCancellationRequestDto request) {
        return ResponseEntity.ok(
                adminBookingService.cancelBooking(
                        bookingId,
                        request.getReason()));
    }
}
