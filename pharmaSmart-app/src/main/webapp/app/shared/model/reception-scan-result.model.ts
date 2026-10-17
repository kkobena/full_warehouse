export type BarcodeType = 'EAN_8' | 'EAN_13' | 'CIP_7' | 'CIP_13' | 'GTIN_14' | 'DATAMATRIX' | 'UNKNOWN';

/** Statut FMD (Falsified Medicines Directive) du scan. */
export type FmdStatus = 'PRESENT' | 'ABSENT' | 'DUPLICATE';

/** Nature d'un code alternatif mémorisé pour un produit. */
export type CodeAliasType = 'GTIN_14' | 'EAN_13' | 'EAN_8' | 'CIP_13' | 'CIP_7' | 'CUSTOM';

/** Origine d'une liaison code ↔ produit. */
export type CodeAliasSource = 'MANUAL' | 'AUTO_LEARNED' | 'IMPORT';

/** Source de la correspondance trouvée pour un code scanné. */
export type MatchedBy = 'CODE_CIP' | 'CODE_EAN' | 'CODE_EAN_LABO' | 'ALIAS';

/**
 * Données lues sur la boîte quand le scan n'a pas retrouvé le produit. Elles pré-remplissent
 * l'écran de résolution manuelle au lieu d'être perdues.
 */
export interface IDataMatrixHint {
  /** Code produit retenu par le parser, sous sa forme normalisée. */
  scannedCode: string;
  /** Numéro de lot (AI 10). */
  numLot?: string | null;
  /** Date de péremption (AI 17), au format ISO. */
  expiryDate?: string | null;
  /** Numéro de série (AI 21). */
  serialNumber?: string | null;
  /** Quantité scannée (AI 37), 1 par défaut. */
  qty: number;
  /**
   * Lot et péremption déduits par motif d'un code sans AI GS1 : à présenter comme proposés et à
   * faire confirmer. Ils ne créent jamais de lot côté serveur.
   */
  proposition?: boolean;
}

/** Code alternatif d'un produit. */
export interface IProduitCodeAlias {
  id: number;
  produitId: number;
  code: string;
  codeType: CodeAliasType;
  source: CodeAliasSource;
  createdAt?: string | null;
  createdBy?: string | null;
}

/** Produit retrouvé à partir d'un code scanné. */
export interface IProductResolveResult {
  produitId: number;
  libelle: string;
  matchedCode: string;
  matchedBy: MatchedBy;
}

export interface IReceptionScanResult {
  found: boolean;
  orderLineId: number | null;
  produitLibelle: string | null;
  produitCip: string | null;
  lotAutoCreated: boolean;
  lotNumero: string | null;
  /** LotDTO complet : auto-créé si lotAutoCreated=true, ou données DataMatrix pour pré-remplissage si false. */
  lot: { numLot?: string; expiryDate?: string } | null;
  warningMessage: string | null;
  barcodeType: BarcodeType;
  /** Numéro de série FMD (AI 21). Null si scan 1D ou absent du DataMatrix. */
  serialNumber: string | null;
  /** Statut FMD : PRESENT (OK), ABSENT (scan 1D / pas d'AI 21), DUPLICATE (alerte contrefaçon). */
  fmdStatus: FmdStatus;
  /** Quantité scannée extraite de l'AI 37 du DataMatrix GS1 (défaut: 1). */
  scannedQty?: number;
  // ── AX-23 — Pont scan → CIP provisoire ──────────────────────────────────
  /** Code brut scanné — renseigné quand found=false pour l'associer à une ligne provisoire. */
  scannedCode?: string | null;
  /** Lot, péremption et quantité lus sur la boîte quand le produit n'a pas été retrouvé. */
  dataMatrixHint?: IDataMatrixHint | null;
  /** Lignes avec provisionalCode=true détectées lors d'un scan inconnu. */
  provisionalLines?: { id: number; libelle: string }[];
}
