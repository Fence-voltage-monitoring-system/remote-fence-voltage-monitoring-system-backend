package com.nerdc.elephantfence.backend.notifications.repository;

import com.nerdc.elephantfence.backend.notifications.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND " +
           "(:read IS NULL OR n.read = :read) AND " +
           "(:category IS NULL OR :category = '' OR n.category = :category) " +
           "ORDER BY n.createdAt DESC")
    Page<UserNotification> findWithFilters(
            @Param("userId") UUID userId,
            @Param("read") Boolean read,
            @Param("category") String category,
            Pageable pageable
    );

    long countByUserIdAndRead(UUID userId, boolean read);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.channels LIKE %:channel%")
    long countByUserIdAndChannel(@Param("userId") UUID userId, @Param("channel") String channel);

    @Modifying
    @Query("UPDATE UserNotification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllReadForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.userId = :userId AND n.read = true")
    int deleteReadForUser(@Param("userId") UUID userId);
}
