/**
 * Miroir de `com.kobe.warehouse.license.LicenseInfo` (backend).
 *
 * Cf. docs/PLAN-GESTION-LICENCE.md §5.1.
 */

export type LicenseStatus =
  | 'VALID'
  | 'EXPIRING_SOON'
  | 'EXPIRING_CRITICAL'
  | 'GRACE'
  | 'EXPIRED'
  | 'MISSING'
  | 'INVALID'
  | 'CLOCK_TAMPERED'
  | 'DEMO_QUOTA_REACHED'
  /** Contrôle indisponible (base injoignable au démarrage) : ni bannière, ni restriction. */
  | 'UNKNOWN';

export type LicenseType = 'DEMO' | 'TRIAL' | 'SUBSCRIPTION' | 'PARTNER';

export interface SupportContacts {
  resellerName?: string;
  phones: string[];
  emails: string[];
  whatsapp?: string;
  website?: string;
}

export interface LicenseInfo {
  status: LicenseStatus;
  licenseType: LicenseType;
  /** Libellé commercial (STANDARD / PREMIUM) — la vérité technique est portée par `features`. */
  edition: string;
  /** ISO date, absente si aucune licence exploitable n'est installée. */
  expiresAt?: string;
  /** Négatif si l'échéance est dépassée. */
  daysRemaining: number;
  /** Raison sociale **portée par la licence**, à comparer à celle de l'officine. */
  magasinName?: string;
  features: string[];
  readOnly: boolean;
  demo: boolean;
  /** Calculé côté serveur : une démo affiche sa bannière quel que soit le temps restant. */
  showBanner: boolean;
  message: string;
  support: SupportContacts;
}

/**
 * Miroir de `PublisherContactsDTO` — coordonnées de l'**éditeur**, issues de la configuration.
 *
 * Distinctes de `SupportContacts`, qui identifie le *revendeur* et n'existe qu'une fois la licence
 * valide : celles-ci sont disponibles dès la première installation, sans licence.
 */
export interface PublisherContacts {
  name?: string;
  emails: string[];
  phones: string[];
}

export interface LicenseAuditEntry {
  id: number;
  eventType:
    | 'ACTIVATION'
    | 'REJECTION'
    | 'EXPIRATION'
    | 'CLOCK_TAMPER'
    | 'BLOCKED_WRITE'
    | 'MAGASIN_NAME_MISMATCH'
    | 'TAX_ID_MISMATCH'
    | 'FINGERPRINT_MISMATCH';
  detail?: string;
  licenseId?: string;
  userLogin?: string;
  createdAt: string;
}

/**
 * Miroir de l'enum `Feature` du backend, **drapeau `optional` compris**.
 *
 * Recopié ici pour deux raisons : l'écran de licence affiche aussi les modules **non** souscrits —
 * le client doit voir ce qu'il peut acheter en plus, ce que la seule liste du serveur ne permet
 * pas — et `LicenseService.hasFeature()` a besoin de savoir quels modules sont optionnels pour
 * appliquer la même règle d'octroi que le backend.
 *
 * `optional: false` = périmètre couvert d'office par toute licence.
 * `optional: true`  = module vendu séparément, accordé seulement s'il est listé dans la licence.
 *
 * ⚠ Toute évolution de `Feature.java` doit être répercutée ici, sinon le client et le serveur
 * divergeraient sur ce qui est accordé.
 */
export const ALL_FEATURES: readonly { code: string; label: string; optional: boolean }[] = [
  {code: 'CAISSE', label: 'Caisse / ventes', optional: false},
  {code: 'FACTURATION', label: 'Tiers-payant & facturation', optional: false},
  {code: 'COMPTABILITE', label: 'Comptabilité', optional: false},
  {code: 'INVENTAIRE_AVANCE', label: 'Inventaire avancé', optional: false},
  {code: 'REPORTS_AVANCES', label: 'Rapports avancés', optional: false},
  {code: 'MOBILE', label: 'Applications mobiles', optional: false},
  {code: 'FNE', label: 'Facture normalisée (FNE)', optional: false},
  {code: 'MULTI_DEPOT', label: 'Multi-dépôts', optional: false}
];

/** Modules exigeant une souscription explicite. */
export const OPTIONAL_FEATURES: ReadonlySet<string> = new Set(ALL_FEATURES.filter(f => f.optional).map(f => f.code));

/** Libellés utilisateur des statuts — le back renvoie un enum, pas un texte à afficher. */
export const LICENSE_STATUS_LABEL: Record<LicenseStatus, string> = {
  VALID: 'Valide',
  EXPIRING_SOON: 'Expire bientôt',
  EXPIRING_CRITICAL: 'Expiration imminente',
  GRACE: 'Période de grâce',
  EXPIRED: 'Expirée',
  MISSING: 'Absente',
  INVALID: 'Non valide',
  CLOCK_TAMPERED: 'Horloge incohérente',
  DEMO_QUOTA_REACHED: 'Limite de démonstration atteinte',
  UNKNOWN: 'Indéterminé',
};

export const LICENSE_TYPE_LABEL: Record<LicenseType, string> = {
  DEMO: 'Démonstration',
  TRIAL: 'Essai',
  SUBSCRIPTION: 'Abonnement',
  PARTNER: 'Partenaire',
};

export const LICENSE_AUDIT_LABEL: Record<LicenseAuditEntry['eventType'], string> = {
  ACTIVATION: 'Activation',
  REJECTION: 'Dépôt refusé',
  EXPIRATION: 'Expiration',
  CLOCK_TAMPER: 'Horloge reculée',
  BLOCKED_WRITE: 'Écriture bloquée',
  MAGASIN_NAME_MISMATCH: 'Officine différente',
  TAX_ID_MISMATCH: 'N° contribuable différent',
  FINGERPRINT_MISMATCH: 'Poste différent',
};
