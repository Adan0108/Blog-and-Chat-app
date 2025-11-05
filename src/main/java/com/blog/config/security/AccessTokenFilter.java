package com.blog.config.security;

import com.blog.repository.security.UserKeyPairRepository;
import com.blog.repository.security.UserSessionRepository;
import com.blog.repository.user.UserRepository;
import com.blog.service.auth.TokenBlacklistService; // ADDED
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {

    private static final String HDR_CLIENT_ID = "x-client-id";
    private static final String HDR_AUTHZ     = "authorization";

    private final UserKeyPairRepository keypairs;
    private final UserSessionRepository sessions;
    private final UserRepository users;
    private final TokenBlacklistService blacklist; // ADDED

    public AccessTokenFilter(UserKeyPairRepository keypairs,
                             UserSessionRepository sessions,
                             UserRepository users,
                             TokenBlacklistService blacklist) { // ADDED
        this.keypairs = keypairs;
        this.sessions = sessions;
        this.users = users;
        this.blacklist = blacklist; // ADDED
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();  // "/api/v1/auth/login"
        String method = request.getMethod();
        // keep exactly your previous allowlist
        return "POST".equalsIgnoreCase(method) &&
                ("/api/v1/auth/signup".equals(path) ||
                        "/api/v1/auth/login".equals(path)  ||
                        "/api/v1/auth/refresh".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Allow CORS preflight to pass
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String uidStr = req.getHeader(HDR_CLIENT_ID);
        String auth   = req.getHeader(HDR_AUTHZ);
        if (uidStr == null || auth == null) { res.setStatus(401); return; }

        Long userId;
        try { userId = Long.valueOf(uidStr.trim()); } // TRIMMED
        catch (Exception e) { res.setStatus(401); return; }

        var kpOpt = keypairs.findByUserId(userId);
        if (kpOpt.isEmpty()) { res.setStatus(401); return; }

        try {
            // Normalize Bearer
            String token = auth.startsWith("Bearer ") ? auth.substring(7).trim() : auth.trim();

            // Verify AT using user's public key (same as before)
            var jws = JwtUtil.verifyAccess(token, kpOpt.get().getPublicKey());

            Long sub = Long.valueOf(jws.getBody().getSubject());
            if (!sub.equals(userId)) { res.setStatus(401); return; }

            // Optional password-rotated token invalidation (unchanged & still commented)
            // var uOpt = users.findById(userId);
            // if (uOpt.isPresent() && uOpt.get().getPasswordChangedAt() != null &&
            //         jws.getBody().getIssuedAt().toInstant().isBefore(uOpt.get().getPasswordChangedAt())) {
            //     res.setStatus(401); return;
            // }

            String jti = jws.getBody().getId();

            // NEW: Redis blacklist check (prevents reuse after logout)
            if (blacklist.isBlacklisted(userId, jti)) {
                res.setStatus(401);
                return;
            }

            // Keep your DB session check
            if (sessions.findActive(userId, jti, Instant.now()).isEmpty()) {
                res.setStatus(401); return;
            }

            var authn = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authn);
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(req, res);
    }
}
