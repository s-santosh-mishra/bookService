package com.servicehub.serv.repository;

import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.entity.WorkerServiceId;
import com.servicehub.serv.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

public interface WorkerServiceRepository
        extends JpaRepository<WorkerService, WorkerServiceId> {
                List<WorkerService> findByWorker_UserId(UUID workerId);
                List<WorkerService> findByService_ServiceId(UUID serviceId);

                @Query(value = """
                        SELECT w.*
                        FROM workers w
                        JOIN users u ON u.user_id = w.user_id
                        JOIN worker_services ws ON ws.worker_id = w.user_id
                        WHERE ws.service_id = :serviceId
                          AND u.is_active = TRUE
                          AND w.verification_status = 'VERIFIED'
                          AND w.availability_status = 'AVAILABLE'
                          AND w.current_latitude IS NOT NULL
                          AND w.current_longitude IS NOT NULL
                          AND w.location_last_updated_at >= :freshAfter
                          AND w.location_last_updated_at <= :locationNow
                          AND w.current_latitude BETWEEN :minLatitude AND :maxLatitude
                          AND w.current_longitude BETWEEN :minLongitude AND :maxLongitude
                          AND NOT EXISTS (
                              SELECT 1
                              FROM bookings active_booking
                              WHERE active_booking.worker_id = w.user_id
                                AND active_booking.status IN (
                                    'ACCEPTED', 'IN_PROGRESS', 'WAITING_FOR_PAYMENT'
                                )
                          )
                          AND NOT EXISTS (
                              SELECT 1
                              FROM booking_offers existing_offer
                              WHERE existing_offer.booking_id = :bookingId
                                AND existing_offer.worker_id = w.user_id
                          )
                          AND NOT EXISTS (
                              SELECT 1
                              FROM worker_booking_rejections rejection
                              WHERE rejection.booking_id = :bookingId
                                AND rejection.worker_id = w.user_id
                          )
                        FOR UPDATE OF w
                        """, nativeQuery = true)
                List<Worker> findEligibleWorkersForOffer(
                                @Param("bookingId") UUID bookingId,
                                @Param("serviceId") UUID serviceId,
                                @Param("freshAfter") LocalDateTime freshAfter,
                                @Param("locationNow") LocalDateTime locationNow,
                                @Param("minLatitude") double minLatitude,
                                @Param("maxLatitude") double maxLatitude,
                                @Param("minLongitude") double minLongitude,
                                @Param("maxLongitude") double maxLongitude);
}