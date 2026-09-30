import { AppBadgeSeverity } from 'app/shared/ui';
import { ITraitementChronique, SuiviTraitement } from '../customer-fiche.model';

// Indexé par `string` : les gabarits le lisent depuis des lignes de tableau non typées.
export const SUIVI_TRAITEMENT: Readonly<Record<string, { libelle: string; severity: AppBadgeSeverity }>> = {
  ARRETE: { libelle: 'Arrêté', severity: 'secondary' },
  SANS_DELIVRANCE: { libelle: 'Jamais délivré', severity: 'secondary' },
  A_JOUR: { libelle: 'À jour', severity: 'success' },
  A_RENOUVELER: { libelle: 'À renouveler', severity: 'warn' },
  EN_RETARD: { libelle: 'En retard', severity: 'danger' },
  RUPTURE: { libelle: 'Rupture de traitement', severity: 'danger' },
};

/** Traitements qui appellent une action au comptoir. */
export const A_RELANCER: readonly SuiviTraitement[] = ['A_RENOUVELER', 'EN_RETARD', 'RUPTURE'];

/** « Metformine 850 mg » ou, pour un patient non substituable, le produit imposé. */
export function libelleTraitement(t: ITraitementChronique): string {
  const base = t.produitLibelle ?? t.dciLibelle ?? '';
  return t.dosage && !t.produitLibelle ? `${base} ${t.dosage}` : base;
}

/** « dans 3 j », « aujourd'hui », « 12 j de retard ». */
export function echeanceTraitement(t: ITraitementChronique): string {
  const j = t.joursRestants;
  if (j === null || j === undefined) {
    return '—';
  }
  if (j === 0) {
    return "aujourd'hui";
  }
  return j > 0 ? `dans ${j} j` : `${-j} j de retard`;
}
