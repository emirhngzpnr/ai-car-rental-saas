package com.aicarrental.common.security;

import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.customer.CustomerAccount;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentCustomerService {
    public CustomerAccount getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomerAccount customer)) {
            throw new BusinessException("Customer authentication is required");
        }
        return customer;
    }
}
