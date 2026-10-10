/** Contrats de l'API du pilotage (`/api/pilotage`), miroir des DTO de `service/dto/pilotage`. */

export type UniteIndicateur = 'MONTANT' | 'NOMBRE' | 'POURCENTAGE' | 'JOURS' | 'RATIO';
export type SensFavorable = 'HAUSSE' | 'BAISSE' | 'NEUTRE';
export type TypeComparaison = 'AUCUNE' | 'PERIODE_PRECEDENTE' | 'MEME_PERIODE_N_1' | 'ANNEE_N_MOINS_K' | 'PERSONNALISEE' | 'OBJECTIF';
export type Granularite = 'JOUR' | 'SEMAINE' | 'MOIS' | 'TRIMESTRE' | 'ANNEE';
export type AxeAnalyse =
  | 'FAMILLE'
  | 'PRODUIT'
  | 'LABORATOIRE'
  | 'FOURNISSEUR'
  | 'FORME'
  | 'GAMME'
  | 'DCI'
  | 'TVA'
  | 'NATURE_VENTE'
  | 'TYPE_PRESCRIPTION'
  | 'VENDEUR'
  | 'REMISE'
  | 'OCTROI_REMISE'
  | 'CAISSIER'
  | 'HEURE'
  | 'JOUR_SEMAINE'
  | 'PERIODE';
export type SourceAnalyse = 'LIGNES' | 'ENTETES';
export type TriAnalyse = 'VALEUR' | 'ECART_HAUSSE' | 'ECART_BAISSE';
export type AffichageAnalyse = 'BARRES' | 'COURBE' | 'TABLEAU' | 'CROISE';

export interface Indicateur {
  code: string;
  libelle: string;
  definition: string;
  unite: UniteIndicateur;
  sensFavorable: SensFavorable;
  droit: string;
}

export interface Periode {
  du: string;
  au: string;
}

export interface ComparaisonPeriodes {
  periode: Periode;
  reference: Periode | null;
  aDate: boolean;
}

export interface PointSerie {
  debut: string;
  fin: string;
  /** « Oct. 2026 », « T4 2026 », « Oct. 2026 (au 10) »… */
  libelle: string;
  valeur: number | null;
  valeurReference: number | null;
  ecart: number | null;
  ecartPct: number | null;
  /** Objectif de la tranche (prorata des jours d'un mois entamé) ; null sans objectif ou sans le droit. */
  objectif: number | null;
}

export interface SerieIndicateur {
  indicateur: Indicateur;
  valeur: number | null;
  valeurReference: number | null;
  ecart: number | null;
  ecartPct: number | null;
  points: PointSerie[];
  objectif: number | null;
  /** Fin du mois en cours, seulement quand la période est ce mois à date. */
  projection: ProjectionObjectif | null;
}

export interface SeriesPilotage {
  comparaison: ComparaisonPeriodes;
  joursOuvres: number;
  joursOuvresReference: number;
  series: SerieIndicateur[];
}

export interface Contribution {
  axe: AxeAnalyse;
  libelleAxe: string;
  cle: string;
  libelle: string;
  valeur: number;
  valeurReference: number;
  ecart: number;
}

export interface EcartsPilotage {
  comparaison: ComparaisonPeriodes;
  ca: number;
  caReference: number;
  effetFrequentation: number | null;
  effetArticles: number | null;
  effetPrix: number | null;
  hausses: Contribution[];
  baisses: Contribution[];
}

export type PeriodePredefinie =
  | 'MOIS_EN_COURS'
  | 'MOIS_PRECEDENT'
  | 'TRIMESTRE_EN_COURS'
  | 'ANNEE_EN_COURS'
  | 'ANNEE_PRECEDENTE'
  | 'DOUZE_MOIS_GLISSANTS'
  | 'PERSONNALISEE';

export interface Axe {
  code: AxeAnalyse;
  libelle: string;
  sources: SourceAnalyse[];
  suivant: AxeAnalyse | null;
  filtrable: boolean;
  ordonne: boolean;
}

export interface CelluleAnalyse {
  valeur: number | null;
  valeurReference: number | null;
  ecart: number | null;
  ecartPct: number | null;
}

export interface ElementAnalyse {
  /** `null` pour la ligne « autres ». */
  cle: string | null;
  libelle: string;
  cellules: CelluleAnalyse[];
  part: number | null;
  contribution: number | null;
}

