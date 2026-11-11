
package com.servicehub.serv.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.servicehub.serv.entity.Billing;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.repository.BillingRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.WorkerRepository;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BillingRepository billingRepository;
    private final WorkerRepository workerRepository;
    private final Clock settlementClock;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public PaymentService(
            RazorpayClient razorpayClient,
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BillingRepository billingRepository,
            WorkerRepository workerRepository,
            Clock settlementClock) {

        this.razorpayClient = razorpayClient;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.billingRepository = billingRepository;
        this.workerRepository = workerRepository;
        this.settlementClock = settlementClock;
    }

    // Create a Razorpay order using the final bill calculated by the backend.
    @Transactional
    public Map<String, Object> createOrder(
            UUID customerId,
            UUID bookingId) throws RazorpayException {

        Booking booking = bookingRepository
                .findByBookingIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found."));

        if (!booking.getCustomer().getUserId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to pay for this booking.");
        }

        validatePaymentEligibility(booking);

        Billing billing = getBilling(bookingId);
        BigDecimal amount = validateAmount(billing.getFinalBilledPrice());

        if (paymentRepository.existsByBookingBookingIdAndStatus(
                bookingId, PaymentStatus.SUCCESS)) {
            throw new IllegalStateException(
                    "This booking has already been paid.");
        }

        Payment existingPayment = paymentRepository
                .findByBookingBookingIdAndPaymentMethodAndStatusAndRazorpayOrderIdIsNotNull(
                        bookingId,
                        PaymentMethod.RAZORPAY,
                        PaymentStatus.PENDING)
                .orElse(null);

        if (existingPayment != null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("paymentId", existingPayment.getPaymentId());
            response.put("keyId", razorpayKeyId);
            response.put("orderId", existingPayment.getRazorpayOrderId());
            response.put(
                    "amount",
                    existingPayment.getAmount()
                            .movePointRight(2)
                            .intValueExact());
            response.put("currency", "INR");

            return response;
        }

        int amountInPaise = amount.movePointRight(2).intValueExact();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", bookingId.toString());

        Order order = razorpayClient.orders.create(orderRequest);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setRazorpayOrderId(order.get("id"));
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentId", savedPayment.getPaymentId());
        response.put("keyId", razorpayKeyId);
        response.put("orderId", order.get("id"));
        response.put("amount", amountInPaise);
        response.put("currency", "INR");

        return response;
    }

    // Verify the Razorpay signature before accepting payment as successful.
    @Transactional
    public Map<String, Object> verifyRazorpayPayment(
            UUID customerId,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) throws RazorpayException {

        Payment payment = paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment order not found."));

        Booking booking = bookingRepository
                .findByBookingIdForUpdate(payment.getBooking().getBookingId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found."));

        if (!booking.getCustomer().getUserId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to verify this payment.");
        }

        if (payment.getPaymentMethod() != PaymentMethod.RAZORPAY) {
            throw new IllegalStateException(
                    "This is not a Razorpay payment.");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (razorpayPaymentId.equals(payment.getRazorpayPaymentId())) {
                completeBookingAfterPayment(booking);
                return paymentResult(payment);
            }

            throw new IllegalStateException(
                    "This order has already been paid.");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "This payment is no longer pending.");
        }

        if (booking.getStatus() != BookingStatus.WAITING_FOR_PAYMENT) {
            throw new IllegalStateException(
                    "Booking is not waiting for payment.");
        }

        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_payment_id", razorpayPaymentId);
        attributes.put("razorpay_signature", razorpaySignature);

        boolean validSignature;

        try {
            Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            validSignature = true;
        } catch (RazorpayException ex) {
            validSignature = false;
        }

        if (!validSignature) {
            throw new IllegalArgumentException(
                    "Razorpay payment signature verification failed.");
        }

        // Signature verification is required, but a successful signature
        // alone should not be treated as proof of captured funds.
        com.razorpay.Payment razorpayPayment =
                razorpayClient.payments.fetch(razorpayPaymentId);

        String capturedOrderId = razorpayPayment.get("order_id");
        String gatewayStatus = razorpayPayment.get("status");

        if (!razorpayOrderId.equals(capturedOrderId)
                || !"captured".equalsIgnoreCase(gatewayStatus)) {
            throw new IllegalStateException(
                    "Razorpay has not confirmed a captured payment.");
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.SUCCESS);

        payment.setPaidAt(LocalDateTime.now(
                settlementClock.withZone(
                        SettlementBusinessDateCalculator.SETTLEMENT_ZONE)));

        paymentRepository.save(payment);
        completeBookingAfterPayment(booking);

        return paymentResult(payment);
    }

    // Create a pending cash payment for the customer to pay the worker directly.
    @Transactional
    public Map<String, Object> createCashPayment(
            UUID customerId,
            UUID bookingId) {

        Booking booking = getCustomerBooking(customerId, bookingId);
        validatePaymentEligibility(booking);

        Billing billing = getBilling(bookingId);
        BigDecimal amount = validateAmount(billing.getFinalBilledPrice());

        if (paymentRepository.existsByBookingBookingIdAndStatus(
                bookingId, PaymentStatus.SUCCESS)) {
            throw new IllegalStateException(
                    "This booking has already been paid.");
        }

        Payment existingCashPayment = paymentRepository
                .findByBookingBookingIdAndPaymentMethodAndStatus(
                        bookingId,
                        PaymentMethod.CASH,
                        PaymentStatus.PENDING)
                .orElse(null);

        if (existingCashPayment != null) {
            return paymentResult(existingCashPayment);
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        return paymentResult(savedPayment);
    }

    @Transactional
    public void cancelPendingPayment(UUID customerId, UUID bookingId) {

        Booking booking = getCustomerBooking(customerId, bookingId);
        validatePaymentEligibility(booking);

        if (paymentRepository.existsByBookingBookingIdAndStatus(
                bookingId, PaymentStatus.SUCCESS)) {
            throw new IllegalStateException(
                    "A successful payment cannot be cancelled.");
        }

        Payment pendingCashPayment = paymentRepository
                .findByBookingBookingIdAndPaymentMethodAndStatus(
                        bookingId,
                        PaymentMethod.CASH,
                        PaymentStatus.PENDING)
                .orElse(null);

        if (pendingCashPayment != null) {
            pendingCashPayment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(pendingCashPayment);
        }
    }

    // Only the assigned worker can confirm receipt of cash.
    @Transactional
    public Map<String, Object> confirmCashPayment(
            UUID workerId,
            UUID bookingId) {

        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found."));

        if (booking.getWorker() == null
                || !booking.getWorker().getUserId().equals(workerId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to confirm this payment.");
        }

        if (booking.getStatus() != BookingStatus.WAITING_FOR_PAYMENT) {
            throw new IllegalStateException(
                    "Booking is not waiting for payment.");
        }

        Payment payment = paymentRepository
                .findByBookingBookingIdAndPaymentMethodAndStatus(
                        bookingId,
                        PaymentMethod.CASH,
                        PaymentStatus.PENDING)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No pending cash payment exists for this booking."));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now(
        settlementClock.withZone(
                SettlementBusinessDateCalculator.SETTLEMENT_ZONE)));

        paymentRepository.save(payment);
        completeBookingAfterPayment(booking);

        return paymentResult(payment);
    }

    private Booking getCustomerBooking(UUID customerId, UUID bookingId) {
        Booking booking = bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Booking not found."));

        if (!booking.getCustomer().getUserId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "You are not authorized to pay for this booking.");
        }

        return booking;
    }

    private void validatePaymentEligibility(Booking booking) {
        if (booking.getStatus() != BookingStatus.WAITING_FOR_PAYMENT) {
            throw new IllegalStateException(
                    "Booking is not waiting for payment.");
        }

        if (booking.getWorker() == null) {
            throw new IllegalStateException(
                    "No worker is assigned to this booking.");
        }
    }

    private Billing getBilling(UUID bookingId) {
        return billingRepository.findByBookingBookingId(bookingId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Billing record not found for this booking."));
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "The final bill amount is invalid.");
        }

        try {
            amount.movePointRight(2).intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalStateException(
                    "The final bill amount must have at most two decimal places.",
                    ex);
        }

        return amount;
    }

    private void completeBookingAfterPayment(Booking booking) {
        if (booking.getStatus() != BookingStatus.WAITING_FOR_PAYMENT
                && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Booking cannot be completed from its current status.");
        }

        boolean completingBooking =
                booking.getStatus() == BookingStatus.WAITING_FOR_PAYMENT;
        if (completingBooking) {
            booking.setStatus(BookingStatus.COMPLETED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.getCompletedAt() == null) {
            booking.setCompletedAt(now);
        }
        if (booking.getWorkerCompletedAt() == null) {
            booking.setWorkerCompletedAt(now);
        }

        bookingRepository.save(booking);

        List<Payment> pendingPayments =
                paymentRepository.findByBookingBookingIdAndStatus(
                        booking.getBookingId(),
                        PaymentStatus.PENDING);
        pendingPayments.forEach(
                payment -> payment.setStatus(PaymentStatus.CANCELLED));
        if (!pendingPayments.isEmpty()) {
            paymentRepository.saveAll(pendingPayments);
        }

        if (completingBooking) {
            Worker worker = workerRepository.findById(
                    booking.getWorker().getUserId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Assigned worker not found."));

            worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            workerRepository.save(worker);
        }
    }

    private Map<String, Object> paymentResult(Payment payment) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("paymentId", payment.getPaymentId());
        response.put("bookingId", payment.getBooking().getBookingId());
        response.put("paymentMethod", payment.getPaymentMethod());
        response.put("status", payment.getStatus());
        response.put("amount", payment.getAmount());
        response.put("paidAt", payment.getPaidAt());

        return response;
    }
}
