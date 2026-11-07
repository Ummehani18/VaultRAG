package com.vaultRag.backend.repository;

import com.vaultRag.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByRoleIgnoreCase(String role);

    long countByRoleIgnoreCase(String role);
}