package com.servicehub.serv.service;

import com.servicehub.serv.entity.Worker;

import java.time.LocalDateTime;

public final class WorkerLocationFreshness {

    private WorkerLocationFreshness() {
    }

    public static boolean isFresh(Worker worker, LocalDateTime now) {
        if (worker == null
                || worker.getCurrentLatitude() == null
                || worker.getCurrentLongitude() == null
                || worker.getLocationLastUpdatedAt() == null
                || now == null) {
            return false;
        }

        LocalDateTime updatedAt = worker.getLocationLastUpdatedAt();
        return !updatedAt.isBefore(now.minusMinutes(3))
                && !updatedAt.isAfter(now);
    }
}
