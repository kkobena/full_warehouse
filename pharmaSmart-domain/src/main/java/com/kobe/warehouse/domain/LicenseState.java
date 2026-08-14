package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * État de la licence installée sur ce poste — table <strong>singleton</strong> ({@code id = 1}).
 *
 * <p>Une installation ne porte qu'une licence à la fois ; la contrainte {@code CHECK (id = 1)} rend
 * l'invariant impossible à violer, y compris depuis un accès direct à la base.
 *
 * <p>Le jeton signé y est recopié : supprimer le fichier {@code .lic} du disque ne suffit donc pas
 * à faire disparaître la licence, qui est rechargée depuis la base (cf. PLAN-GESTION-LICENCE §3.3).
 * À l'inverse, restaurer un ancien dump ne rajeunit rien — {@code expiresAt} est signé dans le
 * jeton, pas lu dans cette table.
 */
@Entity
@Table(name = "license_state")
public class LicenseState implements Serializable {

    /** Identifiant unique de la ligne singleton. */
    public static final int SINGLETON_ID = 1;

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private Integer id = SINGLETON_ID;

    /** JWS complet, copie de secours du fichier {@code license.lic}. */
    @Column(name = "license_token", columnDefinition = "text")
    private String licenseToken;

    @Column(name = "license_id", length = 64)
    private String licenseId;

    /** Dénormalisé depuis les claims, pour le reporting et le support — jamais pour décider. */
    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "activated_by", length = 50)
    private String activatedBy;

    /**
     * Dernier instant où l'application s'est vue tourner. Une horloge système qui repart nettement
     * en arrière par rapport à cette sonde trahit une manipulation (§3.3).
     */
    @Column(name = "last_seen_instant")
    private Instant lastSeenInstant;

    @Column(name = "hardware_fingerprint", length = 128)
    private String hardwareFingerprint;

    /**
     * Instant de la première divergence d'empreinte constatée, ou {@code null} si l'empreinte
     * correspond. C'est cette date qui ouvre le délai de régularisation avant blocage : un
     * changement de serveur légitime ne doit pas arrêter l'officine du jour au lendemain (§3.4).
     */
    @Column(name = "fingerprint_mismatch_since")
    private Instant fingerprintMismatchSince;

    public Integer getId() {
        return id;
    }

    public LicenseState setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getLicenseToken() {
        return licenseToken;
    }

    public LicenseState setLicenseToken(String licenseToken) {
        this.licenseToken = licenseToken;
        return this;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public LicenseState setLicenseId(String licenseId) {
        this.licenseId = licenseId;
        return this;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public LicenseState setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public LicenseState setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
        return this;
    }

    public String getActivatedBy() {
        return activatedBy;
    }

    public LicenseState setActivatedBy(String activatedBy) {
        this.activatedBy = activatedBy;
        return this;
    }

    public Instant getLastSeenInstant() {
        return lastSeenInstant;
    }

    public LicenseState setLastSeenInstant(Instant lastSeenInstant) {
        this.lastSeenInstant = lastSeenInstant;
        return this;
    }

    public String getHardwareFingerprint() {
        return hardwareFingerprint;
    }

    public LicenseState setHardwareFingerprint(String hardwareFingerprint) {
        this.hardwareFingerprint = hardwareFingerprint;
        return this;
    }

    public Instant getFingerprintMismatchSince() {
        return fingerprintMismatchSince;
    }

    public LicenseState setFingerprintMismatchSince(Instant fingerprintMismatchSince) {
        this.fingerprintMismatchSince = fingerprintMismatchSince;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LicenseState other)) {
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
        return "LicenseState{licenseId='" + licenseId + "', expiresAt=" + expiresAt + ", activatedAt=" + activatedAt + '}';
    }
}
