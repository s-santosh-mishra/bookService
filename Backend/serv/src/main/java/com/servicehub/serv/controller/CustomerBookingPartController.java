package com.servicehub.serv.controller;

import com.servicehub.serv.dto.BookingPartDto;
import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.service.BookingPartService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer/bookings")
public class CustomerBookingPartController {

    private final BookingPartService bookingPartService;

    public CustomerBookingPartController(
            BookingPartService bookingPartService) {
        this.bookingPartService = bookingPartService;
    }

    @GetMapping("/{bookingId}/parts")
    public ResponseEntity<List<BookingPartDto>> getBookingParts(
            @PathVariable UUID bookingId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingPartService.getCustomerBookingParts(
                        customerId,
                        bookingId));
    }

    @PostMapping("/parts/{bookingPartId}/approve")
    public ResponseEntity<BookingPartDto> approvePart(
            @PathVariable UUID bookingPartId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingPartService.approvePart(
                        customerId,
                        bookingPartId));
    }

    @GetMapping("/parts/requests")
    public ResponseEntity<List<BookingPartDto>> getPendingParts(
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                bookingPartService.getCustomerPendingParts(
                        customerId));
    }

    @GetMapping("/parts/{bookingPartId}/photo")
    public ResponseEntity<byte[]> getPartPhoto(
            @PathVariable UUID bookingPartId,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        BookingPart part =
                bookingPartService.getBookingPartForPhoto(
                        userId,
                        bookingPartId);

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(
                    part.getPhotoContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(part.getPhoto());
    }
}