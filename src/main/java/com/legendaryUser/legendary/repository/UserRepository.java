package com.legendaryUser.legendary.repository;

import com.legendaryUser.legendary.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = :failAttempts WHERE u.email = :email")
    void updateFailedAttempts(@Param("failAttempts") int failAttempts, @Param("email") String email);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = :accountNonLocked, u.lockTime = :lockTime WHERE u.email = :email")
    void lockUser(@Param("email") String email,
                  @Param("accountNonLocked") boolean accountNonLocked,
                  @Param("lockTime") LocalDateTime lockTime);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = true, u.lockTime = null, u.failedAttempts = 0 WHERE u.email = :email")
    void unlockUser(@Param("email") String email);
}