export interface MembreAnalyse {
  cle: string;
  libelle: string;
}

export interface LigneCroisee {
  cle: string;
  libelle: string;
  cellules: CelluleAnalyse[];
}

export interface CroiseAnalyse {
  colonnes: MembreAnalyse[];
  lignes: LigneCroisee[];
}

export type ModeAnnees = 'MENSUEL' | 'CUMULE' | 'GLISSANT';

export interface AnneeComparee {
  annee: number;
  complete: boolean;
  /** 12 mois ; après le mois en cours, la valeur est vide. Référence : même mois de l'année précédente. */
  mois: CelluleAnalyse[];
  trimestres: CelluleAnalyse[];
  total: CelluleAnalyse;
}

export interface SyntheseAnnee {
  annee: number;
  complete: boolean;
  caTtc: number | null;
  margeBrute: number | null;
  tauxMarge: number | null;
  nbVentes: number | null;
  panierMoyen: number | null;
  remises: number | null;
  croissanceCa: number | null;
}

export interface ComparaisonAnnees {
  indicateur: Indicateur;
  mode: ModeAnnees;
  parJourOuvre: boolean;
  jusquAu: string;
  annees: AnneeComparee[];
  saisonnalite: number[];
  croissanceAnnuelleMoyenne: number | null;
  croissanceDepuis: number | null;
  croissanceJusqua: number | null;
  moisMaximum: { annee: number; mois: number; valeur: number } | null;
  synthese: SyntheseAnnee[];
  parFamille: CroiseAnalyse | null;
  parNatureVente: CroiseAnalyse | null;
}

/** Réglage de l'onglet « Comparer les années », porté par l'URL. */
export interface ReglageAnnees {
  indicateur: string;
  annees: number;
  mode: ModeAnnees;
  parJourOuvre: boolean;
  filtre: EtapeDescente | null;
}

export interface AnalysePilotage {
  comparaison: ComparaisonPeriodes;
  source: SourceAnalyse;
  indicateurs: Indicateur[];
  indicateursIgnores: Indicateur[];
  axe: Axe;
  axe2: Axe | null;
  total: CelluleAnalyse[];
  elements: ElementAnalyse[];
  autres: ElementAnalyse | null;
  nombreElements: number;
  croise: CroiseAnalyse | null;
}

export interface AxeExplique {
  axe: Axe;
  part: number;
  nombreElements: number;
  principales: Contribution[];
}

export interface ExplicationEcart {
  indicateur: Indicateur;
  valeur: number | null;
  valeurReference: number | null;
  ecart: number | null;
  axes: AxeExplique[];
}

/** Une étape de la descente : l'élément choisi sur un axe. */
export interface EtapeDescente {
  axe: AxeAnalyse;
  cle: string;
  libelle: string;
}

/** Réglage de l'onglet Analyser, porté par l'URL et enregistrable en vue. */
export interface ReglageAnalyse {
  indicateurs: string[];
  axe: AxeAnalyse;
  axe2: AxeAnalyse | null;
  top: number;
  tri: TriAnalyse;
  affichage: AffichageAnalyse;
  chemin: EtapeDescente[];
}

export interface VuePilotage {
  id: number | null;
  libelle: string;
  indicateurs: string[];
  axe: AxeAnalyse;
  axe2: AxeAnalyse | null;
  top: number;
  tri: TriAnalyse;
  affichage: AffichageAnalyse;
  livree: boolean;
  partagee: boolean;
  modifiable: boolean;
}

export interface LigneMarge {
  cle: string;
  libelle: string;
  caHt: number;
  coutHt: number;
  marge: number;
  taux: number | null;
  tauxReference: number | null;
  /** En points. */
  ecartTaux: number | null;
  effetVolume: number | null;
  effetTaux: number | null;
}

export interface VenteMarge {
  id: number;
  saleDate: string;
  numero: string;
  vendeur: string | null;
  caHt: number;
  coutHt: number;
  marge: number;
}

export interface MargeRentabilite {
  comparaison: ComparaisonPeriodes;
  axe: Axe;
  tauxMarge: number | null;
  tauxMargeReference: number | null;
  /** Points de taux dus au mix, puis aux taux de chaque élément ; leur somme est la variation du taux. */
  effetMix: number | null;
  effetTaux: number | null;
  lignes: LigneMarge[];
  seuilFaibleMarge: number;
  faiblesMarges: LigneMarge[];
  ventesAMargeNegative: VenteMarge[];
}

