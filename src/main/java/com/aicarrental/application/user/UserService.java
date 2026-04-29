package com.aicarrental.application.user;

import com.aicarrental.api.user.request.CreateUserRequest;
import com.aicarrental.api.user.request.UpdateUserRequest;
import com.aicarrental.api.user.response.UserResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
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
        Tenant currentTenant = currentUser.getTenant();

        if (currentTenant == null) {
            throw new BusinessException("Current user is not assigned to any tenant");
        }

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
        Long tenantId = currentUser.getTenant().getId();

        return userRepository.findByTenant_Id(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public UserResponse getUserById(Long id) {
        User currentUser = getCurrentUser();
        Long tenantId = currentUser.getTenant().getId();

        User user = userRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToResponse(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User currentUser = getCurrentUser();
        Long tenantId = currentUser.getTenant().getId();

        User user = userRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
        User currentUser = getCurrentUser();
        Long tenantId = currentUser.getTenant().getId();

        User user = userRepository.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
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

}
