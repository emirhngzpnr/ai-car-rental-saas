package com.aicarrental.api.insurance.request;

import com.aicarrental.domain.insurance.InsurancePackageType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateInsurancePackageRequest(@NotNull
                                            InsurancePackageType type,

                                            @NotBlank
                                               @Size(max = 100)
                                               String name,

                                            @NotBlank
                                               @Size(max = 1000)
                                               String coverageDescription,

                                            @NotNull
                                               @DecimalMin(value = "0.00")
                                            BigDecimal dailyPrice,

                                            @NotNull
                                               Boolean active) {
}