export interface LigneRemise {
  cle: string;
  libelle: string;
  nbVentes: CelluleAnalyse;
  caTtc: CelluleAnalyse;
  remises: CelluleAnalyse;
  tauxRemise: CelluleAnalyse;
  tauxMarge: CelluleAnalyse | null;
  alerte: boolean;
}

export interface VenteRemisee {
  id: number;
  saleDate: string;
  numero: string;
  vendeur: string | null;
  montant: number;
  remise: number;
  taux: number;
  autorisePar: string | null;
}

export interface RemisesRentabilite {
  comparaison: ComparaisonPeriodes;
  remises: CelluleAnalyse;
  tauxRemise: CelluleAnalyse;
  partVentesRemisees: CelluleAnalyse;
  remiseMoyenne: CelluleAnalyse;
  poidsRemisesMarge: CelluleAnalyse;
  octrois: LigneRemise[];
  tranches: LigneRemise[];
  /** `null` sans le droit « Clients & équipe ». */
  vendeurs: LigneRemise[] | null;
  multipleAlerte: number;
  plusFortesRemises: VenteRemisee[];
}

export interface DemarqueRentabilite {
  comparaison: ComparaisonPeriodes;
  valeur: CelluleAnalyse;
  partDuCa: CelluleAnalyse;
  parMotif: { libelle: string; quantite: number; valeur: CelluleAnalyse }[];
  produits: { cle: string; libelle: string; quantite: number; valeur: number }[];
}

export interface LigneAchats {
  cle: string;
  libelle: string;
  montant: CelluleAnalyse;
  part: number | null;
  nbBons: number | null;
  delaiMoyen: number | null;
  conformite: number | null;
}

export interface AchatsPilotage {
  comparaison: ComparaisonPeriodes;
  achatsTtc: CelluleAnalyse;
  achatsHt: CelluleAnalyse;
  nbBons: CelluleAnalyse;
  delaiMoyen: CelluleAnalyse;
  conformite: CelluleAnalyse;
  fournisseurs: LigneAchats[];
  familles: LigneAchats[];
}

export interface AchatsVentesPilotage {
  comparaison: ComparaisonPeriodes;
  points: { debut: string; fin: string; libelle: string; achatsHt: number; coutVentesHt: number }[];
  familles: { cle: string; libelle: string; achatsHt: number; coutVentesHt: number; ecart: number; ratio: number | null }[];
}

export interface StockPilotage {
  mois: string;
  valeur: CelluleAnalyse;
  rotation: number | null;
  couvertureJours: number | null;
  nbDormants: number;
  valeurDormante: number;
  seuilDormant: number;
  courbe: { mois: string; valeur: number | null; valeurN1: number | null }[];
  familles: {
    cle: string;
    libelle: string;
    valeur: number;
    part: number | null;
    coutVentes12Mois: number;
    rotation: number | null;
    couvertureJours: number | null;
    valeurDormante: number;
  }[];
  dormants: { cle: string; libelle: string; quantite: number; valeur: number; derniereVente: string | null }[];
}

export interface Comptage {
  cle: string;
  libelle: string;
  nombre: number;
  quantite: number;
  montant: number;
}

export interface RupturesPilotage {
  comparaison: ComparaisonPeriodes;
  tauxRupture: CelluleAnalyse;
  ventesManquees: CelluleAnalyse;
  tauxVentesManquees: CelluleAnalyse;
  valeurPerimee: CelluleAnalyse;
  valeurAPerimerTroisMois: number;
  rupturesParFournisseur: Comptage[];
  rupturesParProduit: Comptage[];
  ventesManqueesParProduit: Comptage[];
  peremptionsAVenir: { annee: number; mois: number; quantite: number; valeur: number }[];
}

export interface TrancheMontant {
  libelle: string;
  montant: number;
}

export interface EncaissementsTresorerie {
  comparaison: ComparaisonPeriodes;
  total: CelluleAnalyse;
  modes: { cle: string; libelle: string; montant: CelluleAnalyse; part: number | null }[];
  /** Libellés des tranches, dans l'ordre des séries. */
  tranches: string[];
  series: { libelle: string; valeurs: number[] }[];
  /** `null` sans le droit « Clients & équipe ». */
  caissiers: { cle: string; libelle: string; montant: number; transactions: number; sessions: number }[] | null;
}

