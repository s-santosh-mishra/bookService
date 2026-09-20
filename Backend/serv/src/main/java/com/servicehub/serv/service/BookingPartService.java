package com.servicehub.serv.service;

import com.servicehub.serv.dto.AddBookingPartDto;
import com.servicehub.serv.dto.BookingPartDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PartApprovalStatus;
import com.servicehub.serv.repository.BookingPartRepository;
import com.servicehub.serv.repository.BookingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingPartService {

        private final BookingPartRepository bookingPartRepository;
        private final BookingRepository bookingRepository;

        public BookingPartService(
                        BookingPartRepository bookingPartRepository,
                        BookingRepository bookingRepository) {

                this.bookingPartRepository = bookingPartRepository;
                this.bookingRepository = bookingRepository;
        }

        @Transactional
        public BookingPart addPart(
                        UUID workerId,
                        UUID bookingId,
                        AddBookingPartDto request) {

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

                if (booking.getWorker() == null
                                || !booking.getWorker().getUserId().equals(workerId)) {

                        throw new IllegalStateException(
                                        "You are not assigned to this booking.");
                }

                if (booking.getStatus() != BookingStatus.IN_PROGRESS) {

                        throw new IllegalStateException(
                                        "Parts can only be added while the service is in progress.");
                }

                if (request.getPhoto() == null
                                || request.getPhoto().isEmpty()) {

                        throw new IllegalArgumentException(
                                        "A photo of the part is required.");
                }

                String contentType = request.getPhoto().getContentType();

                if (contentType == null
                                || !contentType.startsWith("image/")) {

                        throw new IllegalArgumentException(
                                        "Only image files are allowed.");
                }

                try {

                        BookingPart bookingPart = new BookingPart();

                        bookingPart.setBooking(booking);
                        bookingPart.setPartName(
                                        request.getPartName().trim());

                        bookingPart.setUnitPrice(request.getPrice());

                        bookingPart.setQuantity(request.getQuantity());

                        bookingPart.setTotalPrice(
                                        request.getPrice()
                                                        .multiply(
                                                                        java.math.BigDecimal.valueOf(
                                                                                        request.getQuantity())));

                        bookingPart.setPhoto(
                                        request.getPhoto().getBytes());

                        bookingPart.setPhotoContentType(contentType);

                        bookingPart.setStatus(
                                        PartApprovalStatus.PENDING);

                        bookingPart.setCreatedAt(
                                        LocalDateTime.now());

                        return bookingPartRepository.save(bookingPart);

                } catch (IOException e) {

                        throw new IllegalStateException(
                                        "Unable to process part photo.", e);
                }
        }

        @Transactional
        public BookingPartDto approvePart(
                        UUID customerId,
                        UUID bookingPartId) {

                BookingPart part = bookingPartRepository.findById(bookingPartId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Part request not found."));

                Booking booking = part.getBooking();

                if (booking.getCustomer() == null
                                || !booking.getCustomer()
                                                .getUserId()
                                                .equals(customerId)) {

                        throw new IllegalStateException(
                                        "You are not the customer for this booking.");
                }

                if (part.getStatus() != PartApprovalStatus.PENDING) {

                        throw new IllegalStateException(
                                        "This part has already been approved.");
                }

                part.setStatus(PartApprovalStatus.APPROVED);
                part.setApprovedAt(LocalDateTime.now());

                return toDto(bookingPartRepository.save(part));
        }

        @Transactional(readOnly = true)
        public List<BookingPart> getBookingParts(UUID workerId, UUID bookingId) {

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new RuntimeException("Booking not found"));

                if (booking.getWorker() == null
                                || !booking.getWorker().getUser().getUserId().equals(workerId)) {
                        throw new RuntimeException("You are not authorized to view parts for this booking");
                }

                return bookingPartRepository.findByBookingBookingId(bookingId);
        }

        @Transactional(readOnly = true)
        public List<BookingPartDto> getCustomerPendingParts(UUID customerId) {

                return bookingPartRepository
                                .findByBookingCustomerUserIdAndStatusOrderByCreatedAtAsc(
                                                customerId,
                                                PartApprovalStatus.PENDING)
                                .stream()
                                .map(this::toDto)
                                .toList();
        }

        @Transactional
        public void autoApprovePendingParts(UUID bookingId) {

                List<BookingPart> parts = bookingPartRepository.findByBookingBookingId(bookingId);

                LocalDateTime now = LocalDateTime.now();

                for (BookingPart part : parts) {

                        if (part.getStatus() == PartApprovalStatus.PENDING) {

                                part.setStatus(PartApprovalStatus.APPROVED);
                                part.setApprovedAt(now);
                        }
                }

                bookingPartRepository.saveAll(parts);
        }

        @Transactional(readOnly = true)
public BookingPart getBookingPartForPhoto(
        UUID userId,
        UUID bookingPartId) {

    BookingPart part = bookingPartRepository.findById(bookingPartId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Part not found."));

    Booking booking = part.getBooking();

    boolean isCustomer = booking.getCustomer() != null
            && booking.getCustomer().getUserId().equals(userId);

    boolean isWorker = booking.getWorker() != null
            && booking.getWorker().getUserId().equals(userId);

    if (!isCustomer && !isWorker) {
        throw new IllegalStateException(
                "You are not authorized to view this part photo.");
    }

    return part;
}

        private BookingPartDto toDto(BookingPart part) {

                return new BookingPartDto(
                                part.getBookingPartId(),
                                part.getPartName(),
                                part.getUnitPrice(),
                                part.getQuantity(),
                                part.getTotalPrice(),
                                part.getStatus(),
                                part.getCreatedAt(),
                                part.getApprovedAt());
        }
}