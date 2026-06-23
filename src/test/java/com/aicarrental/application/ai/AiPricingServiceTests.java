package com.aicarrental.application.ai;

import com.aicarrental.application.tenant.TenantSettingService;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.AiPricingApprovedEvent;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.ai.AiPricingRecommendation;
import com.aicarrental.domain.ai.AiPricingRecommendationStatus;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.outbox.OutboxMessage;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.infrastructure.persistence.AiPricingRecommendationRepository;
import com.aicarrental.infrastructure.persistence.OutboxMessageRepository;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPricingServiceTests {
    @Mock RentalRepository rentalRepository;
    @Mock TenantSettingService tenantSettingService;
    @Mock CurrentUserService currentUserService;
    @Mock ChatClient.Builder chatClientBuilder;
    @Mock AiPricingRecommendationRepository recommendationRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock AuditEventPublisher auditEventPublisher;
    @Mock OutboxMessageRepository outboxMessageRepository;

    private AiPricingService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new AiPricingService(
                rentalRepository,
                tenantSettingService,
                currentUserService,
                chatClientBuilder,
                recommendationRepository,
                vehicleRepository,
                auditEventPublisher,
                outboxMessageRepository,
                objectMapper
        );
    }

    @Test
    void approveRecommendationPublishesCorrectOldAndNewPrice() throws Exception {
        Tenant tenant = Tenant.builder().id(3L).slug("fastcar").companyName("FastCar").build();
        User user = User.builder()
                .id(10L)
                .email("admin@fastcar.com")
                .role(Role.TENANT_ADMIN)
                .tenant(tenant)
                .active(true)
                .build();
        Vehicle vehicle = Vehicle.builder()
                .id(21L)
                .tenant(tenant)
                .plateNumber("34ABC123")
                .brand("Renault")
                .model("Clio")
                .dailyPrice(BigDecimal.valueOf(100))
                .build();
        AiPricingRecommendation recommendation = AiPricingRecommendation.builder()
                .id(7L)
                .tenant(tenant)
                .vehicle(vehicle)
                .currentPrice(BigDecimal.valueOf(100))
                .recommendedPrice(BigDecimal.valueOf(120))
                .confidenceLevel("MEDIUM")
                .reason("Demand is increasing")
                .status(AiPricingRecommendationStatus.PENDING)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.isSuperAdmin(user)).thenReturn(false);
        when(currentUserService.getCurrentTenantId()).thenReturn(3L);
        when(recommendationRepository.findById(7L)).thenReturn(Optional.of(recommendation));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(recommendationRepository.save(recommendation)).thenReturn(recommendation);
        when(outboxMessageRepository.save(any(OutboxMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.approveRecommendation(7L);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        org.mockito.Mockito.verify(outboxMessageRepository).save(captor.capture());

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AiPricingApprovedEvent event = objectMapper.readValue(
                captor.getValue().getPayload(),
                AiPricingApprovedEvent.class
        );

        assertEquals(BigDecimal.valueOf(100), event.oldPrice());
        assertEquals(BigDecimal.valueOf(120), event.newPrice());
        assertEquals(BigDecimal.valueOf(120), vehicle.getDailyPrice());
    }
}
