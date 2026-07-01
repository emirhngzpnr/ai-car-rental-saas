package com.aicarrental.application.publicapi.assistant;

import com.aicarrental.api.publicapi.request.PublicAssistantQueryRequest;
import com.aicarrental.api.publicapi.response.PublicMarketplaceSearchResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchCriteriaResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchDateCriteriaResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretationResponse;
import com.aicarrental.application.knowledge.KnowledgeChunkStore;
import com.aicarrental.application.publicapi.PublicMarketplaceService;
import com.aicarrental.application.publicapi.PublicVehicleSearchInterpretationService;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicMarketplaceAssistantServiceTests {
    @Mock TenantRepository tenantRepository;
    @Mock KnowledgeChunkStore chunkStore;
    @Mock ChatClient.Builder chatClientBuilder;
    @Mock PublicVehicleSearchInterpretationService interpretationService;
    @Mock PublicMarketplaceService marketplaceService;

    private PublicMarketplaceAssistantService service;

    @BeforeEach
    void setUp() {
        service = new PublicMarketplaceAssistantService(
                tenantRepository,
                chunkStore,
                chatClientBuilder,
                interpretationService,
                marketplaceService
        );
    }

    @Test
    void policyQuestionWithoutKnowledgeDoesNotHallucinate() {
        Tenant tenant = Tenant.builder()
                .id(7L)
                .slug("fast-car")
                .companyName("Fast Car Rental")
                .active(true)
                .build();
        when(tenantRepository.findBySlugAndActiveTrue("fast-car")).thenReturn(Optional.of(tenant));
        when(chunkStore.searchTenantChunks(eq(7L), any(), eq(5))).thenReturn(List.of());

        var response = service.query(new PublicAssistantQueryRequest(
                "Deposit ne zaman iade edilir?",
                "fast-car",
                null, null,
                null, null, null,
                null, null, List.of(),
                null, null, null,
                null, null
        ));

        assertEquals(PublicAssistantIntent.POLICY_QA.name(), response.intent());
        assertEquals("I could not find this policy in the selected company's knowledge base.", response.answer());
        assertEquals(0, response.citations().size());
        assertNull(response.vehicles());
        verify(marketplaceService, never()).search(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)
        );
    }

    @Test
    void vehicleQuestionUsesMarketplaceSearchInsteadOfRag() {
        LocalDateTime pickup = LocalDateTime.now().plusDays(2);
        LocalDateTime returned = pickup.plusDays(2);
        var criteria = new PublicVehicleSearchCriteriaResponse(
                null, null, null,
                null, null, List.of(),
                null, null, null,
                null, "topRated"
        );
        var dateCriteria = new PublicVehicleSearchDateCriteriaResponse(pickup, returned);
        when(interpretationService.interpret("en begenilen arabalar", pickup, returned, null))
                .thenReturn(new PublicVehicleSearchInterpretResponse(
                        criteria,
                        dateCriteria,
                        new PublicVehicleSearchInterpretationResponse(null, null, null),
                        List.of(),
                        "Top rated vehicles were interpreted.",
                        List.of("Top rated was interpreted as rating sort."),
                        List.of()
                ));
        when(marketplaceService.search(
                pickup, returned,
                null, null, null,
                null, null, null, List.of(),
                null, null, null,
                null, "topRated", 0, 6
        )).thenReturn(new PublicMarketplaceSearchResponse(List.of(), 0, 6, 0, 0));

        var response = service.query(new PublicAssistantQueryRequest(
                "en begenilen arabalar",
                null,
                pickup, returned,
                null, null, null,
                null, null, List.of(),
                null, null, null,
                null, null
        ));

        assertEquals(PublicAssistantIntent.VEHICLE_SEARCH.name(), response.intent());
        assertEquals("topRated", response.vehicleSearchCriteria().sort());
        assertEquals(0, response.vehicles().totalElements());
        verify(chunkStore, never()).searchTenantChunks(any(), any(), any(Integer.class));
    }
}
