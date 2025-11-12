package com.legendaryUser.legendary.controller;

import com.legendaryUser.legendary.dto.AuthResponse;
import com.legendaryUser.legendary.dto.LoginRequest;
import com.legendaryUser.legendary.dto.OtpVerificationRequest;
import com.legendaryUser.legendary.dto.RegisterRequest;
import com.legendaryUser.legendary.exception.EmailAlreadyExistsException;
import com.legendaryUser.legendary.exception.InvalidTokenException;
import com.legendaryUser.legendary.exception.RateLimitExceededException;
import com.legendaryUser.legendary.exception.TokenExpiredException;
import com.legendaryUser.legendary.service.AuthService;
import com.legendaryUser.legendary.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${app.frontend.url}", maxAge = 3600)
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;


    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Test endpoint works!");
    }


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                                          HttpServletRequest request) {
        logger.info("Received registration request for: {}", registerRequest.getEmail());

        try {
            String clientIp = getClientIp(request);
            logger.info("Client IP: {}", clientIp);

            authService.registerUser(registerRequest, clientIp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful! Please check your email for verification instructions.");
            response.put("email", registerRequest.getEmail());
            response.put("timestamp", System.currentTimeMillis());

            logger.info("Registration request processed successfully for: {}", registerRequest.getEmail());
            return ResponseEntity.ok().body(response);

        } catch (EmailAlreadyExistsException e) {
            logger.warn("Registration failed - Email already exists: {}", registerRequest.getEmail());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "This email address is already registered. Please use a different email or try logging in.");
            errorResponse.put("email", registerRequest.getEmail());
            errorResponse.put("errorCode", "EMAIL_ALREADY_EXISTS");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(409).body(errorResponse); // 409 Conflict

        } catch (RateLimitExceededException e) {
            logger.warn("Registration failed - Rate limit exceeded for IP: {}", getClientIp(request));

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Too many registration attempts from your location. Please try again in a few minutes.");
            errorResponse.put("email", registerRequest.getEmail());
            errorResponse.put("errorCode", "RATE_LIMIT_EXCEEDED");
            errorResponse.put("retryAfter", "5 minutes");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(429).body(errorResponse); // 429 Too Many Requests

        } catch (Exception e) {
            logger.error("Registration failed for {}: {}", registerRequest.getEmail(), e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Registration failed due to a server error. Please try again later.");
            errorResponse.put("email", registerRequest.getEmail());
            errorResponse.put("errorCode", "INTERNAL_SERVER_ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse); // 500 Internal Server Error
        }
    }


    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        logger.info("Received email verification request for token: {}", token);

        try {
            authService.verifyEmail(token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verified successfully! You can now log in to your account.");
            response.put("timestamp", System.currentTimeMillis());

            logger.info("Email verification successful for token: {}", token);
            return ResponseEntity.ok().body(response);

        } catch (InvalidTokenException e) {
            logger.warn("Email verification failed - Invalid token: {}", token);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "The verification link is invalid. Please request a new verification email.");
            errorResponse.put("errorCode", "INVALID_TOKEN");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(400).body(errorResponse); // 400 Bad Request

        } catch (TokenExpiredException e) {
            logger.warn("Email verification failed - Expired token: {}", token);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "The verification link has expired. Please request a new verification email.");
            errorResponse.put("errorCode", "TOKEN_EXPIRED");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(400).body(errorResponse); // 400 Bad Request

        } catch (Exception e) {
            logger.error("Email verification failed for token {}: {}", token, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Email verification failed due to a server error. Please try again later.");
            errorResponse.put("errorCode", "INTERNAL_SERVER_ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse); // 500 Internal Server Error
        }
    }

    // Add this method for resending verification emails
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestParam String email,
                                                     HttpServletRequest request) {
        logger.info("Received resend verification request for: {}", email);

        try {
            String clientIp = getClientIp(request);
            logger.info("Client IP: {}", clientIp);

            // You'll need to implement this method in AuthService
            authService.resendVerificationEmail(email, clientIp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification email sent successfully. Please check your inbox.");
            response.put("email", email);
            response.put("timestamp", System.currentTimeMillis());

            logger.info("Verification email resent successfully for: {}", email);
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            logger.error("Resend verification failed for {}: {}", email, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to resend verification email. Please try again later.");
            errorResponse.put("email", email);
            errorResponse.put("errorCode", "RESEND_FAILED");
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody LoginRequest loginRequest,
                                        HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            authService.requestOtp(loginRequest, clientIp);

            return ResponseEntity.ok().body("OTP sent to your email address.");
        } catch (Exception e) {
            logger.error("OTP request failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationRequest otpRequest) {
        try {
            AuthResponse authResponse = authService.verifyOtp(otpRequest);
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            logger.error("OTP verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

