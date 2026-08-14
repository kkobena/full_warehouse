package com.kobe.warehouse.domain.enumeration;

/**
 * Nature d'un événement journalisé dans {@code license_audit} (cf. PLAN-GESTION-LICENCE §4.1).
 */
public enum LicenseAuditEventType {
    /** Dépôt réussi d'un fichier de licence. */
    ACTIVATION,
    /** Dépôt refusé : signature invalide, officine ou empreinte divergente, licence expirée. */
    REJECTION,
    /** Passage constaté en statut expiré. */
    EXPIRATION,
    /** Recul d'horloge système détecté. */
    CLOCK_TAMPER,
    /** Écriture refusée pour cause de licence (B4). */
    BLOCKED_WRITE,
    /** Le nom de l'officine ne correspond plus à celui porté par la licence. */
    MAGASIN_NAME_MISMATCH,
    /** Le n° contribuable renseigné diverge de celui de la licence. */
    TAX_ID_MISMATCH,
    /** L'empreinte matérielle du poste diverge de celle de la licence. */
    FINGERPRINT_MISMATCH
}
