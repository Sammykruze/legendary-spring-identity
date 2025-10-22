package com.legendaryUser.legendary.scheduler;

import com.legendaryUser.legendary.service.AuthService;
import com.legendaryUser.legendary.service.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private RateLimitService rateLimitService;

    // Run every hour to clean up expired tokens
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupExpiredTokens() {
        try {
            authService.cleanupExpiredTokens();
            logger.info("Scheduled token cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled token cleanup failed: {}", e.getMessage());
        }
    }

    // Run every 6 hours to clean up rate limit cache
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void cleanupRateLimitCache() {
        try {
            rateLimitService.cleanUp();
            logger.info("Scheduled rate limit cache cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled rate limit cache cleanup failed: {}", e.getMessage());
        }
    }
}

