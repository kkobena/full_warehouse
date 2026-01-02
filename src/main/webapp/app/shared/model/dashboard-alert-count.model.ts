export interface IDashboardAlertCount {
  peremptionCount: number;
  ruptureCount: number;
  entreeCount: number;
  ajustementCount: number;
  prixModifCount: number;
}

export class DashboardAlertCount implements IDashboardAlertCount {
  constructor(
    public peremptionCount = 0,
    public ruptureCount = 0,
    public entreeCount = 0,
    public ajustementCount = 0,
    public prixModifCount = 0,
  ) {}
}
