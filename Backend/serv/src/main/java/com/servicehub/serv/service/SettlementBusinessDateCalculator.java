package com.servicehub.serv.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Component
public class SettlementBusinessDateCalculator {

    public static final ZoneId SETTLEMENT_ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalTime CUTOFF_TIME = LocalTime.of(20, 0);

    private final Clock clock;

    public SettlementBusinessDateCalculator(Clock settlementClock) {
        this.clock = Objects.requireNonNull(settlementClock);
    }

    public LocalDate businessDate(LocalDateTime paidAt) {
        Objects.requireNonNull(paidAt, "Payment paidAt cannot be null.");
        ZonedDateTime paymentTime = paidAt.atZone(SETTLEMENT_ZONE);
        LocalDate paymentDate = paymentTime.toLocalDate();
        return paymentTime.toLocalTime().isBefore(CUTOFF_TIME)
                ? paymentDate
                : paymentDate.plusDays(1);
    }

    public ZonedDateTime now() {
        return ZonedDateTime.now(clock.withZone(SETTLEMENT_ZONE));
    }

    public boolean isClosed(LocalDate businessDate, ZonedDateTime decisionTime) {
        Objects.requireNonNull(businessDate);
        ZonedDateTime settlementTime = decisionTime.withZoneSameInstant(SETTLEMENT_ZONE);
        LocalDate today = settlementTime.toLocalDate();
        if (businessDate.isBefore(today)) {
            return true;
        }
        if (businessDate.isAfter(today)) {
            return false;
        }
        return !settlementTime.toLocalTime().isBefore(CUTOFF_TIME);
    }

    public boolean isClosed(LocalDate businessDate) {
        return isClosed(businessDate, now());
    }
}
