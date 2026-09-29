import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { IDashboardLayout } from 'app/shared/model/dashboard-layout.model';
import { AllowedWidget, DashboardItem, GridPosition, WidgetDefinition } from '../../models/dashboard.model';
import { newItemId } from '../../models/layout-config';
import { widgetAvailability } from '../../models/widget-availability';
import { findWidgetDefinition, WIDGET_DEFINITIONS } from '../../widgets/widget-registry';

interface DashboardState {
  /** Layout ouvert ; null tant que le dashboard n'a jamais été enregistré. */
  layout: IDashboardLayout | null;
  items: DashboardItem[];
  editMode: boolean;
  /** Modifié depuis l'ouverture ou le dernier enregistrement. */
  dirty: boolean;
  allowed: AllowedWidget[];
  loading: boolean;
  saving: boolean;
}

const initialState: DashboardState = {
  layout: null,
  items: [],
  editMode: false,
  dirty: false,
  allowed: [],
  loading: false,
  saving: false,
};

/** État de l'éditeur de dashboard. Fourni par l'écran, pas à la racine : chaque ouverture repart de zéro. */
export const DashboardStore = signalStore(
  withState(initialState),

  withComputed(store => ({
    isSaved: computed(() => store.layout()?.id != null),
    /** Widgets proposés à l'ajout : connus du front et autorisés au rôle. */
    catalogue: computed(() =>
      WIDGET_DEFINITIONS.filter(def => store.allowed().some(a => a.key === def.key)).map(def => ({
        definition: def,
        licensed: store.allowed().find(a => a.key === def.key)?.licensed ?? false,
        alreadyAdded: store.items().some(i => i.widgetKey === def.key),
      })),
    ),
    availability: computed(() => {
      const allowed = store.allowed();
      return new Map(store.items().map(i => [i.id, widgetAvailability(i.widgetKey, allowed)]));
    }),
  })),

  withMethods(store => ({
    setAllowed(allowed: AllowedWidget[]): void {
      patchState(store, { allowed });
    },

    setLoading(loading: boolean): void {
      patchState(store, { loading });
    },

    setSaving(saving: boolean): void {
      patchState(store, { saving });
    },

    /** Ouvre un layout (ou un dashboard vierge si null). Un dashboard vierge s'ouvre en édition. */
    open(layout: IDashboardLayout | null, items: DashboardItem[]): void {
      patchState(store, { layout, items, dirty: false, editMode: items.length === 0 });
    },

    setEditMode(editMode: boolean): void {
      patchState(store, { editMode });
    },

    addWidget(definition: WidgetDefinition): DashboardItem {
      const bottom = store.items().reduce((max, i) => Math.max(max, i.y + i.h), 0);
      const item: DashboardItem = {
        id: newItemId(),
        x: 0,
        y: bottom,
        w: definition.defaultSize.w,
        h: definition.defaultSize.h,
        widgetKey: definition.key,
      };
      patchState(store, { items: [...store.items(), item], dirty: true });
      return item;
    },

    removeItem(id: string): void {
      patchState(store, { items: store.items().filter(i => i.id !== id), dirty: true });
    },

    /** Réglages d'une tuile : titre et paramètres remplacent les précédents (un paramètre vidé disparaît). */
    updateSettings(id: string, title: string | undefined, params: Record<string, string>): void {
      patchState(store, {
        items: store.items().map(i => (i.id === id ? { ...i, title: title?.trim() || undefined, params } : i)),
        dirty: true,
      });
    },

    /** Copie de la tuile, posée juste en dessous ; la grille la tasse s'il y a de la place. */
    duplicateItem(id: string): DashboardItem | null {
      const source = store.items().find(i => i.id === id);
      if (!source) {
        return null;
      }
      const copy: DashboardItem = { ...source, id: newItemId(), y: source.y + source.h, params: source.params ? { ...source.params } : undefined };
      patchState(store, { items: [...store.items(), copy], dirty: true });
      return copy;
    },

    updateParams(id: string, params: Record<string, string>): void {
      patchState(store, {
        items: store.items().map(i => (i.id === id ? { ...i, params: { ...i.params, ...params } } : i)),
        dirty: true,
      });
    },

    /**
     * Recopie les positions calculées par GridStack, sans marquer le dashboard modifié : la grille
     * tasse aussi les tuiles d'elle-même, à l'ouverture. Un geste de l'utilisateur passe par
     * {@link markDirty}.
     */
    applyPositions(positions: GridPosition[]): void {
      const byId = new Map(positions.map(p => [p.id, p]));
      let changed = false;
      const items = store.items().map(item => {
        const p = byId.get(item.id);
        if (!p || (p.x === item.x && p.y === item.y && p.w === item.w && p.h === item.h)) {
          return item;
        }
        changed = true;
        return { ...item, x: p.x, y: p.y, w: p.w, h: p.h };
      });
      if (changed) {
        patchState(store, { items });
      }
    },

    markDirty(): void {
      patchState(store, { dirty: true });
    },

    markSaved(layout: IDashboardLayout): void {
      patchState(store, { layout, dirty: false });
    },
  })),
);

export type DashboardStoreType = InstanceType<typeof DashboardStore>;

export function widgetLabel(item: DashboardItem): string {
  return item.title || findWidgetDefinition(item.widgetKey)?.label || item.widgetKey;
}
