package com.aicarrental.common.security;

import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.Tenant;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public User getCurrentUser() {
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (!(principal instanceof User user)) {
            throw new BusinessException("Authenticated user could not be resolved");
        }

        return user;
    }

    public Tenant getCurrentTenant() {
        User currentUser = getCurrentUser();

        if (currentUser.getTenant() == null) {
            throw new BusinessException("Current user is not assigned to any tenant");
        }

        return currentUser.getTenant();
    }

    public Long getCurrentTenantId() {
        return getCurrentTenant().getId();
    }

    public boolean isSuperAdmin() {
        return getCurrentUser().getRole() == Role.SUPER_ADMIN;
    }

    public boolean isSuperAdmin(User user) {
        return user.getRole() == Role.SUPER_ADMIN;
    }
}
