package com.blog.repository.user;

import com.blog.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    boolean existsByUsername(String username);
    Optional<UserProfile> findByUserId(Long userId);
}
