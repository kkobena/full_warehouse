package com.kobe.warehouse.service.license.dto;

import com.kobe.warehouse.domain.LicenseAudit;
import com.kobe.warehouse.domain.enumeration.LicenseAuditEventType;
import java.time.Instant;

/** Ligne du journal de licence, telle qu'affichée dans l'écran {@code admin/license}. */
public record LicenseAuditDTO(Long id, LicenseAuditEventType eventType, String detail, String licenseId, String userLogin, Instant createdAt) {
    public LicenseAuditDTO(LicenseAudit entity) {
        this(entity.getId(), entity.getEventType(), entity.getDetail(), entity.getLicenseId(), entity.getUserLogin(), entity.getCreatedAt());
    }
}
