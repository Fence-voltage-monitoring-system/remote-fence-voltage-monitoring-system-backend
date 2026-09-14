package com.nerdc.elephantfence.backend.notifications.repository;

import com.nerdc.elephantfence.backend.notifications.entity.UserNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, UUID> {
}
