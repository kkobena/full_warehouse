/**
 * Dashboard Scope Enumeration
 */
export enum DashboardScope {
  PRIVATE = 'PRIVATE',
  SHARED = 'SHARED',
  PUBLIC = 'PUBLIC',
}

/**
 * Widget Type Enumeration
 */
export enum WidgetType {
  KPI_CARD = 'KPI_CARD',
  LINE_CHART = 'LINE_CHART',
  BAR_CHART = 'BAR_CHART',
  PIE_CHART = 'PIE_CHART',
  TABLE = 'TABLE',
  TOP_PRODUCTS = 'TOP_PRODUCTS',
  STOCK_ALERTS = 'STOCK_ALERTS',
  RECENT_SALES = 'RECENT_SALES',
  PENDING_INVOICES = 'PENDING_INVOICES',
  PERFORMANCE_GAUGE = 'PERFORMANCE_GAUGE',
}

/**
 * Widget Configuration Interface
 */
export interface IWidgetConfig {
  type: WidgetType;
  title: string;
  dataSource?: string; // API endpoint or data source identifier
  refreshInterval?: number; // Auto-refresh interval in seconds
  config?: any; // Widget-specific configuration
}

/**
 * GridStack Item Interface
 */
export interface IGridStackItem {
  x: number;
  y: number;
  w: number; // width in grid units
  h: number; // height in grid units
  id: string;
  widget: IWidgetConfig;
}

/**
 * Layout Configuration Interface
 */
export interface ILayoutConfig {
  items: IGridStackItem[];
  gridOptions?: {
    column?: number;
    cellHeight?: number;
    margin?: number;
  };
}

/**
 * Dashboard Layout Interface
 */
export interface IDashboardLayout {
  id?: number;
  name?: string;
  description?: string;
  userId?: number;
  userLogin?: string;
  scope?: DashboardScope;
  isDefault?: boolean;
  layoutConfig?: string; // JSON string
  createdAt?: Date;
  updatedAt?: Date;
}

/**
 * Dashboard Layout with parsed config
 */
export interface IDashboardLayoutParsed extends IDashboardLayout {
  config?: ILayoutConfig;
}
