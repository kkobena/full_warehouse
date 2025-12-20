export interface IDashboardAlertCount {
  peremptionCount: number;
  ruptureCount: number;
  entreeCount: number;
  ajustementCount: number;
  prixModifCount: number;
}

export class DashboardAlertCount implements IDashboardAlertCount {
  constructor(
    public peremptionCount: number = 0,
    public ruptureCount: number = 0,
    public entreeCount: number = 0,
    public ajustementCount: number = 0,
    public prixModifCount: number = 0,
  ) {}
}
