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

        System.out.println("DataSeeder çalıştı...");

        if (tenantRepository.count() == 0) {
            System.out.println("Seed tenant data yükleniyor...");

            Tenant fastcar = Tenant.builder()
                    .companyName("FastCar Rental")
                    .subDomain("fastcar")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .email("contact@fastcar.com")
                    .phoneNumber("555-0001")
                    .build();

            Tenant citycar = Tenant.builder()
                    .companyName("CityDrive Mobility")
                    .subDomain("citydrive")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .email("info@citydrive.com")
                    .phoneNumber("555-0002")
                    .build();

            fastcar = tenantRepository.save(fastcar);
            citycar = tenantRepository.save(citycar);

            if (!userRepository.existsByEmail("admin@fastcar.com")) {
                userRepository.save(User.builder()
                        .firstName("FastCar")
                        .lastName("Admin")
                        .email("admin@fastcar.com")
                        .passwordHash(passwordEncoder.encode("123456admin"))
                        .role(Role.TENANT_ADMIN)
                        .active(true)
                        .tenant(fastcar)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
            }

            if (!userRepository.existsByEmail("staff@citydrive.com")) {
                userRepository.save(User.builder()
                        .firstName("CityDrive")
                        .lastName("Staff")
                        .email("staff@citydrive.com")
                        .passwordHash(passwordEncoder.encode("123456staff"))
                        .role(Role.TENANT_STAFF)
                        .active(true)
                        .tenant(citycar)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
        }

        if (!userRepository.existsByEmail("superadmin@aicarrental.com")) {
            Tenant firstTenant = tenantRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow();

            User superAdmin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email("superadmin@aicarrental.com")
                    .passwordHash(passwordEncoder.encode("SuperAdmin123!"))
                    .role(Role.SUPER_ADMIN)
                    .active(true)
                    .tenant(firstTenant)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(superAdmin);

            System.out.println("SUPER_ADMIN oluşturuldu: superadmin@aicarrental.com");
        } else {
            System.out.println("SUPER_ADMIN zaten mevcut.");
        }

        System.out.println("DataSeeder tamamlandı.");
    }
}