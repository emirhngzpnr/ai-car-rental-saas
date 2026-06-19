package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerProfileUpdateRequest;
import com.aicarrental.api.customer.response.CustomerProfileResponse;
import com.aicarrental.common.security.CurrentCustomerService;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {
    private final CurrentCustomerService currentCustomerService;
    private final CustomerAccountRepository repository;

    public CustomerProfileResponse getProfile() {
        return map(currentCustomerService.getCurrentCustomer());
    }

    @Transactional
    public CustomerProfileResponse updateProfile(CustomerProfileUpdateRequest request) {
        CustomerAccount account = currentCustomerService.getCurrentCustomer();
        account.setFirstName(request.firstName().trim());
        account.setLastName(request.lastName().trim());
        account.setPhone(request.phone().trim());
        account.setUpdatedAt(LocalDateTime.now());
        return map(repository.save(account));
    }

    private CustomerProfileResponse map(CustomerAccount account) {
        return new CustomerProfileResponse(
                account.getId(), account.getFirstName(), account.getLastName(),
                account.getEmail(), account.getPhone()
        );
    }
}
