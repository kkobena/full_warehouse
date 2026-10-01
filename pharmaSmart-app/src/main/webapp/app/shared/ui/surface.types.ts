/**
 * Teinte d'accent d'une surface : celles des familles d'icônes, pour qu'une section porte la
 * couleur de son icône (`pi-tag` → fuchsia, `pi-wallet` → amber…). Doit rester aligné sur
 * `$pharma-surface-accents` dans `app/shared/scss/_surface.scss`.
 *
 * ⚠ Passer la valeur en liaison — `[accent]="'amber'"` — et non en attribut statique.
 */
export type AppSurfaceAccent =
  | 'emerald'
  | 'blue'
  | 'amber'
  | 'violet'
  | 'rose'
  | 'teal'
  | 'orange'
  | 'indigo'
  | 'cyan'
  | 'fuchsia'
  | 'slate'
  | 'success'
  | 'danger'
  | 'warning'
  | 'info'
  | 'none';

/** `raised` : carte surélevée, pour ce qu'on vient consulter. `muted` : à plat, pour la référence. */
export type AppSurfaceVariant = 'raised' | 'muted';
