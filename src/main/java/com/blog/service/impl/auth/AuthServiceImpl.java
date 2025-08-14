package com.blog.service.impl.auth;

import com.blog.config.security.JwtUtil;
import com.blog.dto.request.auth.LoginRequest;
import com.blog.dto.request.auth.SignUpRequest;
import com.blog.dto.response.auth.AuthResponse;
import com.blog.dto.response.auth.TokenPair;
import com.blog.entity.session.UserSession;
import com.blog.entity.user.User;
import com.blog.entity.user.UserKeyPair;
import com.blog.entity.user.UserProfile;
import com.blog.repository.security.UserKeyPairRepository;
import com.blog.repository.security.UserSessionRepository;
import com.blog.repository.user.UserProfileRepository;
import com.blog.repository.user.UserRepository;
import com.blog.service.auth.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    public static final String HDR_CLIENT_ID = "x-client-id";
    public static final String HDR_AUTHZ     = "authorization";
    public static final String HDR_REFRESH   = "x-rtoken-id";

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final UserKeyPairRepository keypairs;
    private final UserSessionRepository sessions;
    private final PasswordEncoder encoder;

    private final SecureRandom rng = new SecureRandom();
    private final HexFormat hex = HexFormat.of();

    public AuthServiceImpl(UserRepository users,
                           UserProfileRepository profiles,
                           UserKeyPairRepository keypairs,
                           UserSessionRepository sessions,
                           PasswordEncoder encoder) {
        this.users = users;
        this.profiles = profiles;
        this.keypairs = keypairs;
        this.sessions = sessions;
        this.encoder = encoder;
    }

    @Override
    public AuthResponse signUp(SignUpRequest req) {
        users.findByEmail(req.email()).ifPresent(u -> { throw new IllegalArgumentException("Email already exists"); });

        // Create user (no session/tokens)
        User u = new User();
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setEmailVerified(false);
        u = users.save(u);

        // Create profile
        UserProfile p = new UserProfile();
        p.setUser(u);
        p.setNickname(req.name());

        String baseUsername = req.name() == null ? ("user" + u.getId())
                : req.name().trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")      // collapse non-alphanum into single _
                .replaceAll("^_+|_+$", "");         // trim leading/trailing _
        if (baseUsername.isBlank()) baseUsername = "user"; // fallback

        p.setUsername(baseUsername + "_" + u.getId());
        profiles.save(p); // <-- FIXED: use the injected field name

        // Create keypair
        UserKeyPair kp = new UserKeyPair();
        kp.setUser(u);                 // @MapsId – do not set userId manually
        kp.setPublicKey(secret());
        kp.setPrivateKey(secret());
        keypairs.save(kp);

        return new AuthResponse(u.getId(), u.getEmail(), null);
    }

    @Override
    public AuthResponse login(LoginRequest req, String userAgent, String ipAddress) {
        User u = users.findByEmail(req.email()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        UserKeyPair kp = keypairs.findByUserId(u.getId()).orElseGet(() -> {
            UserKeyPair n = new UserKeyPair();
            n.setUser(u);
            n.setPublicKey(secret());
            n.setPrivateKey(secret());
            return keypairs.save(n);
        });

        String jti = UUID.randomUUID().toString();
        UserSession s = new UserSession();
        s.setUser(u);
        s.setJti(jti);
        s.setIpAddress(ipAddress);
        s.setUserAgent(userAgent);
        s.setExpiresAt(Instant.now().plus(JwtUtil.REFRESH_TTL));
        sessions.save(s);

        TokenPair tokens = JwtUtil.createTokenPair(u.getId(), u.getEmail(), kp.getPublicKey(), kp.getPrivateKey(), jti);
        return new AuthResponse(u.getId(), u.getEmail(), tokens);
    }

    @Override
    public AuthResponse refresh(Long userId, String refreshToken) {
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserKeyPair kp = keypairs.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("KeyPair not found"));

        var jws = JwtUtil.verifyRefresh(refreshToken, kp.getPrivateKey());
        if (!String.valueOf(userId).equals(jws.getBody().getSubject())) {
            throw new IllegalArgumentException("Invalid user");
        }

        String jti = jws.getBody().getId();
        var sess = sessions.findActive(userId, jti, Instant.now())
                .orElseThrow(() -> new IllegalStateException("Session revoked/expired"));

        String newJti = UUID.randomUUID().toString();
        sess.setJti(newJti);
        sess.setLastActive(Instant.now());
        sess.setExpiresAt(Instant.now().plus(JwtUtil.REFRESH_TTL));
        sessions.save(sess);

        TokenPair tokens = JwtUtil.createTokenPair(userId, u.getEmail(), kp.getPublicKey(), kp.getPrivateKey(), newJti);
        return new AuthResponse(userId, u.getEmail(), tokens);
    }

    @Override
    public void logout(Long userId, String accessHeader) {
        UserKeyPair kp = keypairs.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("KeyPair not found"));
        String token = extract(accessHeader);
        var jws = JwtUtil.verifyAccess(token, kp.getPublicKey());
        String jti = jws.getBody().getId();

        int n = sessions.revoke(userId, jti);
        if (n == 0) throw new IllegalStateException("Session not found / already revoked");
        // TODO: Add Redis blacklist later for AT if needed
    }

    private String secret() {
        byte[] b = new byte[64];
        rng.nextBytes(b);
        return hex.formatHex(b);
    }

    private String extract(String h) {
        return h != null && h.startsWith("Bearer ") ? h.substring(7) : h;
    }
}
