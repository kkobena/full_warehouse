package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.LicenseAuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Journal des événements de licence — table <strong>append-only</strong> : aucune mise à jour ni
 * suppression n'est prévue par le code.
 *
 * <p>Le dispositif n'a pas vocation à être inviolable, ce qui est hors d'atteinte en on-premise où
 * le client administre sa machine ; il vise un coût de contournement supérieur au prix de
 * l'abonnement, et cette table en est la trace opposable (cf. PLAN-GESTION-LICENCE §3.3).
 */
@Entity
@Table(name = "license_audit")
public class LicenseAudit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private LicenseAuditEventType eventType;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "license_id", length = 64)
    private String licenseId;

    @Column(name = "user_login", length = 50)
    private String userLogin;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public LicenseAudit() {}

    public LicenseAudit(LicenseAuditEventType eventType, String detail, String licenseId, String userLogin) {
        this.eventType = eventType;
        this.detail = detail;
        this.licenseId = licenseId;
        this.userLogin = userLogin;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public LicenseAudit setId(Long id) {
        this.id = id;
        return this;
    }

    public LicenseAuditEventType getEventType() {
        return eventType;
    }

    public LicenseAudit setEventType(LicenseAuditEventType eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getDetail() {
        return detail;
    }

    public LicenseAudit setDetail(String detail) {
        this.detail = detail;
        return this;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public LicenseAudit setLicenseId(String licenseId) {
        this.licenseId = licenseId;
        return this;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public LicenseAudit setUserLogin(String userLogin) {
        this.userLogin = userLogin;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public LicenseAudit setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LicenseAudit other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "LicenseAudit{eventType=" + eventType + ", licenseId='" + licenseId + "', createdAt=" + createdAt + '}';
    }
}
