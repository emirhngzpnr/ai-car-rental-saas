package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
