import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { CHROMES } from './chrome-theme';
import { ecartCouleur } from './comptoir-contraste';
import {
  deriverChrome,
  empilementTenu,
  lireAccentsChrome,
  lireAccentsFonds,
  lireBoutons,
  lireChromeActuel,
  lireTeintesIcones,
  lireParametres,
  mesurerChrome,
  mesurerInfobulle,
} from './theme-contraste';

/**
 * Garde-fou des couleurs de l'application (docs/PLAN-THEMES-APPLICATION.md, §6) : lit les fichiers SCSS — source
 * unique — et échoue si un thème passe sous son seuil WCAG, si l'ordre des trois bandes (barre de titre < rail <
 * navbar) n'est plus tenu, ou si la liste du sélecteur diverge de la table SCSS.
 *
 * `CONTRASTE_RAPPORT=1 npx ng test --test-path-patterns=theme-contraste` imprime le tableau complet.
 */
describe('Couleurs de l’application (chrome)', () => {
  /** Écarts connus, tolérés tels quels le temps d'une décision (paire → plancher). Vide : aucun. */
  const ECARTS_CONNUS: Record<string, number> = {};
  const lire = (chemin: string) => readFileSync(join(__dirname, '../../../content/scss', chemin), 'utf-8');

  const scss = lire('_pharma-chrome-themes.scss');
  const scssFonds = lire('_pharma-themes.scss');
  const teintes = lireTeintesIcones(lire('icon-colors-global.scss'));
  const boutons = lireBoutons(scss);
  const parametres = lireParametres(scss);
  const actuel = lireChromeActuel(scss);
  const accents = lireAccentsChrome(scss);
  const themes = {
    actuel,
    ...Object.fromEntries(Object.entries(accents).map(([nom, accent]) => [nom, deriverChrome(accent, parametres, teintes, boutons[nom])])),
  };
  const mesures = Object.entries(themes).flatMap(([nom, couleurs]) => mesurerChrome(nom, couleurs));
  const infobulles = Object.entries(lireAccentsFonds(scssFonds)).map(([fond, accent]) => mesurerInfobulle(fond, accent, scssFonds));

  if (process.env.CONTRASTE_RAPPORT) {
    const ligne = (m: { theme: string; paire: string; ratio: number; seuil: number }) =>
      `${m.theme.padEnd(18)} ${m.paire.padEnd(48)} ${m.ratio.toFixed(2).padStart(6)}  (≥ ${m.seuil})`;
    console.log(['', ...mesures.map(ligne), ...infobulles.map(ligne)].join('\n'));
  }

  it('le sélecteur propose exactement les thèmes définis en SCSS', () => {
    expect(CHROMES.map(c => c.name).sort()).toEqual(['actuel', ...Object.keys(accents)].sort());
  });

  it.each(mesures.map(m => [`${m.theme} — ${m.paire}`, m] as const))('%s', (nom, m) => {
    expect(m.ratio).toBeGreaterThanOrEqual(ECARTS_CONNUS[nom] ?? m.seuil);
  });

  it.each(Object.entries(themes))('%s : la barre de titre ancre la pile (barre de titre < rail < navbar)', (_nom, couleurs) => {
    expect(empilementTenu(couleurs)).toBe(true);
  });

  it.each(infobulles.map(m => [`${m.theme} — ${m.paire}`, m] as const))('%s', (_nom, m) => {
    expect(m.ratio).toBeGreaterThanOrEqual(m.seuil);
  });

  // « Un bouton info sur un thème bleu ne doit pas être pareil sur un autre thème » : chaque sévérité a sa couleur propre par thème.
  it.each(['success', 'info', 'warning', 'danger', 'help', 'contrast'])('bouton %s : couleur distincte d’un thème à l’autre (ΔE ≥ 12)', sev => {
    const noms = Object.keys(boutons);
    const trop_proches: string[] = [];
    for (let i = 0; i < noms.length; i++) {
      for (let j = i + 1; j < noms.length; j++) {
        const ecart = ecartCouleur(boutons[noms[i]][sev], boutons[noms[j]][sev]);
        if (ecart < 12) {
          trop_proches.push(`${noms[i]} / ${noms[j]} : ΔE ${ecart.toFixed(1)}`);
        }
      }
    }
    expect(trop_proches).toEqual([]);
  });

  it('la pastille du sélecteur est l’accent du thème', () => {
    for (const [nom, accent] of Object.entries(accents)) {
      expect(CHROMES.find(c => c.name === nom)?.swatch).toBe(accent);
    }
  });
});
