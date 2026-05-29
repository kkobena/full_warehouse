import type { BarcodeType } from './reception-scan-result.model';

export interface IDataMatrixInfo {
  gtin?: string | null;
  cip13?: string | null;
  ean13?: string | null;
  batchNumber?: string | null;
  /** ISO date (YYYY-MM-DD) — LocalDate sérialisé par Jackson. */
  expiryDate?: string | null;
  /** ISO date (YYYY-MM-DD). */
  manufacturingDate?: string | null;
  serialNumber?: string | null;
  /** Quantité extraite de l'AI 37 (défaut: 1). */
  scannedQty: number;
  /** Type de code détecté — renseigné par le backend. */
  barcodeType?: BarcodeType;
}

/** Retourne le meilleur code produit disponible (CIP13 > EAN13 > GTIN). */
export function getProductCode(info: IDataMatrixInfo): string | null {
  return info.cip13 ?? info.ean13 ?? info.gtin ?? null;
}
