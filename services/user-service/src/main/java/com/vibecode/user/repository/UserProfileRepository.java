package com.vibecode.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vibecode.user.entity.UserProfile;

public interface  UserProfileRepository extends JpaRepository<UserProfile, UUID> {

}
