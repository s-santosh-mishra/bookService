package com.servicehub.serv.service;

import com.servicehub.serv.dto.UnlistedPartRequestDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.entity.UnlistedPartRequest;
import com.servicehub.serv.enums.UnlistedPartRequestStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingPartRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.UnlistedPartRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
public class UnlistedPartRequestService {

        private final UnlistedPartRequestRepository requestRepository;
        private final BookingRepository bookingRepository;
        private final BookingPartRepository bookingPartRepository;

        public UnlistedPartRequestService(
                        UnlistedPartRequestRepository requestRepository,
                        BookingRepository bookingRepository,
                        BookingPartRepository bookingPartRepository) {

                this.requestRepository = requestRepository;
                this.bookingRepository = bookingRepository;
                this.bookingPartRepository = bookingPartRepository;
        }

        @Transactional
        public UnlistedPartRequest createRequest(
                        UUID workerId,
                        UUID bookingId,
                        UnlistedPartRequestDto request) {

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

                if (booking.getWorker() == null ||
                                !booking.getWorker().getUserId().equals(workerId)) {
                        throw new IllegalStateException(
                                        "You are not assigned to this booking.");
                }

                if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
                        throw new IllegalStateException(
                                        "Unlisted parts can only be requested while the service is in progress.");
                }

                UnlistedPartRequest partRequest = new UnlistedPartRequest();

                partRequest.setBooking(booking);
                partRequest.setPartName(request.getPartName().trim());
                partRequest.setPrice(request.getPrice());
                partRequest.setQuantity(request.getQuantity());
                partRequest.setStatus(UnlistedPartRequestStatus.PENDING);
                partRequest.setCreatedAt(LocalDateTime.now());

                return requestRepository.save(partRequest);
        }

        @Transactional
        public UnlistedPartRequest approveRequest(
                        UUID customerId,
                        UUID requestId) {

                UnlistedPartRequest request = requestRepository.findById(requestId)
                                .orElseThrow(() -> new IllegalArgumentException("Part request not found."));

                Booking booking = request.getBooking();

                if (booking.getCustomer() == null ||
                                !booking.getCustomer().getUserId().equals(customerId)) {

                        throw new IllegalStateException(
                                        "You are not the customer for this booking.");
                }

                if (request.getStatus() != UnlistedPartRequestStatus.PENDING) {
                        throw new IllegalStateException(
                                        "This part request has already been processed.");
                }

                request.setStatus(UnlistedPartRequestStatus.APPROVED);
                request.setRespondedAt(LocalDateTime.now());

                BookingPart bookingPart = new BookingPart();

                bookingPart.setBooking(booking);

                // Unlisted part → no catalog Part
                bookingPart.setPart(null);

                // Preserve the actual requested part name
                bookingPart.setPartName(request.getPartName());

                bookingPart.setQuantity(request.getQuantity());
                bookingPart.setUnitPrice(request.getPrice());

                BigDecimal totalPrice = request.getPrice()
                                .multiply(BigDecimal.valueOf(request.getQuantity()));

                bookingPart.setTotalPrice(totalPrice);

                bookingPartRepository.save(bookingPart);

                return requestRepository.save(request);
        }

        @Transactional
        public UnlistedPartRequest rejectRequest(
                        UUID customerId,
                        UUID requestId) {

                UnlistedPartRequest request = requestRepository.findById(requestId)
                                .orElseThrow(() -> new IllegalArgumentException("Part request not found."));

                Booking booking = request.getBooking();

                if (booking.getCustomer() == null ||
                                !booking.getCustomer().getUserId().equals(customerId)) {

                        throw new IllegalStateException(
                                        "You are not the customer for this booking.");
                }

                if (request.getStatus() != UnlistedPartRequestStatus.PENDING) {
                        throw new IllegalStateException(
                                        "This part request has already been processed.");
                }

                request.setStatus(UnlistedPartRequestStatus.REJECTED);
                request.setRespondedAt(LocalDateTime.now());

                return requestRepository.save(request);
        }

        @Transactional(readOnly = true)
        public List<UnlistedPartRequest> getCustomerPendingRequests(UUID customerId) {

                return requestRepository
                                .findByBookingCustomerUserIdAndStatusOrderByCreatedAtAsc(
                                                customerId,
                                                UnlistedPartRequestStatus.PENDING);
        }
}