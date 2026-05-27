package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.AccountRoleAssignment;
import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.RefreshToken;
import com.hoandev.pinedrink.entity.Role;
import com.hoandev.pinedrink.entity.Scope;
import com.hoandev.pinedrink.entity.dto.request.Auth.LoginRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RefreshTokenRequest;
import com.hoandev.pinedrink.entity.dto.request.Auth.RegisterRequest;
import com.hoandev.pinedrink.entity.dto.response.Auth.*;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.AccountRoleAssignmentRepository;
import com.hoandev.pinedrink.repository.CustomerProfileRepository;
import com.hoandev.pinedrink.repository.RefreshTokenRepository;
import com.hoandev.pinedrink.repository.RoleRepository;
import com.hoandev.pinedrink.repository.ScopeRepository;
import com.hoandev.pinedrink.security.JwtTokenProvider;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.queue.event.email.PasswordResetEmailEvent;
import com.hoandev.pinedrink.queue.event.email.RegisterOtpEmailEvent;
import com.hoandev.pinedrink.queue.publisher.EventPublisher;
import com.hoandev.pinedrink.service.AuthService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import com.hoandev.pinedrink.utils.Constants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AuthService}.
 * Handles registration (with OTP), authentication, token refresh, and logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AccountRoleAssignmentRepository assignmentRepository;
    private final ScopeRepository scopeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final CodeGenerator codeGenerator;
    private final StringRedisTemplate stringRedisTemplate;
    private final EventPublisher eventPublisher;

    private static final String OTP_KEY_PREFIX = "otp:register:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;
    private static final String RESEND_COOLDOWN_PREFIX = "otp:cooldown:";
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final String OTP_ATTEMPT_PREFIX = "otp:register:attempt:";
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String FORGOT_PASSWORD_OTP_PREFIX = "otp:forgot-password:";
    private static final String FORGOT_PASSWORD_COOLDOWN_PREFIX = "otp:forgot-password:cooldown:";
    private static final String FORGOT_PASSWORD_ATTEMPT_PREFIX = "otp:forgot-password:attempt:";

    @Value("${app.jwt.refresh-token-expiration:86400}")
    private long refreshTokenExpirationSeconds;

    @Value("${app.jwt.reset-token-expiration:900}")
    private long resetTokenExpirationSeconds;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;

        if (accountRepository.existsByUsername(username)) {
            throw new BaseException(ErrorCode.AUTH_013);
        }
        if (accountRepository.existsByEmail(email)) {
            throw new BaseException(ErrorCode.AUTH_014);
        }
        if (phone != null && accountRepository.existsByPhone(phone)) {
            throw new BaseException(ErrorCode.AUTH_015);
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName());
        account.setEmail(email);
        account.setPhone(phone);
        account.setStatus(Constants.STATUS_INACTIVE);
        account = accountRepository.save(account);

        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);
        stringRedisTemplate.opsForValue().set(OTP_KEY_PREFIX + account.getId(), otpHash, OTP_TTL);

        log.info("Account registered (inactive): username={}, email={}", account.getUsername(), account.getEmail());

        RegisterOtpEmailEvent event = RegisterOtpEmailEvent.of(
                account.getEmail(), otp, (int) OTP_TTL.toMinutes());
        publishAfterCommit(event);

        return RegisterResponse.builder()
                .userId(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .message("Account created. Please verify your email with the OTP sent.")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void verifyRegistrationOtp(String email, String otp) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        if (!Constants.STATUS_INACTIVE.equals(account.getStatus())) {
            throw new BaseException(ErrorCode.AUTH_018);
        }

        String key = OTP_KEY_PREFIX + account.getId();
        String storedOtpHash = stringRedisTemplate.opsForValue().get(key);

        if (storedOtpHash == null) {
            throw new BaseException(ErrorCode.AUTH_017);
        }

        String attemptKey = OTP_ATTEMPT_PREFIX + account.getId();

        if (!passwordEncoder.matches(otp, storedOtpHash)) {
            Long attempts = stringRedisTemplate.opsForValue().increment(attemptKey);

            if (attempts != null && attempts == 1) {
                stringRedisTemplate.expire(attemptKey, OTP_TTL);
            }

            if (attempts != null && attempts >= MAX_OTP_ATTEMPTS) {
                stringRedisTemplate.delete(key);
                stringRedisTemplate.delete(attemptKey);
                throw new BaseException(ErrorCode.AUTH_019);
            }

            throw new BaseException(ErrorCode.AUTH_016);
        }

        stringRedisTemplate.delete(key);
        stringRedisTemplate.delete(attemptKey);
        account.setStatus(Constants.STATUS_ACTIVE);
        accountRepository.save(account);

        Role customerRole = roleRepository.findByCode(Constants.ROLE_CUSTOMER)
                .orElseThrow(() -> new BaseException(ErrorCode.ROLE_NOT_FOUND));

        Scope systemScope = scopeRepository.findByScopeTypeAndBrandIdAndBranchId("SYSTEM", null, null)
                .orElseThrow(() -> new BaseException(ErrorCode.SCOPE_NOT_FOUND));

        AccountRoleAssignment assignment = new AccountRoleAssignment();
        assignment.setAccount(account);
        assignment.setRole(customerRole);
        assignment.setScope(systemScope);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus(Constants.STATUS_ACTIVE);
        assignmentRepository.save(assignment);

        CustomerProfile profile = new CustomerProfile();
        profile.setFullName(account.getFullName());
        profile.setPhone(account.getPhone());
        profile.setEmail(account.getEmail());
        profile.setAccount(account);
        profile.setCustomerCode(codeGenerator.generate("KH"));
        profile.setStatus(Constants.STATUS_ACTIVE);
        customerProfileRepository.save(profile);

        log.info("Account activated: username={}, role={}", account.getUsername(), Constants.ROLE_CUSTOMER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void resendRegistrationOtp(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        if (!Constants.STATUS_INACTIVE.equals(account.getStatus())) {
            throw new BaseException(ErrorCode.AUTH_018);
        }

        String cooldownKey = RESEND_COOLDOWN_PREFIX + account.getId();
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            throw new BaseException(ErrorCode.AUTH_020);
        }

        String newOtp = generateOtp();
        String newOtpHash = passwordEncoder.encode(newOtp);
        stringRedisTemplate.opsForValue().set(OTP_KEY_PREFIX + account.getId(), newOtpHash, OTP_TTL);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", RESEND_COOLDOWN);

        RegisterOtpEmailEvent event = RegisterOtpEmailEvent.of(
                account.getEmail(), newOtp, (int) OTP_TTL.toMinutes());
        publishAfterCommit(event);
        log.info("OTP resent: email={}", email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String usernameOrEmail = request.getUsername().trim();
        Account account = findAccountForLogin(usernameOrEmail)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_001));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BaseException(ErrorCode.AUTH_001);
        }
        validateAccountStatus(account);

        List<String> roles = loadActiveRoleCodes(account.getId());
        UserPrincipal principal = new UserPrincipal(
                account.getId(), account.getUsername(), account.getEmail(),
                account.getPassword(), account.getStatus(),
                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
        );

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(account, refreshToken);

        account.setLastLoginAt(LocalDateTime.now());
        accountRepository.save(account);
        log.info("Account logged in: {}", account.getUsername());

        return buildLoginResponse(accessToken, refreshToken, account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_004));

        if (storedToken.getRevokedAt() != null) {
            throw new BaseException(ErrorCode.AUTH_004, "Refresh token has been revoked");
        }
        if (storedToken.getExpiresAt() != null && storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BaseException(ErrorCode.AUTH_004, "Refresh token has expired");
        }

        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        Account account = storedToken.getAccount();
        validateAccountStatus(account);

        List<String> roles = loadActiveRoleCodes(account.getId());
        UserPrincipal principal = new UserPrincipal(
                account.getId(), account.getUsername(), account.getEmail(),
                account.getPassword(), account.getStatus(),
                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
        );

        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(account, newRefreshToken);

        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setTokenType(Constants.TOKEN_TYPE_BEARER);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds());
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
            log.debug("Refresh token revoked for accountId={}", token.getAccount().getId());
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountResponse getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BaseException(ErrorCode.AUTH_003);
        }
        Account account = accountRepository.findById(principal.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));
        return toAccountResponse(account);
    }

    /**
     * Publishes an email event after the current DB transaction commits.
     * If no transaction synchronization is active, publishes immediately.
     */
    private void publishAfterCommit(RegisterOtpEmailEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publish(event);
                        }
                    });
        } else {
            eventPublisher.publish(event);
        }
    }

    /**
     * Finds an account by username or email for login.
     */
    private java.util.Optional<Account> findAccountForLogin(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return accountRepository.findByEmail(usernameOrEmail.toLowerCase());
        }
        return accountRepository.findByUsername(usernameOrEmail);
    }

    /**
     * Validates that the account status allows login.
     */
    private void validateAccountStatus(Account account) {
        String status = account.getStatus();
        if ("LOCKED".equals(status)) {
            throw new BaseException(ErrorCode.AUTH_005, "Account is locked");
        }
        if (!Constants.STATUS_ACTIVE.equals(status)) {
            throw new BaseException(ErrorCode.AUTH_006, "Account is inactive");
        }
    }

    /**
     * Loads active (non-expired) role codes assigned to the account.
     */
    private List<String> loadActiveRoleCodes(String accountId) {
        return assignmentRepository.findActiveRoleCodesByAccountId(accountId, LocalDateTime.now())
                .stream()
                .map(code -> "ROLE_" + code)
                .distinct()
                .toList();
    }

    /**
     * Persists a hashed refresh token tied to the given account.
     */
    private void saveRefreshToken(Account account, String rawToken) {
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hashToken(rawToken));
        token.setAccount(account);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds));
        refreshTokenRepository.save(token);
    }

    /**
     * Computes a SHA-256 hex hash of the given token string.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Builds a {@link LoginResponse} from the generated tokens and account.
     */
    private LoginResponse buildLoginResponse(String accessToken, String refreshToken, Account account) {
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType(Constants.TOKEN_TYPE_BEARER);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiresInSeconds());
        response.setAccount(toAccountResponse(account));
        return response;
    }

    /**
     * Maps an {@link Account} entity to an {@link AccountResponse} DTO.
     */
    private AccountResponse toAccountResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setBrandId(account.getBrand() != null ? account.getBrand().getId() : null);
        response.setUsername(account.getUsername());
        response.setFullName(account.getFullName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAvatarUrl(account.getAvatarUrl());
        response.setStatus(account.getStatus());
        response.setLastLoginAt(account.getLastLoginAt());
        return response;
    }

    /**
     * Generates a cryptographically secure numeric OTP of fixed length.
     */
    private String generateOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        // Find account by email - don't throw exception to prevent email enumeration
        Optional<Account> accountOpt = accountRepository.findByEmail(normalizedEmail);

        if (accountOpt.isEmpty()) {
            log.info("Forgot password requested for non-existing email: {}", normalizedEmail);
            // Return silently - don't reveal that email doesn't exist
            return;
        }

        Account account = accountOpt.get();

        // Check account status - also return silently to prevent enumeration
        if ("LOCKED".equals(account.getStatus())) {
            log.info("Forgot password requested for locked account: {}", normalizedEmail);
            return;
        }

        if ("INACTIVE".equals(account.getStatus())) {
            log.info("Forgot password requested for inactive account: {}", normalizedEmail);
            return;
        }

        // Check cooldown
        String cooldownKey = FORGOT_PASSWORD_COOLDOWN_PREFIX + normalizedEmail;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            throw new BaseException(ErrorCode.AUTH_020);
        }

        // Generate OTP
        String otp = generateOtp();

        // Hash OTP before storing in Redis
        String otpHash = passwordEncoder.encode(otp);
        String otpKey = FORGOT_PASSWORD_OTP_PREFIX + normalizedEmail;
        stringRedisTemplate.opsForValue().set(otpKey, otpHash, OTP_TTL);

        // Set cooldown
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", RESEND_COOLDOWN);

        // Reset attempt counter
        String attemptKey = FORGOT_PASSWORD_ATTEMPT_PREFIX + normalizedEmail;
        stringRedisTemplate.delete(attemptKey);

        log.info("Forgot password OTP generated for account: {}", account.getUsername());

        // Publish email event after transaction commits
        final String finalEmail = normalizedEmail;
        final String finalOtp = otp;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Reuse PasswordResetEmailEvent but with OTP instead of token
                PasswordResetEmailEvent event = PasswordResetEmailEvent.of(
                        finalEmail,
                        finalOtp,
                        (int) OTP_TTL.toMinutes()
                );
                eventPublisher.publish(event);
                log.info("Forgot password OTP email event published for: {}", finalEmail);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ForgotPasswordOtpResponse verifyForgotPasswordOtp(String email, String otp) {
        String normalizedEmail = email.trim().toLowerCase();

        // Find account by email
        Account account = accountRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        // Check account status
        if ("LOCKED".equals(account.getStatus())) {
            throw new BaseException(ErrorCode.AUTH_005);
        }

        if ("INACTIVE".equals(account.getStatus())) {
            throw new BaseException(ErrorCode.AUTH_006);
        }

        // Check attempt limit
        String attemptKey = FORGOT_PASSWORD_ATTEMPT_PREFIX + normalizedEmail;
        String attemptCount = stringRedisTemplate.opsForValue().get(attemptKey);
        if (attemptCount != null && Integer.parseInt(attemptCount) >= MAX_OTP_ATTEMPTS) {
            throw new BaseException(ErrorCode.AUTH_019);
        }

        // Get OTP hash from Redis
        String otpKey = FORGOT_PASSWORD_OTP_PREFIX + normalizedEmail;
        String storedOtpHash = stringRedisTemplate.opsForValue().get(otpKey);

        if (storedOtpHash == null) {
            throw new BaseException(ErrorCode.AUTH_017);
        }

        // Verify OTP using passwordEncoder
        if (!passwordEncoder.matches(otp, storedOtpHash)) {
            // Increment attempt counter
            stringRedisTemplate.opsForValue().increment(attemptKey);
            stringRedisTemplate.expire(attemptKey, OTP_TTL);
            throw new BaseException(ErrorCode.AUTH_016);
        }

        // OTP is valid, delete it
        stringRedisTemplate.delete(otpKey);
        stringRedisTemplate.delete(attemptKey);

        // Generate reset token (JWT with special claim)
        String resetToken = jwtTokenProvider.generateResetToken(account.getId());

        log.info("Forgot password OTP verified for account: {}", account.getUsername());

        return ForgotPasswordOtpResponse.builder()
                .resetToken(resetToken)
                .tokenType(Constants.TOKEN_TYPE_BEARER)
                .expiresIn(jwtTokenProvider.getResetTokenExpiresInSeconds())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void resetPassword(String newPassword, String confirmPassword) {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new BaseException(ErrorCode.AUTH_027);
        }

        // Get current authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BaseException(ErrorCode.AUTH_012);
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Account account = accountRepository.findById(principal.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        // Check account status
        if ("LOCKED".equals(account.getStatus())) {
            throw new BaseException(ErrorCode.AUTH_005);
        }

        // Update password
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        // Revoke all refresh tokens for this account (invalidate all sessions)
        refreshTokenRepository.deleteByAccountId(account.getId());

        log.info("Password reset successfully for account: {} - all sessions revoked", account.getUsername());
    }
}
