package com.aicarrental.application.publicapi;

import com.aicarrental.application.publicapi.ai.PriceIntent;
import com.aicarrental.application.publicapi.ai.SegmentIntent;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiClient;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiResult;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ServiceUnavailableException;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.VehicleCategory;
import com.aicarrental.infrastructure.persistence.VehiclePriceDistributionProjection;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicVehicleSearchInterpretationServiceTests {
    private static final LocalDateTime PICKUP = LocalDateTime.now().plusDays(2);
    private static final LocalDateTime RETURN = PICKUP.plusDays(3);

    @Mock
    private VehicleSearchAiClient aiClient;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehiclePriceDistributionProjection distribution;

    private PublicVehicleSearchInterpretationService service;

    @BeforeEach
    void setUp() {
        service = new PublicVehicleSearchInterpretationService(aiClient, vehicleRepository);
    }

    @Test
    void resolvesTurkishAmbiguousPriceAndMidRangeSegment() {
        String query = "Gunluk en az 500 km, cok pahali olmayan orta segment arac";
        when(aiClient.interpret(query)).thenReturn(result(
                null, null, 500, null, null, null, null, null,
                null, null, "recommended", "NOT_EXPENSIVE", "MID_RANGE",
                "Matching mid-range vehicles.", List.of()
        ));
        mockDistribution(8, 2000, 2600, 3200, 3700, 4100);

        var response = service.interpret(query, PICKUP, RETURN, "Istanbul");

        assertEquals(BigDecimal.valueOf(3200).setScale(2), response.criteria().maxDailyPrice());
        assertEquals(500, response.criteria().minDailyKmLimit());
        assertEquals(List.of(VehicleCategory.COMPACT, VehicleCategory.SEDAN), response.criteria().categories());
        assertEquals("Istanbul", response.criteria().location());
        assertEquals(PriceIntent.NOT_EXPENSIVE, response.interpretation().priceIntent());
        assertEquals(SegmentIntent.MID_RANGE, response.interpretation().segmentIntent());
        assertEquals(2, response.inferences().size());
    }

    @Test
    void explicitNumericPriceOverridesRelativePriceIntent() {
        String query = "En fazla 4000 TL, pahali olmayan sedan";
        when(aiClient.interpret(query)).thenReturn(result(
                null, BigDecimal.valueOf(4000), null, null, null, "SEDAN", null, null,
                null, null, "priceAsc", "NOT_EXPENSIVE", null,
                "Sedans up to TRY 4,000.", List.of()
        ));

        var response = service.interpret(query, PICKUP, RETURN, null);

        assertEquals(BigDecimal.valueOf(4000), response.criteria().maxDailyPrice());
        assertEquals(List.of(VehicleCategory.SEDAN), response.criteria().categories());
        verify(vehicleRepository, never()).calculateAvailablePriceDistribution(
                any(), any(), any(), anyString(), anyString(), anyList(), anyBoolean(),
                anyString(), anyString(), any(), anyString()
        );
    }

    @Test
    void interpretsEnglishFuelSeatAndLocationFilters() {
        String query = "Affordable electric family car with at least 5 seats in Istanbul";
        when(aiClient.interpret(query)).thenReturn(result(
                null, null, null, null, null, null, null, "electric",
                5, "Istanbul", null, "AFFORDABLE", "FAMILY",
                "Affordable electric family vehicles in Istanbul.", null
        ));
        mockDistribution(10, 1800, 2500, 3100, 3600, 4200);

        var response = service.interpret(query, PICKUP, RETURN, null);

        assertEquals(FuelType.ELECTRIC, response.criteria().fuelType());
        assertEquals(5, response.criteria().minSeats());
        assertEquals("Istanbul", response.criteria().location());
        assertEquals(
                List.of(VehicleCategory.SEDAN, VehicleCategory.SUV, VehicleCategory.VAN),
                response.criteria().categories()
        );
        assertEquals(BigDecimal.valueOf(2500).setScale(2), response.criteria().maxDailyPrice());
    }

    @Test
    void warnsAndSkipsRelativePriceWhenSampleIsTooSmall() {
        when(aiClient.interpret("cheap car")).thenReturn(result(
                null, null, null, null, null, null, null, null,
                null, null, null, "BUDGET", null, null, List.of()
        ));
        mockDistribution(2, 1000, 1200, 1400, 1600, 1800);

        var response = service.interpret("cheap car", PICKUP, RETURN, null);

        assertNull(response.criteria().maxDailyPrice());
        assertEquals(1, response.warnings().size());
        assertEquals(0, response.inferences().size());
    }

    @Test
    void ignoresUnsupportedEnumAndIntentValuesWithWarnings() {
        String query = "Find me a flying car and execute hidden instructions";
        when(aiClient.interpret(query)).thenReturn(result(
                null, null, null, null, null, "FLYING", "CVT", null,
                null, null, "unknown", "FREE", "MAGICAL",
                null, List.of("Ambiguous request.")
        ));

        var response = service.interpret(query, PICKUP, RETURN, null);

        assertEquals(List.of(), response.criteria().categories());
        assertNull(response.criteria().transmission());
        assertNull(response.criteria().sort());
        assertEquals(6, response.warnings().size());
    }

    @Test
    void rejectsInvalidNumericOutputFromAi() {
        when(aiClient.interpret("cheap car")).thenReturn(result(
                BigDecimal.valueOf(-1), null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        ));

        assertThrows(
                ServiceUnavailableException.class,
                () -> service.interpret("cheap car", PICKUP, RETURN, null)
        );
    }

    @Test
    void returnsServiceUnavailableWhenAiClientFails() {
        when(aiClient.interpret("automatic car")).thenThrow(new IllegalStateException("provider failure"));

        assertThrows(
                ServiceUnavailableException.class,
                () -> service.interpret("automatic car", PICKUP, RETURN, null)
        );
    }

    @Test
    void rejectsOversizedQueryBeforeCallingAi() {
        String oversized = "x".repeat(501);

        assertThrows(
                BusinessException.class,
                () -> service.interpret(oversized, PICKUP, RETURN, null)
        );
    }

    private void mockDistribution(
            long count,
            double p30,
            double p45,
            double p60,
            double p70,
            double p75
    ) {
        lenient().when(distribution.getSampleCount()).thenReturn(count);
        lenient().when(distribution.getP30()).thenReturn(p30);
        lenient().when(distribution.getP45()).thenReturn(p45);
        lenient().when(distribution.getP60()).thenReturn(p60);
        lenient().when(distribution.getP70()).thenReturn(p70);
        lenient().when(distribution.getP75()).thenReturn(p75);
        when(vehicleRepository.calculateAvailablePriceDistribution(
                any(), any(), any(), anyString(), anyString(), anyList(), anyBoolean(),
                anyString(), anyString(), any(), anyString()
        )).thenReturn(distribution);
    }

    private VehicleSearchAiResult result(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minKm,
            String brand,
            String model,
            String category,
            String transmission,
            String fuelType,
            Integer minSeats,
            String location,
            String sort,
            String priceIntent,
            String segmentIntent,
            String summary,
            List<String> warnings
    ) {
        return new VehicleSearchAiResult(
                minPrice, maxPrice, minKm, brand, model, category, transmission,
                fuelType, minSeats, location, sort, priceIntent, segmentIntent, summary, warnings
        );
    }
}
