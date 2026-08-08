/**
 * Classes de `v_abc_pareto_analysis.classe_pareto` — 5 classes depuis V1.3.3
 * (seuils de CA cumulé 60 / 80 / 95 / 99, plus `D` pour les produits sans vente).
 *
 * La valeur transportée est celle stockée en base : `A_PLUS`, jamais `A+`, qui n'est
 * qu'un libellé d'affichage.
 */
export type ClassePareto = 'A_PLUS' | 'A' | 'B' | 'C' | 'D';

const PARETO_LABELS: Record<string, string> = {
  A_PLUS: 'A+',
  A: 'A',
  B: 'B',
  C: 'C',
  D: 'D',
};

const PARETO_COLORS: Record<string, string> = {
  A_PLUS: '#dc3545',
  A: '#198754',
  B: '#0d6efd',
  C: '#6c757d',
  D: '#adb5bd',
};

export function paretoLabel(classePareto?: string | null): string {
  if (!classePareto) {
    return '';
  }
  return PARETO_LABELS[classePareto] ?? classePareto;
}

/**
 * Pastille de classe Pareto pour les grilles AG Grid — rendu HTML, les deux grilles
 * d'inventaire utilisent le même.
 */
export function renderParetoBadge(classePareto?: string | null): string {
  if (!classePareto) {
    return '';
  }
  const color = PARETO_COLORS[classePareto] ?? '#6c757d';
  return (
    `<span style="display:inline-block;padding:1px 6px;border-radius:10px;font-size:11px;` +
    `font-weight:700;color:#fff;background:${color}">${paretoLabel(classePareto)}</span>`
  );
}
