export type FrequenceTournant = 'QUOTIDIEN' | 'HEBDO' | 'MENSUEL' | 'TRIMESTRIEL';
export type CritereTournant = 'RAYON' | 'FAMILLE' | 'CLASSIFICATION_ABC';

export interface IPlanningInventaireTournant {
  id?: number;
  libelle: string;
  frequence: FrequenceTournant;
  critere: CritereTournant;
  storageId?: number;
  storageLibelle?: string;
  /** Employé affecté — l'inventaire créé lui sera assigné */
  userId?: number;
  userFullName?: string;
  prochaineExecution: string; // LocalDate → ISO string
  actif: boolean;
  critereIndexCourant?: number;
  classeParetoCourante?: string;
  nbExecutions?: number;
  derniereExecution?: string;
}

export interface ITournantDashboard {
  nbPlanningsActifs: number;
  nbInventairesCeMois: number;
  tauxCouverturePct: number;
  prochainesExecutions: IPlanningInventaireTournant[];
  prochaineTournant?: string;
}

export const FREQUENCES: { value: FrequenceTournant; label: string }[] = [
  { value: 'QUOTIDIEN', label: 'Quotidien' },
  { value: 'HEBDO', label: 'Hebdomadaire' },
  { value: 'MENSUEL', label: 'Mensuel' },
  { value: 'TRIMESTRIEL', label: 'Trimestriel' },
];

export const CRITERES: { value: CritereTournant; label: string; icon: string }[] = [
  { value: 'RAYON', label: 'Par rayon', icon: 'pi pi-box' },
  { value: 'FAMILLE', label: 'Par famille de produits', icon: 'pi pi-tags' },
  { value: 'CLASSIFICATION_ABC', label: 'Par classification ABC (Pareto)', icon: 'pi pi-chart-bar' },
];
