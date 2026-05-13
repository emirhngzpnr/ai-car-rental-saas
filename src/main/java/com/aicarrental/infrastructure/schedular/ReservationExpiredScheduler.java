package com.aicarrental.infrastructure.schedular;

import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiredScheduler {
    private static final int PAYMENT_TIMEOUT_MINUTES = 5;

    private final ReservationRepository reservationRepository;
    private final AuditEventPublisher auditEventPublisher;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expirePendingPaymentReservations() {

        LocalDateTime expirationThreshold =
                LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        List<Reservation> expiredReservations =
                reservationRepository.findByStatusAndActiveTrueAndCreatedAtBefore(
                        ReservationStatus.PENDING_PAYMENT,
                        expirationThreshold
                );

        if (expiredReservations.isEmpty()) {
            return;
        }

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setActive(false);
            reservation.setUpdatedAt(LocalDateTime.now());

            auditEventPublisher.publish(new AuditEvent(
                    null,
                    "SYSTEM",
                    "SYSTEM",
                    reservation.getTenant() != null ? reservation.getTenant().getId() : null,
                    AuditAction.RESERVATION_UPDATED,
                    "Reservation",
                    reservation.getId(),
                    "Reservation expired because payment was not completed within "
                            + PAYMENT_TIMEOUT_MINUTES + " minutes"
            ));
        }

        reservationRepository.saveAll(expiredReservations);

        log.info("Expired {} pending payment reservations", expiredReservations.size());
    }
}
