package com.legendaryUser.legendary.security;

import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    public boolean isSelfOrAdmin(UserPrincipal principal, Long userId) {
        // Check if the current user is an ADMIN
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        // Check if the current user is accessing their own data
        boolean isSelf = principal.getId().equals(userId);

        // Allow access if either condition is true
        return isAdmin || isSelf;
    }
}

