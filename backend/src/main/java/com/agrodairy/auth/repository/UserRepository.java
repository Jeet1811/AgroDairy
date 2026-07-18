package com.agrodairy.auth.repository;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);
}
