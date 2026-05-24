package com.kobe.warehouse.web.rest.mobile;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.UserDevice;
import com.kobe.warehouse.repository.UserDeviceRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for mobile device management.
 * Handles device registration for push notifications and notification settings.
 */
@RestController
@RequestMapping("/api/mobile/devices")
public class MobileDeviceResource {

    private static final Logger LOG = LoggerFactory.getLogger(MobileDeviceResource.class);

    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;

    public MobileDeviceResource(
        UserDeviceRepository userDeviceRepository,
        UserRepository userRepository
    ) {
        this.userDeviceRepository = userDeviceRepository;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/mobile/devices/register
     * Register a device for push notifications.
     *
     * @param request Device registration request
     * @return Success response with device ID
     */
    @PostMapping("/register")
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(
        @Valid @RequestBody DeviceRegistrationRequest request
    ) {
        LOG.debug("REST request to register device: {}", request.deviceName());

        Optional<String> currentUserLogin = SecurityUtils.getCurrentUserLogin();
        if (currentUserLogin.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<AppUser> userOptional = userRepository.findOneByLogin(currentUserLogin.get());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        AppUser user = userOptional.get();

        // Check if device already exists
        Optional<UserDevice> existingDevice = userDeviceRepository.findByFcmToken(request.fcmToken());

        UserDevice device;
        if (existingDevice.isPresent()) {
            // Update existing device
            device = existingDevice.get();
            device.setDeviceName(request.deviceName());
            device.setDeviceModel(request.deviceModel());
            device.setOsVersion(request.osVersion());
            device.setAppVersion(request.appVersion());
            device.setLastActiveAt(Instant.now());
            LOG.debug("Updating existing device: {}", device.getId());
        } else {
            // Create new device
            device = new UserDevice();
            device.setFcmToken(request.fcmToken());
            device.setUser(user);
            device.setDeviceName(request.deviceName());
            device.setDeviceModel(request.deviceModel());
            device.setOsVersion(request.osVersion());
            device.setAppVersion(request.appVersion());
            device.setNotificationsEnabled(true);
            device.setRegisteredAt(Instant.now());
            device.setLastActiveAt(Instant.now());
            LOG.debug("Creating new device for user: {}", user.getLogin());
        }

        device = userDeviceRepository.save(device);

        return ResponseEntity.ok(new DeviceRegistrationResponse(
            device.getId(),
            "Device registered successfully",
            device.getNotificationsEnabled()
        ));
    }

    /**
     * PUT /api/mobile/devices/settings
     * Update device notification settings.
     *
     * @param request Settings update request
     * @return Success response
     */
    @PutMapping("/settings")
    public ResponseEntity<DeviceSettingsResponse> updateDeviceSettings(
        @Valid @RequestBody DeviceSettingsRequest request
    ) {
        LOG.debug("REST request to update device settings for token: {}",
            request.fcmToken().substring(0, Math.min(20, request.fcmToken().length())) + "...");

        Optional<UserDevice> deviceOptional = userDeviceRepository.findByFcmToken(request.fcmToken());

        if (deviceOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserDevice device = deviceOptional.get();
        device.setNotificationsEnabled(request.notificationsEnabled());
        device.setLastActiveAt(Instant.now());

        device = userDeviceRepository.save(device);

        return ResponseEntity.ok(new DeviceSettingsResponse(
            "Settings updated successfully",
            device.getNotificationsEnabled()
        ));
    }

    /**
     * DELETE /api/mobile/devices
     * Unregister a device.
     *
     * @param fcmToken FCM token
     * @return No content response
     */
    @DeleteMapping
    public ResponseEntity<Void> unregisterDevice(@RequestParam String fcmToken) {
        LOG.debug("REST request to unregister device");

        userDeviceRepository.deleteByFcmToken(fcmToken);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/mobile/devices/status
     * Check if device is registered and get its status.
     *
     * @param fcmToken FCM token
     * @return Device status
     */
    @GetMapping("/status")
    public ResponseEntity<DeviceStatusResponse> getDeviceStatus(@RequestParam String fcmToken) {
        LOG.debug("REST request to check device status");

        Optional<UserDevice> deviceOptional = userDeviceRepository.findByFcmToken(fcmToken);

        if (deviceOptional.isEmpty()) {
            return ResponseEntity.ok(new DeviceStatusResponse(
                false,
                null,
                false
            ));
        }

        UserDevice device = deviceOptional.get();
        return ResponseEntity.ok(new DeviceStatusResponse(
            true,
            device.getId(),
            device.getNotificationsEnabled()
        ));
    }

    /**
     * PUT /api/mobile/devices/heartbeat
     * Update device last active timestamp.
     *
     * @param fcmToken FCM token
     * @return Success response
     */
    @PutMapping("/heartbeat")
    public ResponseEntity<Void> deviceHeartbeat(@RequestParam String fcmToken) {
        LOG.debug("REST request for device heartbeat");

        Optional<UserDevice> deviceOptional = userDeviceRepository.findByFcmToken(fcmToken);

        if (deviceOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserDevice device = deviceOptional.get();
        device.setLastActiveAt(Instant.now());
        userDeviceRepository.save(device);

        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // REQUEST/RESPONSE RECORDS
    // =========================================================================

    /**
     * Device registration request
     */
    public record DeviceRegistrationRequest(
        String fcmToken,
        String deviceName,
        String deviceModel,
        String osVersion,
        String appVersion
    ) {}

    /**
     * Device registration response
     */
    public record DeviceRegistrationResponse(
        Integer deviceId,
        String message,
        Boolean notificationsEnabled
    ) {}

    /**
     * Device settings request
     */
    public record DeviceSettingsRequest(
        String fcmToken,
        Boolean notificationsEnabled
    ) {}

    /**
     * Device settings response
     */
    public record DeviceSettingsResponse(
        String message,
        Boolean notificationsEnabled
    ) {}

    /**
     * Device status response
     */
    public record DeviceStatusResponse(
        Boolean isRegistered,
        Integer deviceId,
        Boolean notificationsEnabled
    ) {}
}
