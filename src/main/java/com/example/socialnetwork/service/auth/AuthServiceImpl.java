package com.example.socialnetwork.service.auth;

import com.example.socialnetwork.config.JwtProperties;
import com.example.socialnetwork.domain.dto.auth.AuthTokenResponseDto;
import com.example.socialnetwork.domain.dto.auth.CurrentUserDto;
import com.example.socialnetwork.domain.dto.auth.LoginRequestDto;
import com.example.socialnetwork.domain.dto.auth.RefreshTokenRequestDto;
import com.example.socialnetwork.domain.dto.auth.RegisterRequestDto;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.domain.model.UserSession;
import com.example.socialnetwork.repository.UserRepository;
import com.example.socialnetwork.repository.UserSessionRepository;
import com.example.socialnetwork.security.AuthenticatedUser;
import com.example.socialnetwork.security.JwtTokenService;
import com.example.socialnetwork.service.account.AccountPrivacyService;
import io.jsonwebtoken.JwtException;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AccountPrivacyService accountPrivacyService;

    @Override
    @Transactional
    public AuthTokenResponseDto register(RegisterRequestDto request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(normalizeText(request.getFirstName(), "firstName"));
        user.setLastName(normalizeText(request.getLastName(), "lastName"));
        user.setAbout(normalizeOptionalText(request.getAbout()));
        user.setBirthDate(request.getBirthDate());
        user.setCity(normalizeOptionalText(request.getCity()));
        user.setGender(request.getGender());

        User savedUser = userRepository.save(user);
        accountPrivacyService.ensureDefaults(savedUser);
        return issueTokens(savedUser);
    }

    @Override
    @Transactional
    public AuthTokenResponseDto login(LoginRequestDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizeEmail(request.getEmail()), request.getPassword())
            );
            AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
            return issueTokens(user);
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @Override
    @Transactional
    public AuthTokenResponseDto refresh(RefreshTokenRequestDto request) {
        JwtTokenService.JwtClaims claims = parseRefreshClaims(request.getRefreshToken());
        UserSession session = userSessionRepository.findByRefreshTokenId(claims.tokenId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));

        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked");
        }

        session.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(session);
        return issueTokens(session.getUser());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        JwtTokenService.JwtClaims claims = parseRefreshClaims(request.getRefreshToken());
        userSessionRepository.findByRefreshTokenId(claims.tokenId())
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(LocalDateTime.now());
                        userSessionRepository.save(session);
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserDto me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return new CurrentUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAbout(),
                user.getBirthDate(),
                user.getCity(),
                user.getGender()
        );
    }

    private AuthTokenResponseDto issueTokens(User user) {
        String accessToken = jwtTokenService.generateAccessToken(user);
        JwtTokenService.RefreshTokenPayload refreshPayload = jwtTokenService.generateRefreshToken(user);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenId(refreshPayload.tokenId());
        session.setExpiresAt(refreshPayload.expiresAt());
        userSessionRepository.save(session);

        return new AuthTokenResponseDto(
                accessToken,
                refreshPayload.token(),
                jwtProperties.getAccessTtlMinutes(),
                jwtProperties.getRefreshTtlDays()
        );
    }

    private JwtTokenService.JwtClaims parseRefreshClaims(String token) {
        try {
            return jwtTokenService.parseRefreshToken(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
    }

    private String normalizeEmail(String email) {
        return normalizeText(email, "email").toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
