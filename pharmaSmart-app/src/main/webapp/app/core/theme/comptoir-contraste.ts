/**
 * Mesure des couleurs d'accent du comptoir (docs/PLAN-STYLES-COMPTOIR.md, §4).
 *
 * Source unique : `$comptoir-accents` dans `content/scss/_comptoir-styles.scss` et les dérivés
 * `--comptoir-accent-dark` / `--comptoir-accent-tint` dans `sales-home.component.scss`. Rien n'est
 * recopié ici : le garde-fou (comptoir-contraste.spec.ts) lit ces fichiers.
 */

export type Rgb = [number, number, number];

export const MODES_COMPTOIR = ['comptant', 'assurance', 'carnet'] as const;
export type PaletteComptoir = 'vente' | 'document';
export type ModeComptoir = (typeof MODES_COMPTOIR)[number];

export function lireRgb(hex: string): Rgb {
  const h = hex.replace('#', '');
  return [0, 2, 4].map(i => parseInt(h.slice(i, i + 2), 16)) as Rgb;
}

export function versHex(rgb: Rgb): string {
  return '#' + rgb.map(c => Math.max(0, Math.min(255, Math.round(c))).toString(16).padStart(2, '0')).join('');
}

/** `color-mix(in srgb, a pa%, b)` */
export function melanger(a: string, pa: number, b: string): string {
  const A = lireRgb(a);
  const B = lireRgb(b);
  return versHex(A.map((c, i) => (c * pa) / 100 + (B[i] * (100 - pa)) / 100) as Rgb);
}

export function luminance(hex: string): number {
  const lineaire = (c: number) => (c / 255 <= 0.03928 ? c / 255 / 12.92 : Math.pow((c / 255 + 0.055) / 1.055, 2.4));
  const [r, g, b] = lireRgb(hex);
  return 0.2126 * lineaire(r) + 0.7152 * lineaire(g) + 0.0722 * lineaire(b);
}

/** Rapport de contraste WCAG 2.x. */
export function contraste(a: string, b: string): number {
  const [clair, fonce] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (clair + 0.05) / (fonce + 0.05);
}

export type Deficience = 'protanopie' | 'deuteranopie' | 'tritanopie';

// Machado, Oliveira, Fernandes (2009), sévérité 1,0, appliquée en RGB linéaire.
const MATRICES: Record<Deficience, number[][]> = {
  protanopie: [[0.152286, 1.052583, -0.204868], [0.114503, 0.786281, 0.099216], [-0.003882, -0.048116, 1.051998]],
  deuteranopie: [[0.367322, 0.860646, -0.227968], [0.280085, 0.672501, 0.047413], [-0.01182, 0.04294, 0.968881]],
  tritanopie: [[1.255528, -0.076749, -0.178779], [-0.078411, 0.930809, 0.147602], [0.004733, 0.691367, 0.3039]],
};

const versLineaire = (c: number) => (c / 255 <= 0.04045 ? c / 255 / 12.92 : Math.pow((c / 255 + 0.055) / 1.055, 2.4));
const versGamma = (c: number) => 255 * (c <= 0.0031308 ? c * 12.92 : 1.055 * Math.pow(c, 1 / 2.4) - 0.055);

export function simuler(hex: string, deficience: Deficience): string {
  const L = lireRgb(hex).map(versLineaire);
  const M = MATRICES[deficience];
  return versHex(M.map(ligne => versGamma(ligne.reduce((s, m, j) => s + m * L[j], 0))) as Rgb);
}

function versLab(hex: string): [number, number, number] {
  const [r, g, b] = lireRgb(hex).map(versLineaire);
  const X = 0.4124 * r + 0.3576 * g + 0.1805 * b;
  const Y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  const Z = 0.0193 * r + 0.1192 * g + 0.9505 * b;
  const f = (t: number) => (t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16 / 116);
  const [fx, fy, fz] = [f(X / 0.95047), f(Y), f(Z / 1.08883)];
  return [116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)];
}

/** Écart de couleur CIE76 : au-delà d'environ 20, deux teintes se distinguent d'un coup d'œil. */
export function ecartCouleur(a: string, b: string): number {
  const [A, B] = [versLab(a), versLab(b)];
  return Math.hypot(A[0] - B[0], A[1] - B[1], A[2] - B[2]);
}

