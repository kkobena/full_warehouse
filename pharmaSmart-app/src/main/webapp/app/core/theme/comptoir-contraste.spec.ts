import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import {
  contraste,
  ecartCouleur,
  lireAccentDepot,
  lireAccents,
  lireCouleursModes,
  lireDerives,
  lireVariablesScss,
  mesurerContrastes,
  mesurerDistinction,
  mesurerDistinctionDepot,
  mesurerDistinctionEntrePalettes,
  melanger,
  mesurerPastilles,
  simuler,
} from './comptoir-contraste';

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

  const variables = lireVariablesScss(lire('_pharma-bootstrap-palette.scss'));
  const scss = lire('_comptoir-styles.scss');
  const accents = lireAccents(scss, variables);
  const accentsDocument = lireAccents(scss, variables, 'comptoir-accents-document');
  const palettes = { vente: accents, document: accentsDocument };
  const derives = lireDerives(lireApp('features/sales/feature/sales-home/sales-home.component.scss'));
  const accentDepot = lireAccentDepot(scss);
  const couleursModes = lireCouleursModes(lireApp('features/sales/ui/payment-mode/payment-mode.component.scss'));
  const contrastes = [...mesurerContrastes(palettes, derives, accentDepot), ...mesurerPastilles(couleursModes)];
  const distinctions = mesurerDistinction(accents);
  const distinctionsDocument = mesurerDistinction(accentsDocument);
  const entrePalettes = mesurerDistinctionEntrePalettes(palettes);
  const distinctionsDepot = mesurerDistinctionDepot(accents, accentDepot);

  if (process.env.CONTRASTE_RAPPORT) {
    // eslint-disable-next-line no-console
    console.table(contrastes.map(c => ({ mode: c.mode, paire: c.paire, ratio: c.ratio.toFixed(2), seuil: c.seuil })));
    // eslint-disable-next-line no-console
    const tableau = (palette: string, mesures: typeof distinctions) =>
      mesures.map(d => ({ palette, vision: d.vision, plusProche: `${d.plusProche.a}/${d.plusProche.b}`, ecart: d.plusProche.ecart.toFixed(0) }));
    // eslint-disable-next-line no-console
    console.table([...tableau('vente', distinctions), ...tableau('document', distinctionsDocument), ...tableau('entre palettes', entrePalettes)]);
  }

  it('lit les trois types de vente, dans la palette de la vente et dans celle des documents', () => {
    expect(Object.keys(accents).sort()).toEqual(['assurance', 'carnet', 'comptant']);
    expect(Object.keys(accentsDocument).sort()).toEqual(['assurance', 'carnet', 'comptant']);
  });

  it('lit l’accent du dépôt et les huit couleurs de pastille', () => {
    expect(accentDepot).toMatch(/^#[0-9a-f]{6}$/);
    expect(Object.keys(couleursModes).sort()).toEqual(['cash', 'cb', 'ch', 'moov', 'mtn', 'om', 'virement', 'wave']);
  });

  it('chaque paire texte / fond tient son seuil WCAG AA (4,5:1 texte, 3:1 non-texte)', () => {
    const echecs = contrastes.filter(c => c.ratio < c.seuil).map(c => `${c.mode} — ${c.paire} : ${c.ratio.toFixed(2)} < ${c.seuil}`);

    expect(echecs).toEqual([]);
  });

  it('deux types restent distincts (ΔE ≥ 18) dans chaque palette, en vision normale, protanopie et deutéranopie', () => {
    const trop = [...distinctions, ...distinctionsDocument]
      .filter(d => d.vision !== 'tritanopie')
      .filter(d => d.plusProche.ecart < 18)
      .map(d => `${d.vision} : ${d.plusProche.a}/${d.plusProche.b} à ${d.plusProche.ecart.toFixed(0)}`);

    // Tritanopie (≈ 0,01 % de la population) exclue : comptant et assurance y tombent sous le seuil ;
    // l'icône et le libellé du bandeau portent alors seuls la différence (WCAG 1.4.1).
    expect(trop).toEqual([]);
  });

  it('l’accent du dépôt se distingue de chaque type de la vente (ΔE ≥ 18, hors tritanopie)', () => {
    const trop = distinctionsDepot
      .filter(d => d.vision !== 'tritanopie')
      .filter(d => d.plusProche.ecart < 18)
      .map(d => `${d.vision} : ${d.plusProche.a}/${d.plusProche.b} à ${d.plusProche.ecart.toFixed(0)}`);

    expect(trop).toEqual([]);
  });

  it('la palette des documents se distingue de celle de la vente (ΔE ≥ 30, vision normale)', () => {
    // Sous protanopie et deutéranopie, le brun du carnet et l'olive du carnet des documents se rapprochent
    // (ΔE ≈ 9) : les deux palettes ne sont jamais à l'écran en même temps, et le libellé du bandeau tranche.
    const normale = entrePalettes.find(d => d.vision === 'normale')!;

    expect(normale.plusProche.ecart).toBeGreaterThanOrEqual(30);
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
