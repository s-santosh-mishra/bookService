package com.servicehub.serv.controller;

import com.servicehub.serv.dto.AddBookingPartDto;
import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.WorkerBookingLocationDto;
import com.servicehub.serv.dto.WorkerBookingRequestDto;
import com.servicehub.serv.dto.WorkerCancellationRequestDto;
import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.service.BookingPartService;
import com.servicehub.serv.service.BookingService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/worker/bookings")
public class WorkerBookingController {

    private final BookingService bookingService;
    private final BookingPartService bookingPartService;

    public WorkerBookingController(
            BookingService bookingService,
            BookingPartService bookingPartService) {

        this.bookingService = bookingService;
        this.bookingPartService = bookingPartService;
    }

    @GetMapping("/requests")
    public ResponseEntity<List<WorkerBookingRequestDto>> getBookingRequests(
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingService.getWorkerBookingRequests(workerId));
    }

    @PostMapping("/{bookingId}/accept")
    public ResponseEntity<BookingDto> acceptBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody WorkerBookingLocationDto request,
            Authentication authentication) {

        return ResponseEntity.ok(
                bookingService.acceptBooking(
                        UUID.fromString(authentication.getName()),
                        bookingId,
                        request));
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<Void> rejectBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        bookingService.rejectBooking(
                UUID.fromString(authentication.getName()),
                bookingId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookingId}/start")
    public ResponseEntity<BookingDto> startBooking(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                bookingService.startBooking(
                        UUID.fromString(authentication.getName()),
                        bookingId));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingDto> workerCancelBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody WorkerCancellationRequestDto request,
            Authentication authentication) {

        return ResponseEntity.ok(
                bookingService.workerCancelBooking(
                        UUID.fromString(authentication.getName()),
                        bookingId,
                        request));
    }

    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<BookingDto> workerConfirmCompletion(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        return ResponseEntity.ok(
                bookingService.workerConfirmCompletion(
                        UUID.fromString(authentication.getName()),
                        bookingId));
    }

    // =========================
    // BOOKING PARTS
    // =========================

    @PostMapping(
            value = "/{bookingId}/parts",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<BookingPart> addPart(
            @PathVariable UUID bookingId,
            @Valid @ModelAttribute AddBookingPartDto request,
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingPartService.addPart(
                        workerId,
                        bookingId,
                        request));
    }

    @GetMapping("/{bookingId}/parts")
    public ResponseEntity<List<BookingPart>> getBookingParts(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingPartService.getBookingParts(
                        workerId,
                        bookingId));
    }

    // =========================
    // WORKER BOOKINGS
    // =========================

    @GetMapping
    public ResponseEntity<List<BookingDto>> getWorkerBookings(
            Authentication authentication) {

        return ResponseEntity.ok(
                bookingService.getWorkerBookings(
                        UUID.fromString(authentication.getName())));
    }
}