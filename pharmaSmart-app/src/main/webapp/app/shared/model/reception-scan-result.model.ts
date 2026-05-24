export type BarcodeType = 'EAN_8' | 'EAN_13' | 'CIP_7' | 'CIP_13' | 'DATAMATRIX' | 'UNKNOWN';

/** Statut FMD (Falsified Medicines Directive) du scan. */
export type FmdStatus = 'PRESENT' | 'ABSENT' | 'DUPLICATE';

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
  /** Lignes avec provisionalCode=true détectées lors d'un scan inconnu. */
  provisionalLines?: { id: number; libelle: string }[];
}
