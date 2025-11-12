package com.legendaryUser.legendary.controller;


import com.legendaryUser.legendary.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")  // Only admins can access these endpoints
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/promote/{email}")
    public ResponseEntity<?> promoteToAdmin(@PathVariable String email) {
        try {
            authService.promoteToAdmin(email);
            logger.info("User {} promoted to ADMIN successfully", email);
            return ResponseEntity.ok().body("User " + email + " promoted to ADMIN successfully");
        } catch (RuntimeException e) {
            logger.error("Failed to promote user {} to ADMIN: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/demote/{email}")
    public ResponseEntity<?> demoteToUser(@PathVariable String email) {
        try {
            authService.demoteToUser(email);
            logger.info("User {} demoted to USER successfully", email);
            return ResponseEntity.ok().body("User " + email + " demoted to USER successfully");
        } catch (RuntimeException e) {
            logger.error("Failed to demote user {} to USER: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/check-admin/{email}")
    public ResponseEntity<?> checkAdminStatus(@PathVariable String email) {
        try {
            boolean isAdmin = authService.isAdmin(email);
            return ResponseEntity.ok().body(isAdmin ? "ADMIN" : "USER");
        } catch (RuntimeException e) {
            logger.error("Failed to check admin status for {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

