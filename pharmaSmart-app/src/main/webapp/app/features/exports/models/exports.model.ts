/** Contrats de l'API des exports (`/api/exports`), miroir des DTO de `service/dto/exports`. */

export type ExportDonnees =
  | 'VENTES'
  | 'LIGNES_VENTE'
  | 'ENCAISSEMENTS'
  | 'ACHATS'
  | 'MOUVEMENTS_STOCK'
  | 'PRODUITS'
  | 'CLIENTS'
  | 'VENTES_JOUR_PRODUIT';
export type RubriqueExport = 'DONNEES' | 'NOMINATIF' | 'BI';
export type FormatExport = 'CSV' | 'XLSX';
export type StatutExport = 'EN_ATTENTE' | 'EN_COURS' | 'TERMINE' | 'ECHEC' | 'EXPIRE';
export type PeriodeRelative =
  | 'HIER'
  | 'SEMAINE_PRECEDENTE'
  | 'MOIS_EN_COURS'
  | 'MOIS_PRECEDENT'
  | 'TRIMESTRE_PRECEDENT'
  | 'ANNEE_EN_COURS'
  | 'ANNEE_PRECEDENTE';
export type Frequence = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface ExportCatalogue {
  code: ExportDonnees;
  rubrique: RubriqueExport;
  libelleRubrique: string;
  libelle: string;
  description: string;
  periodique: boolean;
  colonnes: string[];
}

export interface LienExport {
  rubrique: string;
  libelle: string;
  description: string;
  route: string;
  onglet: string | null;
}

export interface CatalogueExports {
  exports: ExportCatalogue[];
  liens: LienExport[];
}

export interface DemandeExport {
  export: ExportDonnees;
  format: FormatExport;
  du: string | null;
  au: string | null;
}

export interface ExportFichier {
  id: number;
  export: ExportDonnees;
  libelleExport: string;
  format: FormatExport;
  du: string | null;
  au: string | null;
  statut: StatutExport;
  nombreLignes: number | null;
  taille: number | null;
  erreur: string | null;
  demandePar: string;
  demandeLe: string;
  termineLe: string | null;
  expireLe: string | null;
  modele: string | null;
}

export interface ExportModele {
  id: number | null;
  libelle: string;
  export: ExportDonnees;
  format: FormatExport;
  periode: PeriodeRelative | null;
  partage: boolean;
  frequence: Frequence | null;
  /** `HH:mm:ss` */
  heure: string | null;
  jour: number | null;
  prochaineExecution: string | null;
  proprietaire: string | null;
  modifiable: boolean;
}

export const PERIODES_RELATIVES: { libelle: string; valeur: PeriodeRelative }[] = [
  { libelle: 'Hier', valeur: 'HIER' },
  { libelle: 'Semaine précédente', valeur: 'SEMAINE_PRECEDENTE' },
  { libelle: 'Mois en cours', valeur: 'MOIS_EN_COURS' },
  { libelle: 'Mois précédent', valeur: 'MOIS_PRECEDENT' },
  { libelle: 'Trimestre précédent', valeur: 'TRIMESTRE_PRECEDENT' },
  { libelle: 'Année en cours', valeur: 'ANNEE_EN_COURS' },
  { libelle: 'Année précédente', valeur: 'ANNEE_PRECEDENTE' },
];

export const FORMATS: { label: string; value: FormatExport; icon: string }[] = [
  { label: 'Excel (XLSX)', value: 'XLSX', icon: 'pi pi-file-excel' },
  { label: 'CSV', value: 'CSV', icon: 'pi pi-file' },
];
