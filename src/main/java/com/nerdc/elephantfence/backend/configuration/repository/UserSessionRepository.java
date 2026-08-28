package com.nerdc.elephantfence.backend.configuration.repository;

import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    @Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.expiresAt > :now")
    List<UserSession> findAllActive(@Param("now") OffsetDateTime now);

    @Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.user.id = :userId AND s.expiresAt > :now")
    List<UserSession> findActiveByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.id = :sessionId")
    int revokeSession(@Param("sessionId") String sessionId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = :userId")
    int revokeAllUserSessions(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt <= :now")
    void deleteExpiredSessions(@Param("now") OffsetDateTime now);
}
