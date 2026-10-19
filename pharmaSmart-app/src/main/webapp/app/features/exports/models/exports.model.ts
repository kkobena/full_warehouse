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
/** Éditions que le menu Exports bâtit sur place, en appelant le service de leur écran. */
export type RapportExport =
  | 'EXPORT_COMPTABLE'
  | 'PILOTAGE_TABLEAU_DE_BORD'
  | 'PILOTAGE_ANALYSER'
  | 'PILOTAGE_ANNEES'
  | 'BALANCE_CAISSE'
  | 'TABLEAU_PHARMACIEN'
  | 'RAPPORT_TVA'
  | 'DECLARATION_TVA'
  | 'RECAPITULATIF_CAISSE'
  | 'RAPPORT_ACTIVITE'
  | 'TABLEAU_DE_BORD_CA'
  | 'MEILLEURES_VENTES'
  | 'COMPARATIF_VENTES'
  | 'RENTABILITE_PRODUITS'
  | 'SEGMENTATION_CLIENTS'
  | 'ALERTES_STOCK'
  | 'ROTATION_STOCK'
  | 'VALORISATION_STOCK'
  | 'ABC_PARETO'
  | 'COMPTES_FOURNISSEURS'
  | 'PERFORMANCE_FOURNISSEURS'
  | 'CREANCES_TIERS_PAYANTS'
  | 'RECAPITULATIF_FACTURES'
  | 'RAPPROCHEMENT_TIERS_PAYANTS'
  | 'DIFFERES_IMPAYES'
  | 'MOUVEMENTS_CAISSE_ETAT'
  | 'PRODUITS_A_DETRUIRE';

/** Code d'une entrée du catalogue : export de données ou édition. */
export type CodeExport = ExportDonnees | RapportExport;
export type FormatExport = 'CSV' | 'XLSX' | 'PDF';
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

/** Filtres partagés avec les écrans des données exportées ; chaque export n'annonce que les siens. */
export type FiltreExport =
  | 'FAMILLE'
  | 'LABORATOIRE'
  | 'FOURNISSEUR'
  | 'FORME'
  | 'GAMME'
  | 'MAGASIN'
  | 'VENDEUR'
  | 'CAISSIER'
  | 'UTILISATEUR'
  | 'MODE_PAIEMENT'
  | 'NATURE_VENTE'
  | 'TYPE_PRESCRIPTION'
  | 'TYPE_TRANSACTION'
  | 'TYPE_MOUVEMENT'
  | 'SEXE'
  | 'STATUT_CLIENT'
  | 'SECTIONS_COMPTABLES'
  | 'GRANULARITE'
  | 'COMPARAISON'
  | 'INDICATEURS'
  | 'INDICATEUR_COMPARE'
  | 'AXE_ANALYSE'
  | 'NOMBRE_ELEMENTS'
  | 'TRI_ANALYSE'
  | 'MODE_ANNEES'
  | 'NOMBRE_ANNEES'
  | 'MODE_CA'
  | 'DECOUPAGE_TABLEAU'
  | 'GROUPEMENT_TVA'
  | 'FAMILLE_UNIQUE'
  | 'DECOUPAGE_COMPARATIF'
  | 'TYPE_FACTURE';

/** Valeurs retenues par filtre ; une clé absente ou une liste vide ne filtre rien. */
export type FiltresRetenus = Partial<Record<FiltreExport, string[]>>;

export interface OptionFiltre {
  valeur: string;
  libelle: string;
}

export interface FiltreCatalogue {
  code: FiltreExport;
  libelle: string;
  /** Plusieurs valeurs à la fois ; sinon l'écran n'en propose qu'une. */
  multiple: boolean;
  options: OptionFiltre[];
}

export interface ExportCatalogue {
  code: CodeExport;
  libelleRubrique: string;
  libelle: string;
  description: string;
  periodique: boolean;
  /** Enregistrable dans un modèle programmé ; faux pour une édition, bâtie avec les droits de qui la demande. */
  programmable: boolean;
  formats: FormatExport[];
  /** Vide pour une édition : son service la met en forme lui-même. */
  colonnes: string[];
  filtres: FiltreCatalogue[];
}

export interface CatalogueExports {
  exports: ExportCatalogue[];
}

export interface DemandeExport {
  export: CodeExport;
  format: FormatExport;
  du: string | null;
  au: string | null;
  filtres: FiltresRetenus;
}

export interface ExportFichier {
  id: number;
  export: CodeExport;
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
  /** Les filtres du fichier, déjà rédigés par le serveur (« Famille : 3 valeurs »). */
  filtres: string[];
}

export interface ExportModele {
  id: number | null;
  libelle: string;
  export: CodeExport;
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
  filtres: FiltresRetenus;
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

// PDF en tête : c'est le format proposé d'office, celui qui se lit et s'imprime sans rien ouvrir
// d'autre. Excel et CSV restent à un clic, pour qui retravaille les chiffres.
const FORMATS: { label: string; value: FormatExport; icon: string }[] = [
  { label: 'PDF', value: 'PDF', icon: 'pi pi-file-pdf' },
  { label: 'Excel (XLSX)', value: 'XLSX', icon: 'pi pi-file-excel' },
  { label: 'CSV', value: 'CSV', icon: 'pi pi-file' },
];

/** Format retenu tant que rien n'est choisi. Une entrée qui ne le produit pas prend son premier format offert. */
export const FORMAT_PAR_DEFAUT: FormatExport = 'PDF';

/** Les formats qu'une entrée annonce, dans l'ordre du sélecteur ; une édition n'en propose pas toujours trois. */
export function listerFormats(offerts: readonly FormatExport[]): { label: string; value: FormatExport; icon: string }[] {
  return FORMATS.filter(format => offerts.includes(format.value));
}

/** Le format sous lequel `BlobDownloadService` nomme et ouvre le fichier reçu. */
export const TELECHARGEMENT: Record<FormatExport, 'pdf' | 'excel' | 'csv'> = {
  PDF: 'pdf',
  XLSX: 'excel',
  CSV: 'csv',
};

/** Les seules valeurs utiles : un filtre sans valeur retenue ne part pas dans la demande. */
export function retenirFiltres(filtres: FiltresRetenus): FiltresRetenus {
  const retenus: FiltresRetenus = {};
  for (const [code, valeurs] of Object.entries(filtres) as [FiltreExport, string[] | undefined][]) {
    if (valeurs?.length) {
      retenus[code] = valeurs;
    }
  }
  return retenus;
}
