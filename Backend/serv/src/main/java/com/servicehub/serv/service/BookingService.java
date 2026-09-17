package com.servicehub.serv.service;

import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.CreateBookingDto;
import com.servicehub.serv.dto.WorkerBookingRequestDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.CustomerRepository;
import com.servicehub.serv.repository.ServiceRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
public class BookingService {

        private final BookingRepository bookingRepository;
        private final CustomerRepository customerRepository;
        private final ServiceRepository serviceRepository;
        private final WorkerRepository workerRepository;
        private final WorkerServiceRepository workerServiceRepository;

        public BookingService(
                        BookingRepository bookingRepository,
                        CustomerRepository customerRepository,
                        ServiceRepository serviceRepository,
                        WorkerRepository workerRepository,
                        WorkerServiceRepository workerServiceRepository) {

                this.bookingRepository = bookingRepository;
                this.customerRepository = customerRepository;
                this.serviceRepository = serviceRepository;
                this.workerRepository = workerRepository;
                this.workerServiceRepository = workerServiceRepository;
        }

        @Transactional
        public BookingDto createBooking(
                        UUID customerId,
                        CreateBookingDto request) {

                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> new IllegalArgumentException("Customer not found."));

                Service service = serviceRepository.findById(request.getServiceId())
                                .orElseThrow(() -> new IllegalArgumentException("Service not found."));

                if (!service.isActive()) {
                        throw new IllegalStateException(
                                        "Cannot book an inactive service.");
                }

                List<BookingStatus> activeStatuses = List.of(
                                BookingStatus.PENDING,
                                BookingStatus.ACCEPTED,
                                BookingStatus.IN_PROGRESS);

                boolean alreadyBooked = bookingRepository
                                .existsByCustomerUserIdAndServiceServiceIdAndStatusIn(
                                                customerId,
                                                request.getServiceId(),
                                                activeStatuses);

                if (alreadyBooked) {
                        throw new IllegalStateException(
                                        "You already have an active booking for this service.");
                }

                long activeBookingCount = bookingRepository.countByCustomerUserIdAndStatusIn(
                                customerId,
                                activeStatuses);

                if (activeBookingCount >= 3) {
                        throw new IllegalStateException(
                                        "Maximum 3 active bookings are allowed.");
                }

                Booking booking = new Booking();

                booking.setCustomer(customer);
                booking.setService(service);
                booking.setWorker(null);
                booking.setStatus(BookingStatus.PENDING);
                booking.setCustomerNote(request.getCustomerNote());

                Booking savedBooking = bookingRepository.save(booking);

                return toDto(savedBooking);
        }

        @Transactional
        public BookingDto acceptBooking(
                        UUID workerId,
                        UUID bookingId) {

                Worker worker = workerRepository.findById(workerId)
                                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

                if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
                        throw new IllegalStateException(
                                        "Only verified workers can accept bookings.");
                }

                if (worker.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                        throw new IllegalStateException(
                                        "Worker is not available.");
                }

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

                if (booking.getStatus() != BookingStatus.PENDING) {
                        throw new IllegalStateException(
                                        "Booking is no longer available.");
                }

                if (booking.getWorker() != null) {
                        throw new IllegalStateException(
                                        "Booking has already been assigned.");
                }

                boolean providesService = workerServiceRepository
                                .findByWorker_UserId(workerId)
                                .stream()
                                .anyMatch(workerService -> workerService
                                                .getService()
                                                .getServiceId()
                                                .equals(
                                                                booking.getService()
                                                                                .getServiceId()));

                if (!providesService) {
                        throw new IllegalStateException(
                                        "Worker does not provide this service.");
                }

                List<BookingStatus> activeWorkerStatuses = List.of(
                                BookingStatus.ACCEPTED,
                                BookingStatus.IN_PROGRESS);

                boolean alreadyWorking = bookingRepository.existsByWorkerUserIdAndStatusIn(
                                workerId,
                                activeWorkerStatuses);

                if (alreadyWorking) {
                        throw new IllegalStateException(
                                        "Worker already has an active booking.");
                }

                booking.setWorker(worker);
                booking.setStatus(BookingStatus.ACCEPTED);
                booking.setAcceptedAt(LocalDateTime.now());

                worker.setAvailabilityStatus(
                                AvailabilityStatus.BUSY);

                Booking savedBooking = bookingRepository.save(booking);

                return toDto(savedBooking);
        }

        public List<WorkerBookingRequestDto> getWorkerBookingRequests(
                        UUID workerId) {

                List<WorkerService> workerServices = workerServiceRepository.findByWorker_UserId(workerId);

                List<UUID> serviceIds = workerServices.stream()
                                .map(workerService -> workerService.getService().getServiceId())
                                .toList();

                if (serviceIds.isEmpty()) {
                        return List.of();
                }

                List<Booking> bookings = bookingRepository.findByServiceServiceIdInAndStatus(
                                serviceIds,
                                BookingStatus.PENDING);

                return bookings.stream()
                                .map(booking -> new WorkerBookingRequestDto(
                                                booking.getBookingId(),
                                                booking.getCustomer().getUserId(),
                                                booking.getService().getServiceId(),
                                                booking.getService().getServiceName(),
                                                booking.getCustomerNote(),
                                                booking.getStatus(),
                                                booking.getCreatedAt()))
                                .toList();
        }

        @Transactional
        public void rejectBooking(
                        UUID workerId,
                        UUID bookingId) {

                Worker worker = workerRepository.findById(workerId)
                                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

                if (worker.getVerificationStatus() != VerificationStatus.VERIFIED) {
                        throw new IllegalStateException(
                                        "Only verified workers can reject bookings.");
                }

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

                if (booking.getStatus() != BookingStatus.PENDING) {
                        throw new IllegalStateException(
                                        "Booking is no longer available.");
                }

                boolean providesService = workerServiceRepository
                                .findByWorker_UserId(workerId)
                                .stream()
                                .anyMatch(workerService -> workerService.getService()
                                                .getServiceId()
                                                .equals(
                                                                booking.getService()
                                                                                .getServiceId()));

                if (!providesService) {
                        throw new IllegalStateException(
                                        "Worker does not provide this service.");
                }
        }

        public List<BookingDto> getCustomerBookings(UUID customerId) {

                List<Booking> bookings = bookingRepository.findByCustomerUserId(customerId);

                return bookings.stream()
                                .map(this::toDto)
                                .toList();
        }

        private BookingDto toDto(Booking booking) {

                BookingDto dto = new BookingDto();

                dto.setBookingId(booking.getBookingId());
                dto.setCustomerId(booking.getCustomer().getUserId());

                dto.setServiceId(booking.getService().getServiceId());
                dto.setServiceName(booking.getService().getServiceName());

                if (booking.getWorker() != null) {
                        dto.setWorkerId(booking.getWorker().getUserId());
                        dto.setWorkerName(booking.getWorker().getUser().getFullName());
                }

                dto.setStatus(booking.getStatus());
                dto.setCustomerNote(booking.getCustomerNote());
                dto.setCreatedAt(booking.getCreatedAt());
                dto.setUpdatedAt(booking.getUpdatedAt());
                dto.setAcceptedAt(booking.getAcceptedAt());
                dto.setStartedAt(booking.getStartedAt());
                dto.setCompletedAt(booking.getCompletedAt());
                dto.setCancelledAt(booking.getCancelledAt());
                dto.setFailedAt(booking.getFailedAt());

                return dto;
        }
}