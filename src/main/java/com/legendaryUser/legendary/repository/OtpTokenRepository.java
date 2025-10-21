package com.legendaryUser.legendary.repository;

import com.legendaryUser.legendary.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByToken(String token);
    Optional<OtpToken> findByUserId(Long userId);

    @Query("SELECT ot FROM OtpToken ot WHERE ot.user.email = :email AND ot.used = false AND ot.expiryDate > :now ORDER BY ot.createdAt DESC")
    Optional<OtpToken> findLatestValidOtpByUserEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.expiryDate < :now")
    void deleteAllExpiredSince(@Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("UPDATE OtpToken ot SET ot.used = true WHERE ot.token = :token")
    void markAsUsed(@Param("token") String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.user.email = :email")
    void deleteByUserEmail(@Param("email") String email);
}