export interface OrganismeTresorerie {
  cle: string;
  libelle: string;
  facture: number;
  regle: number;
  tauxRecouvrement: number | null;
  encours: number;
  partEncours: number | null;
  dso: number | null;
  enRetard: number;
  delaiRetenu: number;
  origineDelai: 'OBSERVE' | 'GROUPE' | 'DEFAUT' | null;
}

export interface TiersPayantTresorerie {
  comparaison: ComparaisonPeriodes;
  facture: CelluleAnalyse;
  regle: CelluleAnalyse;
  encours: number;
  dso: number | null;
  organismes: OrganismeTresorerie[];
  vieillissement: TrancheMontant[];
  encaissementsAttendus: TrancheMontant[];
  concentrationTrois: number | null;
  concentrationCinq: number | null;
  seuilHistorique: number;
}

export interface DifferesTresorerie {
  encours: number;
  vieillissement: TrancheMontant[];
  clients: Comptage[];
  avoirsEmis: CelluleAnalyse;
  avoirsRembourses: CelluleAnalyse;
}

export interface ClientsPilotage {
  comparaison: ComparaisonPeriodes;
  actifs: CelluleAnalyse;
  nouveaux: CelluleAnalyse;
  revenus: number | null;
  perdus: number | null;
  partCaIdentifie: CelluleAnalyse;
  caMoyenParClient: CelluleAnalyse;
  enBaisse: Contribution[];
}

export interface LigneVendeur {
  cle: string;
  libelle: string;
  nbVentes: CelluleAnalyse;
  caTtc: CelluleAnalyse;
  panierMoyen: CelluleAnalyse;
  articlesParVente: CelluleAnalyse;
  tauxMarge: CelluleAnalyse;
  tauxRemise: CelluleAnalyse;
  annulations: CelluleAnalyse;
  avoirs: CelluleAnalyse;
  partOrdonnance: CelluleAnalyse;
}

export interface EquipePilotage {
  comparaison: ComparaisonPeriodes;
  vendeurs: LigneVendeur[];
  equipe: LigneVendeur;
}

export interface AlertePilotage {
  code: string;
  gravite: 'HAUTE' | 'MOYENNE';
  titre: string;
  detail: string;
  /** Onglet de la page où creuser. */
  onglet: string;
}

export interface LigneObjectifs {
  indicateur: Indicateur;
  /** 12 valeurs, null pour un mois sans objectif. */
  mois: (number | null)[];
  modifiePar: string | null;
  modifieLe: string | null;
}

export interface GrilleObjectifs {
  annee: number;
  /** Mois déjà écoulés (0 à 12) : leurs objectifs ne se modifient plus. */
  moisClos: number;
  lignes: LigneObjectifs[];
}

export interface SaisieObjectifs {
  annee: number;
  indicateur: string;
  mois: (number | null)[];
}

export interface SuiviMois {
  mois: number;
  objectif: number | null;
  realise: number | null;
  ecart: number | null;
  atteinte: number | null;
  /** Objectif atteint, ou plafond respecté ; null sans objectif ou sans réalisé. */
  tenu: boolean | null;
  enCours: boolean;
}

export interface ProjectionObjectif {
  realiseADate: number | null;
  projection: number | null;
  objectif: number | null;
  atteinteProjetee: number | null;
  tenu: boolean | null;
  methode: string;
}

export interface SuiviIndicateur {
  indicateur: Indicateur;
  mois: SuiviMois[];
  /** Mois clos depuis janvier ; null pour un taux. */
  cumul: SuiviMois | null;
  projection: ProjectionObjectif | null;
}

export interface SuiviObjectifs {
  annee: number;
  jusquAu: string;
  indicateurs: SuiviIndicateur[];
}

/** Ce que l'URL de la page porte, et que chaque onglet transmet à l'API (sauf `predefinie`, état de l'écran). */
export interface RequetePilotage {
  predefinie: PeriodePredefinie;
  du: string;
  au: string;
  comparaison: TypeComparaison;
  anneesEnArriere: number;
  aDate: boolean;
  granularite: Granularite;
}
