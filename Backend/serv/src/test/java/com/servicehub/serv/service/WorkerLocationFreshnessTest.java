package com.servicehub.serv.service;

import com.servicehub.serv.entity.Worker;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerLocationFreshnessTest {

    private final LocalDateTime now =
            LocalDateTime.of(2026, 11, 12, 12, 0);

    @Test
    void missingLocationIsNotFresh() {
        assertFalse(WorkerLocationFreshness.isFresh(new Worker(), now));
    }

    @Test
    void recentlyUpdatedLocationIsFresh() {
        Worker worker = workerAt(now.minusMinutes(2));

        assertTrue(WorkerLocationFreshness.isFresh(worker, now));
    }

    @Test
    void locationAtThreeMinuteBoundaryIsFresh() {
        Worker worker = workerAt(now.minusMinutes(3));

        assertTrue(WorkerLocationFreshness.isFresh(worker, now));
    }

    @Test
    void locationOlderThanThreeMinutesIsStale() {
        Worker worker = workerAt(now.minusMinutes(3).minusNanos(1));

        assertFalse(WorkerLocationFreshness.isFresh(worker, now));
    }

    @Test
    void futureLocationTimestampIsNotFresh() {
        Worker worker = workerAt(now.plusSeconds(1));

        assertFalse(WorkerLocationFreshness.isFresh(worker, now));
    }

    private Worker workerAt(LocalDateTime updatedAt) {
        Worker worker = new Worker();
        worker.setCurrentLatitude(BigDecimal.valueOf(20.2961));
        worker.setCurrentLongitude(BigDecimal.valueOf(85.8245));
        worker.setLocationLastUpdatedAt(updatedAt);
        return worker;
    }
}