/** Lit `$nom: #rrggbb;` dans un fichier SCSS. */
export function lireVariablesScss(scss: string): Record<string, string> {
  const variables: Record<string, string> = {};
  for (const m of scss.matchAll(/^\$([\w-]+):\s*(#[0-9a-fA-F]{6})\s*;/gm)) {
    variables[m[1]] = m[2].toLowerCase();
  }
  return variables;
}

/** Lit une map d'accents (`$comptoir-accents`, `$comptoir-accents-document`) ; `$variable` est résolue dans `variables`. */
export function lireAccents(scss: string, variables: Record<string, string>, nomMap = 'comptoir-accents'): Record<ModeComptoir, string> {
  const bloc = new RegExp('\\$' + nomMap + ':\\s*\\(([\\s\\S]*?)\\);').exec(scss)?.[1] ?? '';
  const accents: Partial<Record<ModeComptoir, string>> = {};
  for (const m of bloc.matchAll(/^\s*(\w+):\s*(#[0-9a-fA-F]{6}|\$[\w-]+)\s*,/gm)) {
    const valeur = m[2].startsWith('$') ? variables[m[2].slice(1)] : m[2].toLowerCase();
    if (!valeur) {
      throw new Error(`Accent « ${m[1]} » : variable ${m[2]} introuvable`);
    }
    accents[m[1] as ModeComptoir] = valeur;
  }
  return accents as Record<ModeComptoir, string>;
}

/** Accent de l'écran dépôt : `$comptoir-accent-depot: #rrggbb;` dans `_comptoir-styles.scss`. */
export function lireAccentDepot(scss: string): string {
  const accent = lireVariablesScss(scss)['comptoir-accent-depot'];
  if (!accent) {
    throw new Error('$comptoir-accent-depot introuvable dans _comptoir-styles.scss');
  }
  return accent;
}

/** Couleurs de marque des pastilles de règlement : `$comptoir-couleurs-modes` dans payment-mode.component.scss. */
export function lireCouleursModes(scss: string): Record<string, string> {
  const bloc = /\$comptoir-couleurs-modes:\s*\(([\s\S]*?)\);/.exec(scss)?.[1] ?? '';
  const couleurs: Record<string, string> = {};
  for (const m of bloc.matchAll(/^\s*(\w+):\s*(#[0-9a-fA-F]{6})\s*,/gm)) {
    couleurs[m[1]] = m[2].toLowerCase();
  }
  return couleurs;
}

/** Pourcentages de mélange des dérivés, lus dans sales-home.component.scss. */
export function lireDerives(scss: string): { fonce: number; teinte: number; survol: number } {
  const lire = (nom: string): number => {
    const m = new RegExp(nom + ':\\s*color-mix\\(in srgb,\\s*var\\(--comptoir-accent\\)\\s*(\\d+)%').exec(scss);
    if (!m) {
      throw new Error(`Dérivé ${nom} introuvable dans sales-home.component.scss`);
    }
    return Number(m[1]);
  };
  return {
    fonce: lire('--comptoir-accent-dark'),
    teinte: lire('--comptoir-accent-tint'),
    survol: lire('--pharma-row-selected-hover-bg'),
  };
}

export interface MesureContraste {
  mode: string;
  paire: string;
  ratio: number;
  seuil: number;
}

const BLANC = '#ffffff';
/** `--pharma-text` des trois thèmes (menthe, ardoise, clair). */
const TEXTE = '#334155';

export function mesurerContrastes(
  palettes: Record<PaletteComptoir, Record<ModeComptoir, string>>,
  derives: { fonce: number; teinte: number; survol: number },
  accentDepot?: string,
): MesureContraste[] {
  const mesures: MesureContraste[] = [];
  for (const [palette, accents] of Object.entries(palettes) as [PaletteComptoir, Record<ModeComptoir, string>][]) {
    for (const type of MODES_COMPTOIR) {
      mesurerPaire(mesures, `${palette}/${type}`, accents[type], derives);
    }
  }
  if (accentDepot) {
    mesurerPaire(mesures, 'depot', accentDepot, derives);
  }
  return mesures;
}

/**
 * Pastilles des modes de règlement : texte = la couleur de marque assombrie de moitié, sur la même couleur
 * à 14 % vers le blanc (voir payment-mode.component.scss).
 */
export function mesurerPastilles(couleurs: Record<string, string>): MesureContraste[] {
  return Object.entries(couleurs).map(([mode, couleur]) => ({
    mode: `pastille/${mode}`,
    paire: 'texte de la pastille sur sa teinte',
    ratio: contraste(melanger(couleur, 50, '#000000'), melanger(couleur, 14, BLANC)),
    seuil: 4.5,
  }));
}

function mesurerPaire(
  mesures: MesureContraste[],
  mode: string,
  accent: string,
  derives: { fonce: number; teinte: number; survol: number },
): void {
  {
    const fonce = melanger(accent, derives.fonce, '#000000');
    const teinte = melanger(accent, derives.teinte, BLANC);
    const survol = melanger(accent, derives.survol, BLANC);
    const ajouter = (paire: string, a: string, b: string, seuil: number) => mesures.push({ mode, paire, ratio: contraste(a, b), seuil });
    ajouter('blanc sur accent (texte sur aplat)', BLANC, accent, 4.5);
    ajouter('blanc sur accent foncé (fin de dégradé)', BLANC, fonce, 4.5);
    ajouter('accent foncé sur teinte (titres, libellés)', fonce, teinte, 4.5);
    ajouter('texte courant sur teinte', TEXTE, teinte, 4.5);
    ajouter('accent foncé sur teinte de survol (ligne sélectionnée)', fonce, survol, 4.5);
    ajouter('accent sur teinte (icône, non-texte)', accent, teinte, 3);
    ajouter('accent sur blanc (filet, marqueur)', accent, BLANC, 3);
  }
}

export interface MesureDistinction {
  vision: 'normale' | Deficience;
  plusProche: { a: string; b: string; ecart: number };
}

const VISIONS: ('normale' | Deficience)[] = ['normale', 'protanopie', 'deuteranopie', 'tritanopie'];

/** Paire de couleurs la plus proche, pour chaque vision, parmi `couleurs` (nom → hex). */
function plusProche(couleurs: Record<string, string>, paires: [string, string][]): MesureDistinction[] {
  return VISIONS.map(vision => {
    const vue = (nom: string) => (vision === 'normale' ? couleurs[nom] : simuler(couleurs[nom], vision));
    let proche = { a: paires[0][0], b: paires[0][1], ecart: Infinity };
    for (const [a, b] of paires) {
      const ecart = ecartCouleur(vue(a), vue(b));
      if (ecart < proche.ecart) {
        proche = { a, b, ecart };
      }
    }
    return { vision, plusProche: proche };
  });
}

function toutesLesPaires(noms: string[]): [string, string][] {
  return noms.flatMap((a, i) => noms.slice(i + 1).map(b => [a, b] as [string, string]));
}

/** Distinction entre les trois types d'une même palette. */
export function mesurerDistinction(accents: Record<ModeComptoir, string>): MesureDistinction[] {
  return plusProche({ ...accents }, toutesLesPaires([...MODES_COMPTOIR]));
}

/** Distinction entre l'accent du dépôt et chacun des trois types de la vente. */
export function mesurerDistinctionDepot(accents: Record<ModeComptoir, string>, accentDepot: string): MesureDistinction[] {
  const couleurs: Record<string, string> = { ...accents, depot: accentDepot };
  return plusProche(
    couleurs,
    MODES_COMPTOIR.map(type => ['depot', type] as [string, string]),
  );
}

/** Distinction entre un type de la palette « document » et un type de la palette « vente ». */
export function mesurerDistinctionEntrePalettes(palettes: Record<PaletteComptoir, Record<ModeComptoir, string>>): MesureDistinction[] {
  const couleurs: Record<string, string> = {};
  for (const type of MODES_COMPTOIR) {
    couleurs[`vente/${type}`] = palettes.vente[type];
    couleurs[`document/${type}`] = palettes.document[type];
  }
  const paires = MODES_COMPTOIR.flatMap(d => MODES_COMPTOIR.map(v => [`document/${d}`, `vente/${v}`] as [string, string]));
  return plusProche(couleurs, paires);
}
