package com.aicarrental.application.user;

import com.aicarrental.api.user.request.CreateUserRequest;
import com.aicarrental.api.user.request.UpdateUserRequest;
import com.aicarrental.api.user.response.UserResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import com.aicarrental.infrastructure.persistence.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserService currentUserService;
    private final UserInvitationTokenService invitationTokenService;
    private final UserInvitationEmailService invitationEmailService;

    public UserResponse createUser(CreateUserRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        validateUserCreationPermission(currentUser, request.role());
        Tenant tenantToAssign = resolveTenantForUserCreation(currentUser, request);

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(generateUnusablePassword()))
                .role(request.role())
                .active(true)
                .tenant(tenantToAssign)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);
        String rawToken = invitationTokenService.createInvitationToken(savedUser, currentUser);
        try {
            invitationEmailService.sendInvitationEmail(savedUser, rawToken);
        } catch (RuntimeException exception) {
            log.warn(
                    "User invitation email could not be sent. userId={}, email={}, tenantId={}",
                    savedUser.getId(),
                    savedUser.getEmail(),
                    tenantToAssign.getId(),
                    exception
            );
            throw new BusinessException("Invitation email could not be sent. Check mail configuration and try again.");
        }

        //                LOG
        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                tenantToAssign.getId(),
                AuditAction.USER_CREATED,
                "User",
                savedUser.getId(),
                "User created: " + savedUser.getEmail()
        ));

        return mapToResponse(savedUser);
    }

    public void setPasswordFromInvitation(String rawToken, String newPassword) {
        var invitationToken = invitationTokenService.consume(rawToken);
        User user = invitationToken.getUser();

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("Invalid or expired invitation link");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findByActiveTrue()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Long tenantId = currentUserService.getCurrentTenantId();

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
        User currentUser = currentUserService.getCurrentUser();
        User user = findUserByIdWithTenantIsolation(id);
        validateUserUpdatePermission(currentUser, user);

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
            validateUserRoleUpdatePermission(
                    currentUser,
                    user,
                    request.role()
            );

            user.setRole(request.role());
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                updatedUser.getTenant() != null ? updatedUser.getTenant().getId() : null,
                AuditAction.USER_UPDATED,
                "User",
                updatedUser.getId(),
                "User updated: " + updatedUser.getEmail()
        ));

        return mapToResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        User currentUser = currentUserService.getCurrentUser();
        User user = findUserByIdWithTenantIsolation(id);
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                AuditAction.USER_DELETED,
                "User",
                user.getId(),
                "User soft deleted: " + user.getEmail()
        ));

    }

    private User findUserByIdWithTenantIsolation(Long userId) {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return userRepository.findByIdAndTenant_IdAndActiveTrue(userId,tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

    private String generateUnusablePassword() {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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

        return currentUserService.getCurrentTenant();
    }
    private void validateUserUpdatePermission(
            User currentUser,
            User targetUser
    ) {
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            if (targetUser.getRole() == Role.SUPER_ADMIN) {
                throw new BusinessException("SUPER_ADMIN user cannot be updated from this endpoint");
            }

            return;
        }

        if (currentUser.getRole() == Role.TENANT_ADMIN) {
            if (targetUser.getRole() != Role.TENANT_STAFF) {
                throw new BusinessException("Tenant admin can only update tenant staff users");
            }

            return;
        }

        throw new BusinessException("You are not allowed to update users");
    }
    private void validateUserRoleUpdatePermission(
            User currentUser,
            User targetUser,
            Role requestedRole
    ) {
        if (requestedRole == Role.SUPER_ADMIN) {
            throw new BusinessException("SUPER_ADMIN role cannot be assigned");
        }

        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            if (targetUser.getRole() == Role.SUPER_ADMIN) {
                throw new BusinessException("SUPER_ADMIN user cannot be updated from this endpoint");
            }

            return;
        }

        if (currentUser.getRole() == Role.TENANT_ADMIN) {
            if (targetUser.getRole() != Role.TENANT_STAFF) {
                throw new BusinessException("Tenant admin can only update tenant staff users");
            }

            if (requestedRole != Role.TENANT_STAFF) {
                throw new BusinessException("Tenant admin cannot change user role");
            }

            return;
        }

        throw new BusinessException("You are not allowed to update user roles");
    }
}
