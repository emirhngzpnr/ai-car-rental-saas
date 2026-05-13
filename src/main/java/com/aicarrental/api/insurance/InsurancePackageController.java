package com.aicarrental.api.insurance;

import com.aicarrental.api.insurance.request.CreateInsurancePackageRequest;
import com.aicarrental.api.insurance.request.UpdateInsurancePackageRequest;
import com.aicarrental.api.insurance.response.InsurancePackageResponse;
import com.aicarrental.application.insurance.InsurancePackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insurance-packages")
@RequiredArgsConstructor
public class InsurancePackageController {
    private final InsurancePackageService insurancePackageService;

    @PostMapping
    public ResponseEntity<InsurancePackageResponse> createInsurancePackage(
            @Valid @RequestBody CreateInsurancePackageRequest request
    ) {
        InsurancePackageResponse response =
                insurancePackageService.createInsurancePackage(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InsurancePackageResponse>> getAllInsurancePackages(
            @RequestParam Long tenantId
    ) {
        return ResponseEntity.ok(
                insurancePackageService.getAllInsurancePackages(tenantId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsurancePackageResponse> updateInsurancePackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInsurancePackageRequest request
    ) {
        return ResponseEntity.ok(
                insurancePackageService.updateInsurancePackage(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInsurancePackage(
            @PathVariable Long id
    ) {
        insurancePackageService.deleteInsurancePackage(id);
        return ResponseEntity.noContent().build();
    }
}
