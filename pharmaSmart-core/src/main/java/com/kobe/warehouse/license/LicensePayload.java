package com.kobe.warehouse.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;

/**
 * Claims d'une licence, tels que signés par l'éditeur (cf. PLAN-GESTION-LICENCE §3.1).
 *
 * <p>Le record est tolérant à l'absence de champs : une licence émise par une version antérieure du
 * CLI doit rester exploitable. Les valeurs par défaut appliquées ici sont donc systématiquement les
 * plus permissives — un défaut restrictif bloquerait des clients en production sans raison.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LicensePayload(
    String licenseId,
    LicenseType licenseType,
    String customerRef,
    String magasinName,
    String magasinFullName,
    String taxId,
    String edition,
    Instant issuedAt,
    LocalDate validFrom,
    LocalDate expiresAt,
    int gracePeriodDays,
    Integer maxUsers,
    Integer maxSales,
    Integer maxProduits,
    Set<Feature> features,
    String hardwareFingerprint,
    BindingPolicy bindingPolicy,
    SupportContacts support
) {
    /** Quotas par défaut de la version de démonstration (§3.5). */
    public static final int DEMO_DEFAULT_MAX_SALES = 500;
    public static final int DEMO_DEFAULT_MAX_PRODUITS = 1_000;

    public LicensePayload {
        licenseType = licenseType == null ? LicenseType.SUBSCRIPTION : licenseType;
        edition = edition == null ? "STANDARD" : edition;
        // Défaut MAGASIN plutôt que NONE : une licence sans politique explicite reste nominative,
        // mais on n'impose pas l'empreinte matérielle, absente des licences historiques.
        bindingPolicy = bindingPolicy == null ? BindingPolicy.MAGASIN : bindingPolicy;
        support = support == null ? SupportContacts.empty() : support;
        gracePeriodDays = Math.max(gracePeriodDays, 0);
        // Une valeur d'enum inconnue est désérialisée en null (READ_UNKNOWN_ENUM_VALUES_AS_NULL) :
        // on l'écarte au lieu de rejeter toute la licence.
        if (features == null) {
            features = EnumSet.noneOf(Feature.class);
        } else {
            EnumSet<Feature> copy = EnumSet.noneOf(Feature.class);
            features.stream().filter(java.util.Objects::nonNull).forEach(copy::add);
            features = copy;
        }
    }

    /**
     * Instant exact d'expiration : fin de la journée {@code expiresAt} en UTC.
     *
     * <p>Une licence « expirant le 15/01 » reste donc utilisable <em>pendant</em> toute la journée
     * du 15 — c'est l'interprétation commerciale attendue par le client.
     */
    public Instant expiryInstant() {
        return expiresAt == null ? Instant.MAX : expiresAt.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Instant de fin de la période de grâce (égal à {@link #expiryInstant()} si grâce nulle). */
    public Instant graceEndInstant() {
        return expiresAt == null ? Instant.MAX : expiryInstant().plus(java.time.Duration.ofDays(gracePeriodDays));
    }

    /** Début de validité, ou {@code Instant.MIN} si le champ n'est pas renseigné. */
    public Instant validFromInstant() {
        return validFrom == null ? Instant.MIN : validFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * {@code true} si le module est accordé.
     *
     * <p>Le périmètre couvert d'office l'est toujours ; seuls les modules {@link Feature#isOptional()
     * optionnels} exigent d'être listés. Voir {@link Feature} pour le raisonnement.
     */
    public boolean hasFeature(Feature feature) {
        return !feature.isOptional() || features.contains(feature);
    }

    /** {@code @JsonIgnore} : dérivé de {@code licenseType}, il n'a pas à figurer dans les claims signés. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isDemo() {
        return licenseType.isDemo();
    }

    /** Quota de ventes applicable, uniquement pertinent pour une licence {@code DEMO}. */
    public int effectiveMaxSales() {
        return maxSales == null ? DEMO_DEFAULT_MAX_SALES : maxSales;
    }

    /** Quota de produits applicable, uniquement pertinent pour une licence {@code DEMO}. */
    public int effectiveMaxProduits() {
        return maxProduits == null ? DEMO_DEFAULT_MAX_PRODUITS : maxProduits;
    }
}
