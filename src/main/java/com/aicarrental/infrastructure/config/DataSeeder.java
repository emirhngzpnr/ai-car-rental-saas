package com.aicarrental.infrastructure.config;

import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import com.aicarrental.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {


        if (tenantRepository.count() > 0) {
            return;
        }

        System.out.println(" Seed data yükleniyor...");

        // 1. Tenant oluştur
        Tenant fastcar = Tenant.builder()
                .companyName("FastCar Rental")
                .subDomain("fastcar")
                .active(true)
                .createdAt(LocalDateTime.now())
                .email("contact@fastcar.com")
                .phoneNumber("555-0001")
                .build();

        Tenant citycar = Tenant.builder()
                .companyName("CityDrive Mobility")
                .subDomain("citydrive")
                .active(true)
                .createdAt(LocalDateTime.now())
                .email("info@citydrive.com")
                .phoneNumber("555-0002")
                .build();

        fastcar = tenantRepository.save(fastcar);
        citycar = tenantRepository.save(citycar);

        User fastcarAdmin = User.builder()
                .email("admin@fastcar.com")
                .passwordHash(passwordEncoder.encode("123456admin"))
                .role(Role.TENANT_ADMIN)
                .tenant(fastcar)
                .build();

        User citycarStaff = User.builder()
                .email("staff@citydrive.com")
                .passwordHash(passwordEncoder.encode("123456staff"))
                .role(Role.TENANT_STAFF)
                .tenant(citycar)
                .build();

        userRepository.save(fastcarAdmin);
        userRepository.save(citycarStaff);

        System.out.println(" Seed data başarıyla yüklendi!");
    }
}