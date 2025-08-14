package com.blog.repository.security;

import com.blog.entity.session.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, String>, UserSessionRepositoryCustom { }
