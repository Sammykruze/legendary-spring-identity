package com.legendaryUser.legendary.controller;

import com.legendaryUser.legendary.dto.AuthResponse;
import com.legendaryUser.legendary.dto.LoginRequest;
import com.legendaryUser.legendary.dto.OtpVerificationRequest;
import com.legendaryUser.legendary.dto.RegisterRequest;
import com.legendaryUser.legendary.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.frontend.url}", maxAge = 3600)
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
                                          HttpServletRequest request) {
        try {
            String clientIp = getClientIp(request);
            authService.registerUser(registerRequest, clientIp);

            return ResponseEntity.ok().body("Registration successful. Please check your email for verification.");
        } catch (Exception e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok().body("Email verified successfully. You can now login.");
        } catch (Exception e) {
            logger.error("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
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
        String remoteAddr = request.getRemoteAddr();

        // Check for X-Forwarded-For header in case of proxies
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            remoteAddr = xForwardedFor.split(",")[0].trim();
        }

        return remoteAddr;
    }
}

