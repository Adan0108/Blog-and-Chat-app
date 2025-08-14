package com.blog.repository.security;

import com.blog.entity.user.UserKeyPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserKeyPairRepository extends JpaRepository<UserKeyPair, Long> {
    Optional<UserKeyPair> findByUserId(Long userId);
}
