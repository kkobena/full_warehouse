export interface IBfrSnapshot {
  stockValue?: number;
  creancesTp?: number;
  dettesFournisseurs?: number;
  bfr?: number;
  dio?: number;
  dso?: number;
  dpo?: number;
  ccc?: number;
}

export interface IBfrEvolution {
  labels?: string[];
  creancesEmises?: number[];
  achatsRecus?: number[];
}
