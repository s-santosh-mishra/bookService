package com.servicehub.serv.service;

import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.dto.CreateBookingDto;
import com.servicehub.serv.dto.WorkerBookingRequestDto;
import com.servicehub.serv.entity.*;
import com.servicehub.serv.enums.*;
import com.servicehub.serv.repository.*;
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

    public BookingService(BookingRepository bookingRepository, CustomerRepository customerRepository, ServiceRepository serviceRepository,
                           WorkerRepository workerRepository, WorkerServiceRepository workerServiceRepository) {
        this.bookingRepository=bookingRepository; this.customerRepository=customerRepository; this.serviceRepository=serviceRepository;
        this.workerRepository=workerRepository; this.workerServiceRepository=workerServiceRepository;
    }

    private List<BookingStatus> customerActiveStatuses(){return List.of(BookingStatus.PENDING,BookingStatus.ACCEPTED,BookingStatus.IN_PROGRESS);}

    @Transactional
    public BookingDto createBooking(UUID customerId,CreateBookingDto request){
        Customer customer=customerRepository.findById(customerId).orElseThrow(()->new IllegalArgumentException("Customer not found."));
        Service service=serviceRepository.findById(request.getServiceId()).orElseThrow(()->new IllegalArgumentException("Service not found."));
        if(!service.isActive()) throw new IllegalStateException("Cannot book an inactive service.");
        List<BookingStatus> active=customerActiveStatuses();
        if(bookingRepository.existsByCustomerUserIdAndServiceServiceIdAndStatusIn(customerId,request.getServiceId(),active))
            throw new IllegalStateException("You already have an active booking for this service.");
        if(bookingRepository.countByCustomerUserIdAndStatusIn(customerId,active)>=3)
            throw new IllegalStateException("Maximum 3 active bookings are allowed.");
        Booking booking=new Booking(); booking.setCustomer(customer); booking.setService(service); booking.setWorker(null);
        booking.setStatus(BookingStatus.PENDING); booking.setCustomerNote(request.getCustomerNote());
        return toDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto acceptBooking(UUID workerId,UUID bookingId){
        Worker worker=workerRepository.findById(workerId).orElseThrow(()->new IllegalArgumentException("Worker not found."));
        if(worker.getVerificationStatus()!=VerificationStatus.VERIFIED) throw new IllegalStateException("Only verified workers can accept bookings.");
        if(worker.getAvailabilityStatus()!=AvailabilityStatus.AVAILABLE) throw new IllegalStateException("Worker is not available.");
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->new IllegalArgumentException("Booking not found."));
        if(booking.getStatus()!=BookingStatus.PENDING) throw new IllegalStateException("Booking is no longer available.");
        if(booking.getWorker()!=null) throw new IllegalStateException("Booking has already been assigned.");
        boolean provides=workerServiceRepository.findByWorker_UserId(workerId).stream().anyMatch(ws->ws.getService().getServiceId().equals(booking.getService().getServiceId()));
        if(!provides) throw new IllegalStateException("Worker does not provide this service.");
        if(bookingRepository.existsByWorkerUserIdAndStatusIn(workerId,List.of(BookingStatus.ACCEPTED,BookingStatus.IN_PROGRESS)))
            throw new IllegalStateException("Worker already has an active booking.");
        booking.setWorker(worker); booking.setStatus(BookingStatus.ACCEPTED); booking.setAcceptedAt(LocalDateTime.now());
        worker.setAvailabilityStatus(AvailabilityStatus.BUSY);
        return toDto(bookingRepository.save(booking));
    }

    public List<WorkerBookingRequestDto> getWorkerBookingRequests(UUID workerId){
        List<UUID> serviceIds=workerServiceRepository.findByWorker_UserId(workerId).stream().map(ws->ws.getService().getServiceId()).toList();
        if(serviceIds.isEmpty()) return List.of();
        return bookingRepository.findByServiceServiceIdInAndStatus(serviceIds,BookingStatus.PENDING).stream().map(b->
            new WorkerBookingRequestDto(b.getBookingId(),b.getCustomer().getUserId(),b.getService().getServiceId(),
                b.getCustomer().getUser().getFullName(),b.getService().getServiceName(),b.getCustomerNote(),b.getStatus(),b.getCreatedAt(),
                b.getCustomer().getUser().getAddressLine1(),b.getCustomer().getUser().getCity())).toList();
    }

    @Transactional
    public void rejectBooking(UUID workerId,UUID bookingId){
        Worker worker=workerRepository.findById(workerId).orElseThrow(()->new IllegalArgumentException("Worker not found."));
        if(worker.getVerificationStatus()!=VerificationStatus.VERIFIED) throw new IllegalStateException("Only verified workers can reject bookings.");
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->new IllegalArgumentException("Booking not found."));
        if(booking.getStatus()!=BookingStatus.PENDING) throw new IllegalStateException("Booking is no longer available.");
        boolean provides=workerServiceRepository.findByWorker_UserId(workerId).stream().anyMatch(ws->ws.getService().getServiceId().equals(booking.getService().getServiceId()));
        if(!provides) throw new IllegalStateException("Worker does not provide this service.");
    }

    @Transactional
    public BookingDto startBooking(UUID workerId,UUID bookingId){
        Worker worker=workerRepository.findById(workerId).orElseThrow(()->new IllegalArgumentException("Worker not found."));
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->new IllegalArgumentException("Booking not found."));
        if(booking.getWorker()==null || !booking.getWorker().getUserId().equals(workerId)) throw new IllegalStateException("Booking is not assigned to this worker.");
        if(booking.getStatus()!=BookingStatus.ACCEPTED) throw new IllegalStateException("Booking cannot be started.");
        if(worker.getVerificationStatus()!=VerificationStatus.VERIFIED) throw new IllegalStateException("Only verified workers can start bookings.");
        booking.setStatus(BookingStatus.IN_PROGRESS); booking.setStartedAt(LocalDateTime.now());
        return toDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto workerConfirmCompletion(UUID workerId,UUID bookingId){
        Worker worker=workerRepository.findById(workerId).orElseThrow(()->new IllegalArgumentException("Worker not found."));
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->new IllegalArgumentException("Booking not found."));
        if(booking.getWorker()==null || !booking.getWorker().getUserId().equals(workerId)) throw new IllegalStateException("Booking is not assigned to this worker.");
        if(booking.getStatus()!=BookingStatus.IN_PROGRESS) throw new IllegalStateException("Booking is not in progress.");
        if(worker.getVerificationStatus()!=VerificationStatus.VERIFIED) throw new IllegalStateException("Only verified workers can confirm completion.");
        booking.setWorkerConfirmedCompletion(true);
        completeIfBothConfirmed(booking);
        return toDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto customerConfirmCompletion(UUID customerId,UUID bookingId){
        Customer customer=customerRepository.findById(customerId).orElseThrow(()->new IllegalArgumentException("Customer not found."));
        Booking booking=bookingRepository.findById(bookingId).orElseThrow(()->new IllegalArgumentException("Booking not found."));
        if(!booking.getCustomer().getUserId().equals(customerId)) throw new IllegalStateException("Booking does not belong to this customer.");
        if(booking.getStatus()!=BookingStatus.IN_PROGRESS) throw new IllegalStateException("Booking is not in progress.");
        booking.setCustomerConfirmedCompletion(true);
        completeIfBothConfirmed(booking);
        return toDto(bookingRepository.save(booking));
    }

    private void completeIfBothConfirmed(Booking booking){
        if(booking.isWorkerConfirmedCompletion() && booking.isCustomerConfirmedCompletion()){
            booking.setStatus(BookingStatus.COMPLETED); booking.setCompletedAt(LocalDateTime.now());
            if(booking.getWorker()!=null) booking.getWorker().setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        }
    }

    public List<BookingDto> getCustomerBookings(UUID customerId){return bookingRepository.findByCustomerUserId(customerId).stream().map(this::toDto).toList();}
    public List<BookingDto> getWorkerBookings(UUID workerId){return bookingRepository.findByWorkerUserId(workerId).stream().map(this::toDto).toList();}

    private BookingDto toDto(Booking b){
        BookingDto d=new BookingDto(); d.setBookingId(b.getBookingId()); d.setCustomerId(b.getCustomer().getUserId());
        d.setServiceId(b.getService().getServiceId()); d.setServiceName(b.getService().getServiceName());
        if(b.getWorker()!=null){d.setWorkerId(b.getWorker().getUserId()); d.setWorkerName(b.getWorker().getUser().getFullName());}
        d.setStatus(b.getStatus()); d.setCustomerNote(b.getCustomerNote()); d.setCreatedAt(b.getCreatedAt()); d.setUpdatedAt(b.getUpdatedAt());
        d.setAcceptedAt(b.getAcceptedAt()); d.setStartedAt(b.getStartedAt()); d.setCompletedAt(b.getCompletedAt()); d.setCancelledAt(b.getCancelledAt()); d.setFailedAt(b.getFailedAt());
        d.setWorkerConfirmedCompletion(b.isWorkerConfirmedCompletion()); d.setCustomerConfirmedCompletion(b.isCustomerConfirmedCompletion());
        d.setCustomerName(b.getCustomer().getUser().getFullName()); d.setCustomerPhone(b.getCustomer().getUser().getPhone());
        d.setCustomerAddressLine1(b.getCustomer().getUser().getAddressLine1()); d.setCustomerAddressLine2(b.getCustomer().getUser().getAddressLine2());
        d.setCustomerLandmark(b.getCustomer().getUser().getLandmark()); d.setCustomerCity(b.getCustomer().getUser().getCity());
        d.setCustomerState(b.getCustomer().getUser().getState()); d.setCustomerPinCode(b.getCustomer().getUser().getPinCode());
        return d;
    }
}
