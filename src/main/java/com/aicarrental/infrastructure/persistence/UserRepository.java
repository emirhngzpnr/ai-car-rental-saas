package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.auth.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findByTenant_Id(Long tenantId);

    Optional<User> findByIdAndTenant_Id(Long id, Long tenantId);
}
