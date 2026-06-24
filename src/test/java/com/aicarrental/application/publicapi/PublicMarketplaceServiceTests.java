package com.aicarrental.application.publicapi;

import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.infrastructure.persistence.InsurancePackageRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PublicMarketplaceServiceTests {
    @Mock VehicleRepository vehicleRepository;
    @Mock InsurancePackageRepository insurancePackageRepository;
    private PublicMarketplaceService service;

    @BeforeEach
    void setUp() {
        service = new PublicMarketplaceService(vehicleRepository, insurancePackageRepository);
    }

    @Test
    void searchRejectsInvertedPriceRange() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(2);
        assertThrows(BusinessException.class, () -> service.search(
                pickup,
                pickup.plusDays(2),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(2000),
                null, null, null, null, null, null, null, null, null,
                "recommended", 0, 12
        ));
    }

    @Test
    void searchRejectsPastPickupDate() {
        LocalDateTime pickup = LocalDateTime.now().minusHours(1);
        assertThrows(BusinessException.class, () -> service.search(
                pickup,
                pickup.plusDays(2),
                null, null, null, null, null, null, null, null, null, null, null,
                "recommended", 0, 12
        ));
    }
}
