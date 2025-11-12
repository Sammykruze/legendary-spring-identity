package com.legendaryUser.legendary.controller;



import com.legendaryUser.legendary.security.UserPrincipal;
import com.legendaryUser.legendary.dto.UserProfileDto;
import com.legendaryUser.legendary.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // Protects all endpoints in this controller
public class UserController {

    @Autowired
    private UserService userService;

    // Gets the current user's profile
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        UserProfileDto profile = userService.getUserProfileById(currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    // Gets a user's profile by ID (Secured: only self or admin can access)
    @GetMapping("/{userId}")
    @PreAuthorize("@userSecurity.isSelfOrAdmin(principal, #userId)")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable Long userId) {
        UserProfileDto profile = userService.getUserProfileById(userId);
        return ResponseEntity.ok(profile);
    }

    // Gets all users (Secured: only admin can access)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}

