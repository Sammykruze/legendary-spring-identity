package com.legendaryUser.legendary.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Allow 5 requests per hour per IP for OTP generation
    private static final Bandwidth OTP_LIMIT = Bandwidth.classic(5, Refill.greedy(5, Duration.ofHours(1)));

    public Bucket resolveBucket(String ipAddress) {
        return cache.computeIfAbsent(ipAddress, this::newBucket);
    }

    private Bucket newBucket(String ipAddress) {
        return Bucket.builder()
                .addLimit(OTP_LIMIT)
                .build();
    }

    public void cleanUp() {
        // Optional: periodically clean up old entries
        cache.entrySet().removeIf(entry ->
                entry.getValue().getAvailableTokens() == OTP_LIMIT.getCapacity()
        );
    }
}

