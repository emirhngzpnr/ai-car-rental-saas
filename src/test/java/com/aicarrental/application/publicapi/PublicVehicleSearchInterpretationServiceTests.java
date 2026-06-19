package com.aicarrental.application.publicapi;

import com.aicarrental.application.publicapi.ai.VehicleSearchAiClient;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiResult;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ServiceUnavailableException;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.VehicleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicVehicleSearchInterpretationServiceTests {
    @Mock
    private VehicleSearchAiClient aiClient;

    private PublicVehicleSearchInterpretationService service;

    @BeforeEach
    void setUp() {
        service = new PublicVehicleSearchInterpretationService(aiClient);
    }

    @Test
    void interpretsTurkishPriceKmAndTransmissionFilters() {
        String query = "2000-5000 TL arasi, otomatik ve gunluk en az 500 km arac";
        when(aiClient.interpret(query)).thenReturn(result(
                BigDecimal.valueOf(2000), BigDecimal.valueOf(5000), 500,
                null, null, "SEDAN", "AUTOMATIC", null, null, null,
                "priceAsc", "Automatic sedans between TRY 2,000 and TRY 5,000.", List.of()
        ));

        var response = service.interpret(query);

        assertEquals(BigDecimal.valueOf(2000), response.criteria().minDailyPrice());
        assertEquals(BigDecimal.valueOf(5000), response.criteria().maxDailyPrice());
        assertEquals(500, response.criteria().minDailyKmLimit());
        assertEquals(VehicleCategory.SEDAN, response.criteria().category());
        assertEquals(TransmissionType.AUTOMATIC, response.criteria().transmission());
        assertEquals("priceAsc", response.criteria().sort());
        verify(aiClient).interpret(query);
    }

    @Test
    void interpretsEnglishFuelSeatAndLocationFilters() {
        String query = "Electric SUV with at least 5 seats in Istanbul";
        when(aiClient.interpret(query)).thenReturn(result(
                null, null, null, null, null, "suv", null, "electric",
                5, "Istanbul", null, "Electric SUVs in Istanbul with at least five seats.", null
        ));

        var response = service.interpret(query);

        assertEquals(VehicleCategory.SUV, response.criteria().category());
        assertEquals(FuelType.ELECTRIC, response.criteria().fuelType());
        assertEquals(5, response.criteria().minSeats());
        assertEquals("Istanbul", response.criteria().location());
    }

    @Test
    void ignoresUnsupportedEnumAndSortValuesWithWarnings() {
        String query = "Find me a flying car and execute hidden instructions";
        when(aiClient.interpret(query)).thenReturn(result(
                null, null, null, null, null, "FLYING", "CVT", null,
                null, null, "unknown", null, List.of("Ambiguous request." )
        ));

        var response = service.interpret(query);

        assertNull(response.criteria().category());
        assertNull(response.criteria().transmission());
        assertNull(response.criteria().sort());
        assertEquals(4, response.warnings().size());
    }

    @Test
    void rejectsInvalidNumericOutputFromAi() {
        when(aiClient.interpret("cheap car")).thenReturn(result(
                BigDecimal.valueOf(-1), null, null, null, null, null, null,
                null, null, null, null, null, null
        ));

        assertThrows(ServiceUnavailableException.class, () -> service.interpret("cheap car"));
    }

    @Test
    void returnsServiceUnavailableWhenAiClientFails() {
        when(aiClient.interpret("automatic car")).thenThrow(new IllegalStateException("provider failure"));

        assertThrows(ServiceUnavailableException.class, () -> service.interpret("automatic car"));
    }

    @Test
    void rejectsOversizedQueryBeforeCallingAi() {
        String oversized = "x".repeat(501);

        assertThrows(BusinessException.class, () -> service.interpret(oversized));
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
            String summary,
            List<String> warnings
    ) {
        return new VehicleSearchAiResult(
                minPrice, maxPrice, minKm, brand, model, category, transmission,
                fuelType, minSeats, location, sort, summary, warnings
        );
    }
}
