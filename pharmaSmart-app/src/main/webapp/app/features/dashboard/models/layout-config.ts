import { DASHBOARD_CONFIG_VERSION, DashboardConfig, DashboardContextSettings, DashboardItem } from './dashboard.model';
import { parseContextSettings } from './period';

export interface ParsedConfig {
  config: DashboardConfig;
  /** Tuiles de l'ancien format (widget sans clé), écartées car elles n'affichaient rien. */
  legacyItemsDropped: number;
}

export function emptyConfig(): DashboardConfig {
  return { version: DASHBOARD_CONFIG_VERSION, items: [] };
}

/** Lit `layout_config`. Tolère l'absence, un JSON invalide et l'ancien format sans `widgetKey`. */
export function parseLayoutConfig(json: string | null | undefined): ParsedConfig {
  if (!json) {
    return { config: emptyConfig(), legacyItemsDropped: 0 };
  }
  let raw: unknown;
  try {
    raw = JSON.parse(json);
  } catch {
    return { config: emptyConfig(), legacyItemsDropped: 0 };
  }
  const rawItems: unknown[] = Array.isArray((raw as { items?: unknown })?.items) ? (raw as { items: unknown[] }).items : [];
  const items = rawItems.map(toItem).filter((item): item is DashboardItem => item !== null);
  const context = parseContextSettings((raw as { context?: unknown })?.context);
  return { config: { version: DASHBOARD_CONFIG_VERSION, items, context }, legacyItemsDropped: rawItems.length - items.length };
}

export function serializeLayoutConfig(items: DashboardItem[], context?: DashboardContextSettings): string {
  const config: DashboardConfig = { version: DASHBOARD_CONFIG_VERSION, items, context };
  return JSON.stringify(config);
}

function toItem(value: unknown): DashboardItem | null {
  const v = value as Partial<DashboardItem> | null;
  if (!v || typeof v.widgetKey !== 'string' || !v.widgetKey) {
    return null;
  }
  return {
    id: typeof v.id === 'string' && v.id ? v.id : newItemId(),
    x: toInt(v.x, 0),
    y: toInt(v.y, 0),
    w: toInt(v.w, 4),
    h: toInt(v.h, 3),
    widgetKey: v.widgetKey,
    title: typeof v.title === 'string' ? v.title : undefined,
    params: v.params && typeof v.params === 'object' ? { ...v.params } : undefined,
  };
}

function toInt(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? Math.max(0, Math.round(value)) : fallback;
}

export function newItemId(): string {
  return `w-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}
