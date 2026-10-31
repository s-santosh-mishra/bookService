
package com.servicehub.serv.service;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingReviewDecision;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerReview;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.BookingReviewDecisionRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerReviewRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkerReviewService {

    private final BookingRepository bookingRepository;
    private final WorkerReviewRepository workerReviewRepository;
    private final BookingReviewDecisionRepository decisionRepository;
    private final WorkerRepository workerRepository;

    private static final Set<String> ALLOWED_TAGS = Set.of(
            "Punctual",
            "Knowledgeable",
            "Professional",
            "Polite & respectful",
            "Quality work",
            "Good communication");

    public WorkerReviewService(
            BookingRepository bookingRepository,
            WorkerReviewRepository workerReviewRepository,
            BookingReviewDecisionRepository decisionRepository,
            WorkerRepository workerRepository) {

        this.bookingRepository = bookingRepository;
        this.workerReviewRepository = workerReviewRepository;
        this.decisionRepository = decisionRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional
    public void submitReview(
            UUID customerId,
            UUID bookingId,
            int rating,
            List<String> feedbackTags) {

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5.");
        }

        Booking booking = getEligibleBooking(customerId, bookingId);

        if (decisionRepository.existsByBookingId(bookingId)) {
            throw new IllegalStateException(
                    "A review decision has already been made for this booking.");
        }

        List<String> tags = feedbackTags == null
                ? List.of()
                : feedbackTags.stream().distinct().toList();

        if (!ALLOWED_TAGS.containsAll(tags)) {
            throw new IllegalArgumentException(
                    "One or more feedback tags are invalid.");
        }

        Worker worker = booking.getWorker();

        WorkerReview review = new WorkerReview();
        review.setBookingId(booking.getBookingId());
        review.setCustomerId(booking.getCustomer().getUserId());
        review.setWorkerId(worker.getUserId());
        review.setRating(rating);
        review.setFeedbackTags(tags.toArray(new String[0]));

        workerReviewRepository.save(review);

        BookingReviewDecision decision = new BookingReviewDecision();
        decision.setBookingId(booking.getBookingId());
        decision.setDecision("SUBMITTED");

        decisionRepository.save(decision);

        updateWorkerRating(worker.getUserId());

        booking.setStatus(BookingStatus.REVIEWED);
        bookingRepository.save(booking);
    }

    @Transactional
    public void skipReview(UUID customerId, UUID bookingId) {

        Booking booking = getEligibleBooking(customerId, bookingId);

        if (decisionRepository.existsByBookingId(bookingId)) {
            throw new IllegalStateException(
                    "A review decision has already been made for this booking.");
        }

        BookingReviewDecision decision = new BookingReviewDecision();
        decision.setBookingId(booking.getBookingId());
        decision.setDecision("SKIPPED");

        decisionRepository.save(decision);

        booking.setStatus(BookingStatus.REVIEWED);
        bookingRepository.save(booking);
    }

    public Map<UUID, String> getCustomerReviewDecisions(UUID customerId) {

        List<Booking> customerBookings = bookingRepository.findByCustomerUserId(customerId);

        List<UUID> completedBookingIds = customerBookings.stream()
                .filter(booking ->
                        booking.getStatus() == BookingStatus.COMPLETED
                        || booking.getStatus() == BookingStatus.REVIEWED)
                .map(booking -> booking.getBookingId())
                .toList();

        Map<UUID, String> decisions = new HashMap<>();

        decisionRepository.findAllById(completedBookingIds)
                .forEach(decision -> decisions.put(
                        decision.getBookingId(),
                        decision.getDecision()));

        return decisions;
    }

    private Booking getEligibleBooking(UUID customerId, UUID bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found."));

        if (!booking.getCustomer().getUserId().equals(customerId)) {
            throw new SecurityException(
                    "You are not authorized to review this booking.");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only completed bookings can be reviewed.");
        }

        if (booking.getWorker() == null) {
            throw new IllegalStateException(
                    "This booking does not have an assigned worker.");
        }

        return booking;
    }

    private void updateWorkerRating(UUID workerId) {

        List<WorkerReview> reviews = workerReviewRepository.findByWorkerId(workerId);

        if (reviews.isEmpty()) {
            return;
        }

        double totalReviewRating = reviews.stream()
                .mapToInt(review -> review.getRating())
                .sum();

        long reviewCount = reviews.size();

        // Weighted baseline: 4.0 rating with a weight of 10 reviews.
        BigDecimal weightedRating = BigDecimal.valueOf(
                (40.0 + totalReviewRating) / (10.0 + reviewCount));

        BigDecimal roundedRating = weightedRating.setScale(
                1, RoundingMode.HALF_UP);

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Worker not found."));

        worker.setRating(roundedRating);
        workerRepository.save(worker);
    }
}
