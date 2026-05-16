package com.aicarrental.application.tenant;
import com.aicarrental.api.tenant.request.UpdateTenantSettingRequest;
import com.aicarrental.api.tenant.response.TenantSettingResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.*;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import com.aicarrental.infrastructure.persistence.TenantSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantSettingService {
    private final TenantSettingRepository tenantSettingRepository;
    private final TenantRepository tenantRepository;
    private final CurrentUserService currentUserService;
    private final AuditEventPublisher auditEventPublisher;

    public List<TenantSettingResponse> getCurrentTenantSettings() {
        User currentUser = currentUserService.getCurrentUser();

        Long tenantId = currentUserService.isSuperAdmin(currentUser)
                ? currentUser.getTenant().getId()
                : currentUserService.getCurrentTenantId();

        createMissingDefaultSettings(tenantId);

        return tenantSettingRepository.findByTenant_IdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void createMissingDefaultSettings(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Arrays.stream(TenantSettingKey.values())
                .filter(key -> !tenantSettingRepository.existsByTenant_IdAndSettingKey(
                        tenantId,
                        key.name()
                ))
                .forEach(key -> tenantSettingRepository.save(
                        TenantSetting.builder()
                                .tenant(tenant)
                                .settingKey(key.name())
                                .settingValue(key.getDefaultValue())
                                .description("Default setting for " + key.name())
                                .active(true)
                                .build()
                ));
    }
    public TenantSettingResponse updateCurrentTenantSetting(
            String settingKey,
             UpdateTenantSettingRequest request
    ) {
        TenantSettingKey key;

        try {
            key = TenantSettingKey.valueOf(settingKey);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Invalid setting key");
        }

        validateSettingValue(key, request.settingValue());

        User currentUser = currentUserService.getCurrentUser();

        Long tenantId = currentUserService.isSuperAdmin(currentUser)
                ? currentUser.getTenant().getId()
                : currentUserService.getCurrentTenantId();

        createMissingDefaultSettings(tenantId);

        TenantSetting setting =
                tenantSettingRepository
                        .findByTenant_IdAndSettingKeyAndActiveTrue(
                                tenantId,
                                key.name()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Tenant setting not found")
                        );
        String oldValue = setting.getSettingValue();

        setting.setSettingValue(request.settingValue());
        TenantSetting savedSetting = tenantSettingRepository.save(setting);
        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                tenantId,
                AuditAction.TENANT_SETTING_UPDATED,
                "TenantSetting",
                savedSetting.getId(),
                "Tenant setting updated. Key: "
                        + savedSetting.getSettingKey()
                        + ", Old value: "
                        + oldValue
                        + ", New value: "
                        + savedSetting.getSettingValue()
        ));
        return mapToResponse(
                tenantSettingRepository.save(savedSetting)
        );
    }
    private void validateSettingValue(
            TenantSettingKey key,
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Setting value cannot be empty");
        }

        try {
            switch (key.getDataType()) {
                case INTEGER -> Integer.parseInt(value);
                case DECIMAL -> new BigDecimal(value);
                case BOOLEAN -> {
                    if (!value.equalsIgnoreCase("true")
                            && !value.equalsIgnoreCase("false")) {
                        throw new BusinessException("Boolean value must be true or false");
                    }
                }
                case STRING -> {
                    // no-op
                }
            }
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    "Invalid value for setting type: " + key.getDataType()
            );
        }
    }

    private TenantSettingResponse mapToResponse(TenantSetting setting) {
        TenantSettingKey key = TenantSettingKey.valueOf(setting.getSettingKey());

        return new TenantSettingResponse(
                setting.getId(),
                setting.getTenant().getId(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                key.getDataType(),
                key.getDefaultValue(),
                setting.getDescription(),
                setting.getActive()
        );
    }

}
