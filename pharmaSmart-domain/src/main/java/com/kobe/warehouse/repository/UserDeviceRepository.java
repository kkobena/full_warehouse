package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserDevice entity.
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    /**
     * Find device by FCM token.
     */
    Optional<UserDevice> findByFcmToken(String fcmToken);

    /**
     * Find all devices for a specific user.
     */
    List<UserDevice> findByUserId(Integer userId);

    /**
     * Find all devices for a user with notifications enabled.
     */
    List<UserDevice> findByUserIdAndNotificationsEnabled(Integer userId, Boolean notificationsEnabled);

    /**
     * Find all devices by user authority (role) with notifications enabled.
     */
    @Query("SELECT ud FROM UserDevice ud " +
        "JOIN ud.user u " +
        "JOIN u.authorities a " +
        "WHERE a.name = :authority " +
        "AND ud.notificationsEnabled = :notificationsEnabled")
    List<UserDevice> findByUserAuthorityAndNotificationsEnabled(
        @Param("authority") String authority,
        @Param("notificationsEnabled") Boolean notificationsEnabled
    );

    /**
     * Find all devices with notifications enabled.
     */
    List<UserDevice> findByNotificationsEnabled(Boolean notificationsEnabled);

    /**
     * Delete device by FCM token (for cleanup of invalid tokens).
     */
    void deleteByFcmToken(String fcmToken);

    /**
     * Check if device exists for token.
     */
    boolean existsByFcmToken(String fcmToken);

    /**
     * Delete all devices for a user.
     */
    void deleteByUserId(Integer userId);
}
