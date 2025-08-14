package com.blog.repository.impl;

import com.blog.entity.session.UserSession;
import com.blog.repository.security.UserSessionRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@Transactional
public class UserSessionRepositoryImpl implements UserSessionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<UserSession> findActive(Long userId, String jti, Instant now) {
        var list = em.createQuery("""
                select s from UserSession s
                 where s.user.id = :userId
                   and s.jti = :jti
                   and s.revoked = false
                   and s.expiresAt > :now
                """, UserSession.class)
                .setParameter("userId", userId)
                .setParameter("jti", jti)
                .setParameter("now", now)
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public int revoke(Long userId, String jti) {
        return em.createQuery("""
                update UserSession s
                   set s.revoked = true
                 where s.user.id = :userId
                   and s.jti = :jti
                   and s.revoked = false
                """)
                .setParameter("userId", userId)
                .setParameter("jti", jti)
                .executeUpdate();
    }
}
