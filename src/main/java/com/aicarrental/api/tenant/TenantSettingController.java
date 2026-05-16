package com.aicarrental.api.tenant;
import com.aicarrental.api.tenant.request.UpdateTenantSettingRequest;
import com.aicarrental.api.tenant.response.TenantSettingResponse;
import com.aicarrental.application.tenant.TenantSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant-settings")
@RequiredArgsConstructor
public class TenantSettingController {
    private final TenantSettingService tenantSettingService;

    @GetMapping("/me")
    public ResponseEntity<List<TenantSettingResponse>> getCurrentTenantSettings() {
        return ResponseEntity.ok(
                tenantSettingService.getCurrentTenantSettings()
        );
    }
    @PutMapping("/me/{settingKey}")
    public ResponseEntity<TenantSettingResponse> updateCurrentTenantSetting(
            @PathVariable String settingKey,
            @Valid @RequestBody UpdateTenantSettingRequest request
    ) {
        return ResponseEntity.ok(
                tenantSettingService.updateCurrentTenantSetting(
                        settingKey,
                        request
                )
        );
    }
}
