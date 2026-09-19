package com.servicehub.serv.service;

import com.servicehub.serv.dto.AddBookingPartDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingPart;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.entity.Part;
import com.servicehub.serv.repository.BookingPartRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@Service
public class BookingPartService {

        private final BookingPartRepository bookingPartRepository;
        private final BookingRepository bookingRepository;
        private final PartRepository partRepository;

        public BookingPartService(
                        BookingPartRepository bookingPartRepository,
                        BookingRepository bookingRepository,
                        PartRepository partRepository) {

                this.bookingPartRepository = bookingPartRepository;
                this.bookingRepository = bookingRepository;
                this.partRepository = partRepository;
        }

        @Transactional
        public BookingPart addPart(
                        UUID workerId,
                        UUID bookingId,
                        AddBookingPartDto request) {

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

                if (booking.getWorker() == null ||
                                !booking.getWorker().getUserId().equals(workerId)) {
                        throw new IllegalStateException(
                                        "You are not assigned to this booking.");
                }

                if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
                        throw new IllegalStateException(
                                        "Parts can only be added while the service is in progress.");
                }

                Part part = partRepository.findById(request.getPartId())
                                .orElseThrow(() -> new IllegalArgumentException("Part not found."));

                if (!part.isActive()) {
                        throw new IllegalStateException("This part is no longer active.");
                }

                BigDecimal unitPrice = part.getUnitPrice();

                BigDecimal totalPrice = unitPrice.multiply(
                                BigDecimal.valueOf(request.getQuantity()));

                BookingPart bookingPart = new BookingPart();
                bookingPart.setBooking(booking);
                bookingPart.setPart(part);
                bookingPart.setQuantity(request.getQuantity());
                bookingPart.setPartName(part.getName());
                bookingPart.setUnitPrice(unitPrice);
                bookingPart.setTotalPrice(totalPrice);

                return bookingPartRepository.save(bookingPart);
        }

        @Transactional(readOnly = true)
        public List<BookingPart> getBookingParts(UUID bookingId) {

                if (!bookingRepository.existsById(bookingId)) {
                        throw new IllegalArgumentException("Booking not found.");
                }

                return bookingPartRepository.findByBookingBookingId(bookingId);
        }
}