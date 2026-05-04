package com.aicarrental.application.user;

import com.aicarrental.api.user.request.CreateUserRequest;
import com.aicarrental.api.user.request.UpdateUserRequest;
import com.aicarrental.api.user.response.UserResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import com.aicarrental.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;

    public UserResponse createUser(CreateUserRequest request) {
        User currentUser = getCurrentUser();
        validateUserCreationPermission(currentUser, request.role());
        Tenant tenantToAssign = resolveTenantForUserCreation(currentUser, request);

        Tenant currentTenant = getCurrentTenant();

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .tenant(currentTenant)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findByActiveTrue()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Long tenantId = getCurrentTenantId();

        return userRepository.findByTenant_IdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = findUserByIdWithTenantIsolation(id);
        return mapToResponse(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserByIdWithTenantIsolation(id);

        if (request.email() != null && userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new BusinessException("Email already exists");
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        if (request.email() != null) {
            user.setEmail(request.email());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        User user = findUserByIdWithTenantIsolation(id);
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    private User findUserByIdWithTenantIsolation(Long userId) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        Long tenantId = getCurrentTenantId();

        return userRepository.findByIdAndTenant_IdAndActiveTrue(userId,tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (!(principal instanceof User user)) {
            throw new BusinessException("Authenticated user could not be resolved");
        }

        return user;
    }

    private Tenant getCurrentTenant() {
        User currentUser = getCurrentUser();

        if (currentUser.getTenant() == null) {
            throw new BusinessException("Current user is not assigned to any tenant");
        }

        return currentUser.getTenant();
    }

    private Long getCurrentTenantId() {
        return getCurrentTenant().getId();
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getActive(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getTenant() != null ? user.getTenant().getCompanyName() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
    private void validateUserCreationPermission(User currentUser, Role requestedRole) {

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            if (requestedRole == Role.SUPER_ADMIN) {
                throw new BusinessException("SUPER_ADMIN user cannot be created from this endpoint");
            }
            return;
        }

        if (currentUser.getRole() == Role.TENANT_ADMIN) {
            if (requestedRole != Role.TENANT_STAFF) {
                throw new BusinessException("Tenant admin can only create tenant staff users");
            }
            return;
        }

        throw new BusinessException("You are not allowed to create users");
    }
    private Tenant resolveTenantForUserCreation(User currentUser, CreateUserRequest request) {

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            if (request.tenantId() == null) {
                throw new BusinessException("TenantId is required when SUPER_ADMIN creates a user");
            }

            return tenantRepository.findByIdAndActiveTrue(request.tenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        }

        if (request.tenantId() != null) {
            throw new BusinessException("Only SUPER_ADMIN can assign tenant");
        }

        return getCurrentTenant();
    }
}