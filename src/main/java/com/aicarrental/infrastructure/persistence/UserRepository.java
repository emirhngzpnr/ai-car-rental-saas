package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.auth.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    @EntityGraph(attributePaths = "tenant")
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findByTenant_IdAndActiveTrue(Long tenantId);

    Optional<User> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);

    List<User> findByActiveTrue();

    Optional<User> findByIdAndActiveTrue(Long id);
}
