-- Manual migration for the existing worker-location and booking-discovery modules.
-- Do not run automatically; apply only after backing up the target database.

ALTER TABLE workers
    ADD COLUMN IF NOT EXISTS current_latitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS current_longitude NUMERIC(10, 7),
    ADD COLUMN IF NOT EXISTS location_last_updated_at TIMESTAMP WITHOUT TIME ZONE;

CREATE TABLE IF NOT EXISTS booking_offers (
    offer_id UUID NOT NULL PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (booking_id),
    worker_id UUID NOT NULL REFERENCES workers (user_id),
    offered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_booking_offer_booking_worker UNIQUE (booking_id, worker_id)
);

CREATE INDEX IF NOT EXISTS idx_booking_offers_worker
    ON booking_offers (worker_id, booking_id);

CREATE TABLE IF NOT EXISTS worker_booking_rejections (
    rejection_id UUID NOT NULL PRIMARY KEY,
    worker_id UUID NOT NULL REFERENCES workers (user_id),
    booking_id UUID NOT NULL REFERENCES bookings (booking_id),
    rejected_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_worker_booking_rejection UNIQUE (worker_id, booking_id)
);
