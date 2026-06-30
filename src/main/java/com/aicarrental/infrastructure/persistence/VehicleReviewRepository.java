package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.review.VehicleReview;
import com.aicarrental.infrastructure.persistence.projection.VehicleReviewSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleReviewRepository extends JpaRepository<VehicleReview, Long> {
    boolean existsByReservation_Id(Long reservationId);

    boolean existsByReservation_IdAndActiveTrue(Long reservationId);

    Optional<VehicleReview> findByReservation_IdAndCustomerAccount_IdAndActiveTrue(
            Long reservationId,
            Long customerAccountId
    );

    @EntityGraph(attributePaths = {"customerAccount"})
    @Query("""
        SELECT vr
        FROM VehicleReview vr
        WHERE vr.vehicle.id = :vehicleId
          AND vr.active = true
          AND vr.vehicle.active = true
          AND vr.vehicle.tenant.active = true
        ORDER BY vr.createdAt DESC
        """)
    Page<VehicleReview> findPublicReviewsByVehicleId(
            @Param("vehicleId") Long vehicleId,
            Pageable pageable
    );

    @Query("""
        SELECT
            vr.vehicle.id AS vehicleId,
            AVG(vr.rating) AS averageRating,
            COUNT(vr.id) AS reviewCount
        FROM VehicleReview vr
        WHERE vr.vehicle.id IN :vehicleIds
          AND vr.active = true
        GROUP BY vr.vehicle.id
        """)
    List<VehicleReviewSummaryProjection> summarizeByVehicleIds(
            @Param("vehicleIds") List<Long> vehicleIds
    );
}
