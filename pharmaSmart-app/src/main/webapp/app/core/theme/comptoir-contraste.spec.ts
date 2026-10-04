import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import {
  contraste,
  ecartCouleur,
  lireCouleursModes,
  lireDerives,
  mesurerContrastes,
  melanger,
  mesurerPastilles,
  simuler,
} from './comptoir-contraste';
import { lireAccentsChrome } from './theme-contraste';

/**
 * Garde-fou des couleurs d'accent du comptoir (docs/PLAN-STYLES-COMPTOIR.md, §4) : lit les fichiers SCSS
 * — source unique — et échoue si une paire passe sous son seuil WCAG ou si deux modes cessent de se
 * distinguer sous protanopie et deutéranopie.
 *
 * `CONTRASTE_RAPPORT=1 npm run contraste:comptoir` imprime le tableau complet.
 */
describe('Couleurs d\u2019accent du comptoir', () => {
  const lire = (chemin: string) => readFileSync(join(__dirname, '../../../content/scss', chemin), 'utf-8');
  const lireApp = (chemin: string) => readFileSync(join(__dirname, '../..', chemin), 'utf-8');

  const scss = lire('_comptoir-styles.scss');
  const themesScss = lire('_pharma-chrome-themes.scss');
  // Un seul accent pour tous les types de vente, la prévente, le proforma et le dépôt : celui du thème du poste.
  const accentActuel = /--pharma-chrome-comptoir-accent:\s*(#[0-9a-fA-F]{6})/.exec(themesScss)?.[1].toLowerCase() as string;
  const accentsThemes: Record<string, string> = { actuel: accentActuel, ...lireAccentsChrome(themesScss) };
  const derives = lireDerives(lireApp('features/sales/feature/sales-home/sales-home.component.scss'));
  const couleursModes = lireCouleursModes(lireApp('features/sales/ui/payment-mode/payment-mode.component.scss'));
  const contrastes = [
    ...Object.entries(accentsThemes).flatMap(([theme, accent]) =>
      mesurerContrastes(
        { vente: { comptant: accent, assurance: accent, carnet: accent }, document: { comptant: accent, assurance: accent, carnet: accent } },
        derives,
        accent,
      )
        .filter(c => c.mode === 'vente/comptant')
        .map(c => ({ ...c, mode: theme })),
    ),
    ...mesurerPastilles(couleursModes),
  ];

  if (process.env.CONTRASTE_RAPPORT) {
    // eslint-disable-next-line no-console
    console.table(contrastes.map(c => ({ mode: c.mode, paire: c.paire, ratio: c.ratio.toFixed(2), seuil: c.seuil })));
  }

  it('lit l’accent du thème « Actuel » et celui de chaque thème dérivé', () => {
    expect(accentActuel).toMatch(/^#[0-9a-f]{6}$/);
    expect(Object.keys(accentsThemes).sort()).toEqual(['actuel', 'assurance', 'comptant', 'prevente-carnet', 'prevente-comptant']);
  });

  it('lit les huit couleurs de pastille', () => {
    expect(Object.keys(couleursModes).sort()).toEqual(['cash', 'cb', 'ch', 'moov', 'mtn', 'om', 'virement', 'wave']);
  });

  it('le comptoir n’a plus d’accent propre à un type de vente : il suit le thème de l’application', () => {
    expect(scss).toContain('--comptoir-accent: var(--pharma-chrome-comptoir-accent)');
    expect(scss).not.toMatch(/comptoir-accents|comptoir-accent-depot|comptoir-doc-accent/);
  });

  it('chaque paire texte / fond tient son seuil WCAG AA (4,5:1 texte, 3:1 non-texte)', () => {
    const echecs = contrastes.filter(c => c.ratio < c.seuil).map(c => `${c.mode} — ${c.paire} : ${c.ratio.toFixed(2)} < ${c.seuil}`);

    expect(echecs).toEqual([]);
  });

  it('l’infobulle (ardoise foncé teinté de l’accent du thème) porte du texte blanc à 4,5:1 dans chaque thème', () => {
    const themes = lire('_pharma-themes.scss');
    const accentsThemes = [...themes.matchAll(/'(\w+)':\s*\([^)]*?accent:\s*(#[0-9a-fA-F]{6})/g)].map(m => [m[1], m[2]]);
    const formule = /--bs-tooltip-bg:\s*color-mix\(in srgb,\s*var\(--pharma-accent\)\s*(\d+)%,\s*(#[0-9a-fA-F]{6})\)/.exec(themes);
    const [pourcentage, base] = [Number(formule?.[1]), formule?.[2] as string];

    expect(accentsThemes.map(([nom]) => nom).sort()).toEqual(['ardoise', 'clair', 'menthe']);
    const echecs = accentsThemes
      .map(([nom, accent]) => [nom, contraste('#ffffff', melanger(accent, pourcentage, base))] as const)
      .filter(([, ratio]) => ratio < 4.5)
      .map(([nom, ratio]) => `${nom} : ${ratio.toFixed(2)}`);

    expect(echecs).toEqual([]);
  });

  describe('outils de mesure', () => {
    it('rapport de contraste : noir sur blanc vaut 21', () => {
      expect(contraste('#000000', '#ffffff')).toBeCloseTo(21, 5);
    });

    it('la simulation de daltonisme ne change pas un gris', () => {
      expect(simuler('#808080', 'deuteranopie')).toBe('#808080');
    });

    it('l’écart de couleur est nul entre deux couleurs identiques', () => {
      expect(ecartCouleur('#047857', '#047857')).toBe(0);
    });
  });
});
