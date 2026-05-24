package com.kobe.warehouse.service.settings.dto;

import com.kobe.warehouse.domain.enumeration.DeviceType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PosteDeviceRecord(
    Long id,
    @NotNull Integer posteId,
    @NotNull DeviceType deviceType,
    @NotNull String portName,
    String label,
    Integer baudRate,
    Integer vid,
    Integer pid,
    String manufacturer,
    String productName,
    String serialNumber,
    boolean active,
    LocalDateTime lastConnectedAt,
    boolean connected // champ calculé côté Tauri (non persisté)
) {
    /**
     * Constructeur sans le champ connected (par défaut false).
     */
    public PosteDeviceRecord(
        Long id, Integer posteId, DeviceType deviceType, String portName,
        String label, Integer baudRate, Integer vid, Integer pid,
        String manufacturer, String productName, String serialNumber,
        boolean active, LocalDateTime lastConnectedAt
    ) {
        this(id, posteId, deviceType, portName, label, baudRate, vid, pid,
            manufacturer, productName, serialNumber, active, lastConnectedAt, false);
    }

    /** Copie avec posteId forcé (depuis le path variable). */
    public PosteDeviceRecord withPosteId(Integer posteId) {
        return new PosteDeviceRecord(id, posteId, deviceType, portName, label, baudRate,
            vid, pid, manufacturer, productName, serialNumber, active, lastConnectedAt);
    }

    /** Copie avec id et posteId forcés (pour update). */
    public PosteDeviceRecord withIdAndPosteId(Long id, Integer posteId) {
        return new PosteDeviceRecord(id, posteId, deviceType, portName, label, baudRate,
            vid, pid, manufacturer, productName, serialNumber, active, lastConnectedAt);
    }
}

