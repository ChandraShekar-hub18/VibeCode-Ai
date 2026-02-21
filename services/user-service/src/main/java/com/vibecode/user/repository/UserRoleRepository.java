package com.vibecode.user.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vibecode.user.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long>{
    List<UserRole> findByUserId(UUID userId);
}
