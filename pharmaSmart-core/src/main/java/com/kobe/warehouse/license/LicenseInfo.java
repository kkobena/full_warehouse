package com.kobe.warehouse.license;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Vue applicative du statut de licence : ce qui est renvoyé au frontend par
 * {@code GET /api/license/status} et ce que consultent l'aspect d'enforcement et les services métier.
 *
 * <p>Contrairement à {@link LicensePayload}, ce record ne contient rien de confidentiel et peut être
 * exposé à un utilisateur non administrateur (le caissier doit voir la bannière et l'échéance).
 */
public record LicenseInfo(
    LicenseStatus status,
    LicenseType licenseType,
    String edition,
    LocalDate expiresAt,
    long daysRemaining,
    String magasinName,
    Set<Feature> features,
    boolean readOnly,
    boolean demo,
    boolean showBanner,
    String message,
    SupportContacts support
) {
    /**
     * Statut renvoyé lorsque le contrôle de licence est désactivé
     * ({@code pharma-smart.license.enabled=false} en dev et dans les tests).
     */
    public static LicenseInfo disabled() {
        return new LicenseInfo(
            LicenseStatus.VALID,
            LicenseType.PARTNER,
            "STANDARD",
            null,
            Long.MAX_VALUE,
            null,
            // Tous les modules, options comprises : contrôle désactivé signifie « rien n'est bridé ».
            // Renvoyer un jeu vide masquerait les écrans optionnels en développement et dans les tests.
            EnumSet.allOf(Feature.class),
            false,
            false,
            false,
            "Contrôle de licence désactivé",
            SupportContacts.empty()
        );
    }

    /** Aucun fichier de licence trouvé, ni sur disque ni en base. */
    public static LicenseInfo missing(SupportContacts support) {
        return new LicenseInfo(
            LicenseStatus.MISSING,
            LicenseType.SUBSCRIPTION,
            "STANDARD",
            null,
            0,
            null,
            Set.of(),
            true,
            false,
            true,
            "Aucune licence n'est installée. Rendez-vous dans « Gérer ma licence » pour activer votre abonnement.",
            support == null ? SupportContacts.empty() : support
        );
    }

    /**
     * L'évaluation n'a pas pu aboutir pour une raison <em>technique</em> (base injoignable, disque
     * en erreur) et aucun statut antérieur n'est connu.
     *
     * <p>Trois choix délibérés :
     *
     * <ul>
     *   <li><strong>Pas de lecture seule</strong> — voir {@link LicenseStatus#UNKNOWN} ;
     *   <li><strong>Pas de bannière ni de message technique</strong> — l'utilisateur ne peut rien
     *       faire d'une panne d'infrastructure, et le détail (message JDBC, requête SQL) n'a rien à
     *       faire dans une interface de caisse : il reste dans les logs, à destination du support ;
     *   <li><strong>Tous les modules</strong> — masquer les menus optionnels sur un incident
     *       transitoire ferait croire à une perte d'abonnement.
     * </ul>
     */
    public static LicenseInfo unavailable() {
        return new LicenseInfo(
            LicenseStatus.UNKNOWN,
            LicenseType.SUBSCRIPTION,
            "STANDARD",
            null,
            0,
            null,
            EnumSet.allOf(Feature.class),
            false,
            false,
            false,
            "",
            SupportContacts.empty()
        );
    }

    /**
     * {@code true} si le module est accordé.
     *
     * <p>Le périmètre couvert d'office l'est toujours ; seuls les modules {@link Feature#isOptional()
     * optionnels} exigent d'être listés dans la licence (§3.6).
     */
    public boolean hasFeature(Feature feature) {
        return !feature.isOptional() || features.contains(feature);
    }

    /** Libellé des modules souscrits, pour l'affichage. */
    public List<String> featureNames() {
        return features.stream().map(Enum::name).sorted().toList();
    }
}
