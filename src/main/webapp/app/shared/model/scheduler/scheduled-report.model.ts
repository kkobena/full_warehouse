export interface IScheduledReport {
  id?: number;
  reportName?: string;
  reportType?: ScheduledReportType;
  frequency?: ScheduledReportFrequency;
  executionTime?: string; // HH:mm format
  dayOfWeek?: number; // 1-7
  dayOfMonth?: number; // 1-31
  active?: boolean;
  emailRecipients?: string; // Comma-separated
  includePdf?: boolean;
  includeExcel?: boolean;
  lastExecution?: string;
  nextExecution?: string;
  createdAt?: string;
  updatedAt?: string;
  filterParams?: string; // JSON string
}

export enum ScheduledReportType {
  DASHBOARD_CA = 'DASHBOARD_CA',
  SALES_SUMMARY = 'SALES_SUMMARY',
  TOP_PRODUCTS = 'TOP_PRODUCTS',
  STOCK_ALERTS = 'STOCK_ALERTS',
  STOCK_VALUATION = 'STOCK_VALUATION',
  STOCK_ROTATION = 'STOCK_ROTATION',
  CASH_REGISTER = 'CASH_REGISTER',
  TIERS_PAYANT_CREANCES = 'TIERS_PAYANT_CREANCES',
  CUSTOMER_SEGMENTATION = 'CUSTOMER_SEGMENTATION',
  SUPPLIER_PERFORMANCE = 'SUPPLIER_PERFORMANCE',
  PROFITABILITY_ANALYSIS = 'PROFITABILITY_ANALYSIS',
  ABC_PARETO = 'ABC_PARETO',
  COMPARATIVE_ANALYSIS = 'COMPARATIVE_ANALYSIS',
}

export enum ScheduledReportFrequency {
  DAILY = 'DAILY',
  WEEKLY = 'WEEKLY',
  MONTHLY = 'MONTHLY',
  CUSTOM = 'CUSTOM',
}

export const REPORT_TYPE_LABELS: Record<ScheduledReportType, string> = {
  [ScheduledReportType.DASHBOARD_CA]: 'Dashboard CA',
  [ScheduledReportType.SALES_SUMMARY]: 'Synthèse des Ventes',
  [ScheduledReportType.TOP_PRODUCTS]: 'Top Produits',
  [ScheduledReportType.STOCK_ALERTS]: 'Alertes Stock',
  [ScheduledReportType.STOCK_VALUATION]: 'Valorisation Stock',
  [ScheduledReportType.STOCK_ROTATION]: 'Rotation Stock',
  [ScheduledReportType.CASH_REGISTER]: 'Rapport de Caisse',
  [ScheduledReportType.TIERS_PAYANT_CREANCES]: 'Créances Tiers-Payants',
  [ScheduledReportType.CUSTOMER_SEGMENTATION]: 'Segmentation Clients',
  [ScheduledReportType.SUPPLIER_PERFORMANCE]: 'Performance Fournisseurs',
  [ScheduledReportType.PROFITABILITY_ANALYSIS]: 'Analyse de Rentabilité',
  [ScheduledReportType.ABC_PARETO]: 'Analyse ABC/Pareto',
  [ScheduledReportType.COMPARATIVE_ANALYSIS]: 'Tableaux Comparatifs',
};

export const FREQUENCY_LABELS: Record<ScheduledReportFrequency, string> = {
  [ScheduledReportFrequency.DAILY]: 'Quotidien',
  [ScheduledReportFrequency.WEEKLY]: 'Hebdomadaire',
  [ScheduledReportFrequency.MONTHLY]: 'Mensuel',
  [ScheduledReportFrequency.CUSTOM]: 'Personnalisé',
};
