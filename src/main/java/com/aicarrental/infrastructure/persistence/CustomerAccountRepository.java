package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.customer.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByEmailIgnoreCase(String email);
    Optional<CustomerAccount> findByEmailIgnoreCaseAndActiveTrue(String email);
    boolean existsByEmailIgnoreCase(String email);
}

