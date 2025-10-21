package com.legendaryUser.legendary.repository;


import com.legendaryUser.legendary.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUserId(Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiryDate < :now")
    void deleteAllExpiredSince(@Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.user.email = :email")
    void deleteByUserEmail(@Param("email") String email);
}

