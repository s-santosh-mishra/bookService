package com.servicehub.serv.controller;

import com.servicehub.serv.dto.AddBookingPartDto;
import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.UnlistedPartRequestDto;
import com.servicehub.serv.dto.WorkerBookingLocationDto;
import com.servicehub.serv.dto.WorkerBookingRequestDto;
import com.servicehub.serv.dto.WorkerCancellationRequestDto;
import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.entity.UnlistedPartRequest;
import com.servicehub.serv.service.BookingPartService;
import com.servicehub.serv.service.BookingService;
import com.servicehub.serv.service.UnlistedPartRequestService;

import jakarta.validation.Valid;

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
    private final UnlistedPartRequestService unlistedPartRequestService;

    public WorkerBookingController(
            BookingService bookingService,
            BookingPartService bookingPartService,
            UnlistedPartRequestService unlistedPartRequestService) {

        this.bookingService = bookingService;
        this.bookingPartService = bookingPartService;
        this.unlistedPartRequestService = unlistedPartRequestService;
    }

    @GetMapping("/requests")
    public ResponseEntity<List<WorkerBookingRequestDto>> getBookingRequests(Authentication authentication) {
        UUID workerId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(bookingService.getWorkerBookingRequests(workerId));
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
    public ResponseEntity<Void> rejectBooking(@PathVariable UUID bookingId, Authentication authentication) {
        bookingService.rejectBooking(UUID.fromString(authentication.getName()), bookingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookingId}/start")
    public ResponseEntity<BookingDto> startBooking(@PathVariable UUID bookingId, Authentication authentication) {
        return ResponseEntity.ok(bookingService.startBooking(UUID.fromString(authentication.getName()), bookingId));
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
    public ResponseEntity<BookingDto> workerConfirmCompletion(@PathVariable UUID bookingId,
            Authentication authentication) {
        return ResponseEntity
                .ok(bookingService.workerConfirmCompletion(UUID.fromString(authentication.getName()), bookingId));
    }

    @PostMapping("/{bookingId}/parts")
    public ResponseEntity<BookingPart> addBookingPart(
            @PathVariable UUID bookingId,
            @Valid @RequestBody AddBookingPartDto request,
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingPartService.addPart(
                        workerId,
                        bookingId,
                        request));
    }

    @PostMapping("/{bookingId}/unlisted-parts")
    public ResponseEntity<UnlistedPartRequest> requestUnlistedPart(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UnlistedPartRequestDto request,
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                unlistedPartRequestService.createRequest(
                        workerId,
                        bookingId,
                        request));
    }

    @GetMapping("/{bookingId}/parts")
    public ResponseEntity<List<BookingPart>> getBookingParts(
            @PathVariable UUID bookingId) {

        return ResponseEntity.ok(
                bookingPartService.getBookingParts(bookingId));
    }

    @GetMapping
    public ResponseEntity<List<BookingDto>> getWorkerBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getWorkerBookings(UUID.fromString(authentication.getName())));
    }
}
