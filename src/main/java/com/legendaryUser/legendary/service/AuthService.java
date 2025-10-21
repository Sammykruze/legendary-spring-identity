package com.legendaryUser.legendary.service;

import com.legendaryUser.legendary.dto.AuthResponse;
import com.legendaryUser.legendary.dto.LoginRequest;
import com.legendaryUser.legendary.dto.OtpVerificationRequest;
import com.legendaryUser.legendary.dto.RegisterRequest;
import com.legendaryUser.legendary.exception.*;
import com.legendaryUser.legendary.model.OtpToken;
import com.legendaryUser.legendary.model.User;
import com.legendaryUser.legendary.model.VerificationToken;
import com.legendaryUser.legendary.repository.OtpTokenRepository;
import com.legendaryUser.legendary.repository.UserRepository;
import com.legendaryUser.legendary.repository.VerificationTokenRepository;
import com.legendaryUser.legendary.security.JwtUtils;
import com.legendaryUser.legendary.security.UserPrincipal;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION_MINUTES = 30;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RateLimitService rateLimitService;

    @Value("${app.security.otp.length:6}")
    private int otpLength;

    @Transactional
    public void registerUser(RegisterRequest registerRequest, String clientIp) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        // Check rate limiting
        Bucket bucket = rateLimitService.resolveBucket(clientIp);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many registration attempts. Please try again later.");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEnabled(false);

        userRepository.save(user);

        String token = emailService.generateEmailVerificationToken();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationTokenRepository.save(verificationToken);

        emailService.sendEmailVerification(user.getEmail(), token, user.getFirstName());

        logger.info("User registered successfully: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            throw new TokenExpiredException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);

        logger.info("Email verified successfully for user: {}", user.getEmail());
    }

    @Transactional
    public void requestOtp(LoginRequest loginRequest, String clientIp) {
        // Check rate limiting
        Bucket bucket = rateLimitService.resolveBucket(clientIp);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Too many OTP requests. Please try again later.");
        }

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            if (user.getLockTime().plusMinutes(LOCK_TIME_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                throw new AccountLockedException("Account is locked. Please try again later.");
            } else {
                // Auto-unlock after lock time duration
                user.unlockAccount();
                userRepository.save(user);
            }
        }

        // Check if user is enabled
        if (!user.isEnabled()) {
            throw new RuntimeException("Account is not verified. Please verify your email first.");
        }

        // Generate OTP
        String otp = generateOtp();
        OtpToken otpToken = new OtpToken(otp, user);
        otpTokenRepository.save(otpToken);

        // Send OTP via email
        emailService.sendOtpEmail(user.getEmail(), otp, user.getFirstName());

        logger.info("OTP sent to user: {}", user.getEmail());
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerificationRequest otpRequest) {
        User user = userRepository.findByEmail(otpRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new AccountLockedException("Account is locked. Please try again later.");
        }

        Optional<OtpToken> otpTokenOptional = otpTokenRepository.findLatestValidOtpByUserEmail(
                otpRequest.getEmail(), LocalDateTime.now());

        if (otpTokenOptional.isEmpty()) {
            incrementFailedAttempts(user);
            throw new InvalidOtpException("Invalid or expired OTP");
        }

        OtpToken otpToken = otpTokenOptional.get();

        if (!otpToken.getToken().equals(otpRequest.getOtp())) {
            incrementFailedAttempts(user);
            otpTokenRepository.markAsUsed(otpToken.getToken());
            throw new InvalidOtpException("Invalid OTP");
        }

        // OTP is valid
        otpTokenRepository.markAsUsed(otpToken.getToken());
        user.resetFailedAttempts();
        userRepository.save(user);

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(otpRequest.getEmail(), "otp-based-auth"));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        logger.info("User logged in successfully with OTP: {}", user.getEmail());

        return new AuthResponse(jwt, userPrincipal.getId(), userPrincipal.getUsername(),
                user.getFirstName(), user.getLastName());
    }

    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }

    private void incrementFailedAttempts(User user) {
        user.incrementFailedAttempts();

        if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount();
            logger.warn("Account locked due to too many failed attempts: {}", user.getEmail());
        }

        userRepository.save(user);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        verificationTokenRepository.deleteAllExpiredSince(now);
        otpTokenRepository.deleteAllExpiredSince(now);
        logger.info("Cleaned up expired tokens");
    }
}

