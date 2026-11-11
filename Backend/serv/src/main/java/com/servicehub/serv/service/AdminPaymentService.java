package com.servicehub.serv.service;

import com.servicehub.serv.dto.AdminPaymentAttemptDto;
import com.servicehub.serv.dto.AdminPaymentDashboardDto;
import com.servicehub.serv.dto.AdminPaymentDetailsDto;
import com.servicehub.serv.dto.AdminPaymentListItemDto;
import com.servicehub.serv.dto.AdminPaymentSummaryDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.exception.ResourceNotFoundException;
import com.servicehub.serv.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;

    public AdminPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public AdminPaymentDashboardDto getPayments() {
        List<Payment> payments = paymentRepository.findAllForAdmin();
        return new AdminPaymentDashboardDto(
                summarize(payments),
                payments.stream().map(this::toListItem).toList());
    }

    @Transactional(readOnly = true)
    public AdminPaymentDetailsDto getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found."));

        Booking booking = payment.getBooking();
        List<Payment> relatedPayments = booking == null
                ? List.of()
                : paymentRepository.findByBookingBookingIdOrderByCreatedAtDesc(
                        booking.getBookingId());

        return toDetails(payment, relatedPayments);
    }

    private AdminPaymentSummaryDto summarize(List<Payment> payments) {
        long successfulCount = countByStatus(payments, PaymentStatus.SUCCESS);
        BigDecimal successfullyCollectedAmount = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                .map(payment -> payment.getAmount())
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));

        return new AdminPaymentSummaryDto(
                successfulCount,
                countByStatus(payments, PaymentStatus.PENDING),
                countByStatus(payments, PaymentStatus.FAILED),
                countByStatus(payments, PaymentStatus.CANCELLED),
                countByStatus(payments, PaymentStatus.REFUNDED),
                successfullyCollectedAmount);
    }

    private long countByStatus(
            List<Payment> payments,
            PaymentStatus status) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == status)
                .count();
    }

    private AdminPaymentListItemDto toListItem(Payment payment) {
        Booking booking = payment.getBooking();
        Users customer = getCustomerUser(booking);
        Users worker = getWorkerUser(booking);

        return new AdminPaymentListItemDto(
                payment.getPaymentId(),
                booking == null ? null : booking.getBookingId(),
                customer == null ? null : customer.getFullName(),
                customer == null ? null : customer.getPhone(),
                worker == null ? null : worker.getFullName(),
                worker == null ? null : worker.getPhone(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt());
    }

    private AdminPaymentDetailsDto toDetails(
            Payment payment,
            List<Payment> relatedPayments) {
        Booking booking = payment.getBooking();
        Users customer = getCustomerUser(booking);
        Users worker = getWorkerUser(booking);
        com.servicehub.serv.entity.Service service =
                booking == null ? null : booking.getService();

        AdminPaymentDetailsDto.PersonInfo customerInfo = customer == null
                ? null
                : toPersonInfo(customer);
        AdminPaymentDetailsDto.PersonInfo workerInfo = worker == null
                ? null
                : toPersonInfo(worker);
        AdminPaymentDetailsDto.BookingInfo bookingInfo = booking == null
                ? null
                : new AdminPaymentDetailsDto.BookingInfo(
                        booking.getStatus(),
                        service == null ? null : service.getServiceName(),
                        service == null || service.getCategory() == null
                                ? null
                                : service.getCategory().getCategoryName(),
                        booking.getCreatedAt());

        return new AdminPaymentDetailsDto(
                payment.getPaymentId(),
                booking == null ? null : booking.getBookingId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                customerInfo,
                workerInfo,
                bookingInfo,
                relatedPayments.stream().map(this::toAttempt).toList());
    }

    private AdminPaymentDetailsDto.PersonInfo toPersonInfo(Users user) {
        Credentials credentials = user.getCredentials();
        return new AdminPaymentDetailsDto.PersonInfo(
                user.getFullName(),
                user.getPhone(),
                credentials == null ? null : credentials.getEmail());
    }

    private AdminPaymentAttemptDto toAttempt(Payment payment) {
        return new AdminPaymentAttemptDto(
                payment.getPaymentId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getPaidAt());
    }

    private Users getCustomerUser(Booking booking) {
        Customer customer = booking == null ? null : booking.getCustomer();
        return customer == null ? null : customer.getUser();
    }

    private Users getWorkerUser(Booking booking) {
        Worker worker = booking == null ? null : booking.getWorker();
        return worker == null ? null : worker.getUser();
    }
}
