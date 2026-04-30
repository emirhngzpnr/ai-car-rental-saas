package com.aicarrental.application.user;

import com.aicarrental.api.user.request.CreateUserRequest;
import com.aicarrental.api.user.request.UpdateUserRequest;
import com.aicarrental.api.user.response.UserResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.Tenant;
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

    public UserResponse createUser(CreateUserRequest request) {
        User currentUser = getCurrentUser();
        validateUserCreationPermission(currentUser, request.role());
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
            return userRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Long tenantId = getCurrentTenantId();

        return userRepository.findByTenant_Id(tenantId)
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
        userRepository.delete(user);
    }

    private User findUserByIdWithTenantIsolation(Long userId) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        Long tenantId = getCurrentTenantId();

        return userRepository.findByIdAndTenant_Id(userId, tenantId)
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
            // SUPER_ADMIN has full privileges to create users with any role
            return;
        }

        if (currentUser.getRole() == Role.TENANT_ADMIN) {
            if (requestedRole != Role.TENANT_STAFF) {
                throw new BusinessException("Tenant admin can only create tenant staff users");
            }
            return;
        }

        // TENANT_STAFF lacks the authorization to create new users
        throw new BusinessException("You are not allowed to create users");
    }
}