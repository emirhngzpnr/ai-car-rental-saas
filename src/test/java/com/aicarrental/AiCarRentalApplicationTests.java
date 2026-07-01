package com.aicarrental;

import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleCategory;
import com.aicarrental.domain.vehicle.VehicleStatus;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AiCarRentalApplicationTests {
	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private TenantRepository tenantRepository;

	@Container
	static final PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("pgvector/pgvector:pg16")
					.withDatabaseName("ai_car_rental_test")
					.withUsername("test")
					.withPassword("test");

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("jwt.secret", () -> Base64.getEncoder().encodeToString(
				"test-only-signing-key-with-minimum-required-length"
						.getBytes(StandardCharsets.UTF_8)
		));
	}

	@Test
	void contextLoads() {
	}

	@Test
	void calculatesAvailableVehiclePricePercentilesInPostgres() {
		Tenant tenant = tenantRepository.save(Tenant.builder()
				.companyName("Semantic Search Test")
				.subDomain("semantic-test")
				.slug("semantic-search-test")
				.active(true)
				.build());

		vehicleRepository.saveAll(List.of(
				vehicle(tenant, "TEST001", 1000),
				vehicle(tenant, "TEST002", 2000),
				vehicle(tenant, "TEST003", 3000),
				vehicle(tenant, "TEST004", 4000),
				vehicle(tenant, "TEST005", 5000)
		));

		LocalDateTime pickup = LocalDateTime.now().plusDays(2);
		var distribution = vehicleRepository.calculateAvailablePriceDistribution(
				pickup,
				pickup.plusDays(3),
				500,
				"",
				"",
				List.of("COMPACT", "SEDAN"),
				false,
				"AUTOMATIC",
				"GASOLINE",
				4,
				"istanbul"
		);

		assertNotNull(distribution);
		assertEquals(5, distribution.getSampleCount());
		assertEquals(3400.0, distribution.getP60(), 0.01);

		var marketplacePage = vehicleRepository.searchPublicMarketplace(
				pickup,
				pickup.plusDays(3),
				null,
				BigDecimal.valueOf(3500),
				500,
				"",
				"",
				List.of(VehicleCategory.COMPACT, VehicleCategory.SEDAN),
				false,
				TransmissionType.AUTOMATIC,
				FuelType.GASOLINE,
				4,
				"istanbul",
				PageRequest.of(0, 10)
		);

		assertEquals(3, marketplacePage.getTotalElements());
	}

	@AfterEach
	void cleanSemanticSearchData() {
		vehicleRepository.deleteAll();
		tenantRepository.deleteAll();
	}

	private Vehicle vehicle(Tenant tenant, String plate, int dailyPrice) {
		return Vehicle.builder()
				.tenant(tenant)
				.brand("Test")
				.model("Compact")
				.plateNumber(plate)
				.productionYear(2025)
				.currentMileage(10_000)
				.dailyKmLimit(500)
				.extraKmPricePerKm(BigDecimal.TEN)
				.dailyPrice(BigDecimal.valueOf(dailyPrice))
				.category(VehicleCategory.COMPACT)
				.transmission(TransmissionType.AUTOMATIC)
				.fuelType(FuelType.GASOLINE)
				.seatCount(5)
				.location("Istanbul")
				.status(VehicleStatus.AVAILABLE)
				.active(true)
				.build();
	}
}
