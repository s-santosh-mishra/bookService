package com.servicehub.serv.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementBusinessDateCalculatorTest {

    @Test
    void assignsPaymentTimesAroundCutoffToTheCorrectBusinessDate() {
        SettlementBusinessDateCalculator calculator =
                calculatorAt("2026-11-11T15:00:00Z");
        LocalDate paymentDate = LocalDate.of(2026, 11, 11);

        assertEquals(
                paymentDate,
                calculator.businessDate(
                        LocalDateTime.of(2026, 11, 11, 19, 59, 59)));
        assertEquals(
                paymentDate.plusDays(1),
                calculator.businessDate(
                        LocalDateTime.of(2026, 11, 11, 20, 0)));
        assertEquals(
                paymentDate.plusDays(1),
                calculator.businessDate(
                        LocalDateTime.of(2026, 11, 11, 20, 0, 1)));
        assertEquals(
                LocalDate.of(2026, 11, 12),
                calculator.businessDate(
                        LocalDateTime.of(2026, 11, 12, 0, 0)));
    }

    @Test
    void closesTodaysBatchAtTheExactEightPmIstInstant() {
        LocalDate today = LocalDate.of(2026, 11, 11);
        assertFalse(calculatorAt("2026-11-11T14:29:59Z").isClosed(today));
        assertTrue(calculatorAt("2026-11-11T14:30:00Z").isClosed(today));
        assertTrue(calculatorAt("2026-11-11T14:30:01Z").isClosed(today));
    }

    @Test
    void convertsClockInstantsToIstAcrossMidnightAndKeepsOlderBatchesClosed() {
        SettlementBusinessDateCalculator calculator =
                calculatorAt("2026-11-11T18:30:00Z");

        assertEquals(
                LocalDateTime.of(2026, 11, 12, 0, 0),
                calculator.now().toLocalDateTime());
        assertTrue(calculator.isClosed(LocalDate.of(2026, 11, 11)));
        assertFalse(calculator.isClosed(LocalDate.of(2026, 11, 12)));
        assertFalse(calculator.isClosed(LocalDate.of(2026, 11, 13)));
    }

    private SettlementBusinessDateCalculator calculatorAt(String instant) {
        return new SettlementBusinessDateCalculator(
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }
}
