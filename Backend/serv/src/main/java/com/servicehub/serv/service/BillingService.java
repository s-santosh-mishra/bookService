package com.servicehub.serv.service;

import com.servicehub.serv.entity.Billing;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.repository.BillingRepository;
import com.servicehub.serv.repository.BookingPartRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BillingService {

    private static final BigDecimal AUTO_COMPLETION_FEE =
            BigDecimal.valueOf(100.00);

    private static final BigDecimal CANCELLATION_FEE_PER_KM =
            BigDecimal.valueOf(5.00);

    private final BillingRepository billingRepository;
    private final BookingPartRepository bookingPartRepository;

    public BillingService(
            BillingRepository billingRepository,
            BookingPartRepository bookingPartRepository) {

        this.billingRepository = billingRepository;
        this.bookingPartRepository = bookingPartRepository;
    }

    @Transactional
    public Billing createBilling(Booking booking) {

        if (billingRepository.existsByBookingBookingId(
                booking.getBookingId())) {

            return billingRepository
                    .findByBookingBookingId(booking.getBookingId())
                    .orElseThrow();
        }

        BigDecimal hourlyRate =
                booking.getService().getBasePricePerHour();

        BigDecimal minimumFee =
                booking.getService().getMinimumServiceFee();

        Worker worker = booking.getWorker();

        if (worker == null) {
            throw new IllegalStateException(
                    "Cannot create billing without an assigned worker.");
        }

        BigDecimal ratingMultiplier =
                getRatingMultiplier(worker.getRating());

        LocalDateTime startTime = booking.getStartedAt();
        LocalDateTime endTime = booking.getCompletedAt();

        if (startTime == null || endTime == null) {
            throw new IllegalStateException(
                    "Cannot create billing without valid service start and completion times.");
        }

        BigDecimal billableHours =
                calculateBillableHours(startTime, endTime);

        BigDecimal labourCharge =
                calculateLabourCharge(
                        hourlyRate,
                        minimumFee,
                        billableHours,
                        ratingMultiplier);

        /*
         * Worker cannot complete → 30% labour reduction
         */
        if (booking.getStatus() ==
                BookingStatus.WORKER_CANNOT_COMPLETE) {

            labourCharge = labourCharge
                    .multiply(BigDecimal.valueOf(0.70))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal travelCharge =
                calculateTravelCharge(booking);

        BigDecimal partsCost =
                calculatePartsCost(booking.getBookingId());

        BigDecimal additionalFees =
                BigDecimal.ZERO;

        if (booking.getStatus() ==
                BookingStatus.AUTO_COMPLETED) {

            additionalFees = AUTO_COMPLETION_FEE;
        }

        BigDecimal finalBilledPrice =
                labourCharge
                        .add(travelCharge)
                        .add(partsCost)
                        .add(additionalFees)
                        .setScale(2, RoundingMode.HALF_UP);

        Billing billing = new Billing();

        billing.setBooking(booking);
        billing.setBasePricePerHour(hourlyRate);
        billing.setMinimumServiceFee(minimumFee);
        billing.setWorkerRating(worker.getRating());
        billing.setBillableHours(billableHours);
        billing.setLabourCharge(labourCharge);
        billing.setTravelCharge(travelCharge);
        billing.setPartsCost(partsCost);
        billing.setAdditionalFees(additionalFees);
        billing.setFinalBilledPrice(finalBilledPrice);

        return billingRepository.save(billing);
    }

    /*
     * Customer cancellation billing.
     *
     * ACCEPTED:
     * - Within 5 minutes of acceptance → no charge
     * - After 5 minutes → distance × ₹5/km
     *
     * IN_PROGRESS:
     * - Actual labour
     * - Travel
     * - Approved parts
     */
    @Transactional
    public Billing createCustomerCancellationBilling(
            Booking booking,
            LocalDateTime cancellationTime) {

        if (billingRepository.existsByBookingBookingId(
                booking.getBookingId())) {

            return billingRepository
                    .findByBookingBookingId(booking.getBookingId())
                    .orElseThrow();
        }

        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cancellation billing can only be created for cancelled bookings.");
        }

        Worker worker = booking.getWorker();

        if (worker == null) {
            throw new IllegalStateException(
                    "Cannot create cancellation billing without an assigned worker.");
        }

        BigDecimal hourlyRate =
                booking.getService().getBasePricePerHour();

        BigDecimal minimumFee =
                booking.getService().getMinimumServiceFee();

        BigDecimal workerRating =
                worker.getRating();

        BigDecimal labourCharge = BigDecimal.ZERO;
        BigDecimal travelCharge = BigDecimal.ZERO;
        BigDecimal partsCost = BigDecimal.ZERO;
        BigDecimal additionalFees = BigDecimal.ZERO;
        BigDecimal billableHours = BigDecimal.ZERO;

        /*
         * Cancellation after worker acceptance
         */
        if (booking.getStartedAt() == null) {

            LocalDateTime acceptedAt =
                    booking.getAcceptedAt();

            if (acceptedAt == null) {
                throw new IllegalStateException(
                        "Cannot calculate cancellation fee without acceptance time.");
            }

            long minutesSinceAcceptance =
                    Duration.between(
                            acceptedAt,
                            cancellationTime)
                            .toMinutes();

            /*
             * Within 5 minutes → no cancellation charge.
             */
            if (minutesSinceAcceptance > 5) {

                additionalFees =
                        calculateCancellationFee(booking);
            }
        }

        /*
         * Cancellation after work has started
         */
        else {

            billableHours =
                    calculateBillableHours(
                            booking.getStartedAt(),
                            cancellationTime);

            BigDecimal ratingMultiplier =
                    getRatingMultiplier(workerRating);

            labourCharge =
                    calculateLabourCharge(
                            hourlyRate,
                            minimumFee,
                            billableHours,
                            ratingMultiplier);

            travelCharge =
                    calculateTravelCharge(booking);

            partsCost =
                    calculatePartsCost(
                            booking.getBookingId());
        }

        BigDecimal finalBilledPrice =
                labourCharge
                        .add(travelCharge)
                        .add(partsCost)
                        .add(additionalFees)
                        .setScale(2, RoundingMode.HALF_UP);

        Billing billing = new Billing();

        billing.setBooking(booking);
        billing.setBasePricePerHour(hourlyRate);
        billing.setMinimumServiceFee(minimumFee);
        billing.setWorkerRating(workerRating);
        billing.setBillableHours(billableHours);
        billing.setLabourCharge(labourCharge);
        billing.setTravelCharge(travelCharge);
        billing.setPartsCost(partsCost);
        billing.setAdditionalFees(additionalFees);
        billing.setFinalBilledPrice(finalBilledPrice);

        return billingRepository.save(billing);
    }

    private BigDecimal calculateBillableHours(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (startTime == null || endTime == null) {
            throw new IllegalStateException(
                    "Cannot calculate billable hours without valid timestamps.");
        }

        long minutes =
                Duration.between(startTime, endTime).toMinutes();

        if (minutes < 0) {
            throw new IllegalStateException(
                    "End time cannot be before start time.");
        }

        /*
         * Round up to the next 15-minute block.
         */
        long billableBlocks =
                (long) Math.ceil(minutes / 15.0);

        return BigDecimal.valueOf(billableBlocks)
                .multiply(BigDecimal.valueOf(15))
                .divide(
                        BigDecimal.valueOf(60),
                        2,
                        RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLabourCharge(
            BigDecimal hourlyRate,
            BigDecimal minimumFee,
            BigDecimal billableHours,
            BigDecimal ratingMultiplier) {

        BigDecimal labourCharge =
                hourlyRate
                        .multiply(billableHours)
                        .multiply(ratingMultiplier)
                        .setScale(2, RoundingMode.HALF_UP);

        /*
         * Minimum service fee.
         */
        if (labourCharge.compareTo(minimumFee) < 0) {
            labourCharge = minimumFee;
        }

        return labourCharge;
    }

    private BigDecimal getRatingMultiplier(
            BigDecimal rating) {

        if (rating.compareTo(
                BigDecimal.valueOf(3.5)) >= 0) {

            return BigDecimal.ONE;
        }

        if (rating.compareTo(
                BigDecimal.valueOf(2.0)) >= 0) {

            return BigDecimal.valueOf(0.95);
        }

        if (rating.compareTo(
                BigDecimal.valueOf(0.5)) >= 0) {

            return BigDecimal.valueOf(0.90);
        }

        return BigDecimal.valueOf(0.90);
    }

    private BigDecimal calculateTravelCharge(
            Booking booking) {

        if (booking.getServiceLatitude() == null
                || booking.getServiceLongitude() == null
                || booking.getWorkerAcceptanceLatitude() == null
                || booking.getWorkerAcceptanceLongitude() == null) {

            throw new IllegalStateException(
                    "Cannot calculate travel charge without valid location data.");
        }

        BigDecimal customerLatitude =
                booking.getServiceLatitude();

        BigDecimal customerLongitude =
                booking.getServiceLongitude();

        BigDecimal workerLatitude =
                booking.getWorkerAcceptanceLatitude();

        BigDecimal workerLongitude =
                booking.getWorkerAcceptanceLongitude();

        double earthRadiusKm = 6371.0;

        double lat1 =
                Math.toRadians(
                        customerLatitude.doubleValue());

        double lon1 =
                Math.toRadians(
                        customerLongitude.doubleValue());

        double lat2 =
                Math.toRadians(
                        workerLatitude.doubleValue());

        double lon2 =
                Math.toRadians(
                        workerLongitude.doubleValue());

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(deltaLon / 2)
                        * Math.sin(deltaLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a));

        double distanceKm =
                earthRadiusKm * c;

        /*
         * First 3 km are included in normal travel billing.
         */
        double chargeableDistance =
                Math.max(0, distanceKm - 3.0);

        return BigDecimal.valueOf(chargeableDistance)
                .multiply(BigDecimal.valueOf(10.00))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /*
     * Customer cancellation fee after 5 minutes of acceptance.
     *
     * Unlike normal travel billing, there is NO 3 km free allowance.
     * The rule is simply:
     *
     * distance × ₹5/km
     */
    private BigDecimal calculateCancellationFee(
            Booking booking) {

        if (booking.getServiceLatitude() == null
                || booking.getServiceLongitude() == null
                || booking.getWorkerAcceptanceLatitude() == null
                || booking.getWorkerAcceptanceLongitude() == null) {

            throw new IllegalStateException(
                    "Cannot calculate cancellation fee without valid location data.");
        }

        double earthRadiusKm = 6371.0;

        double lat1 =
                Math.toRadians(
                        booking.getServiceLatitude()
                                .doubleValue());

        double lon1 =
                Math.toRadians(
                        booking.getServiceLongitude()
                                .doubleValue());

        double lat2 =
                Math.toRadians(
                        booking.getWorkerAcceptanceLatitude()
                                .doubleValue());

        double lon2 =
                Math.toRadians(
                        booking.getWorkerAcceptanceLongitude()
                                .doubleValue());

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(deltaLon / 2)
                        * Math.sin(deltaLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a));

        double distanceKm =
                earthRadiusKm * c;

        return BigDecimal.valueOf(distanceKm)
                .multiply(CANCELLATION_FEE_PER_KM)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePartsCost(
            UUID bookingId) {

        return bookingPartRepository
                .findByBookingBookingId(bookingId)
                .stream()
                .map(bookingPart ->
                        bookingPart.getTotalPrice())
                .reduce(
                        BigDecimal.ZERO,
                        (total, price) -> total.add(price));
    }
}