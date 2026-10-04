/**
 * Mesure des couleurs de l'application (docs/PLAN-THEMES-APPLICATION.md, §3 et §6).
 *
 * Source unique : `content/scss/_pharma-chrome-themes.scss` (thème `actuel` en valeurs littérales, quatre thèmes
 * dérivés d'un accent et de pourcentages de mélange) et `_pharma-themes.scss` (accent de chaque fond de page, pour
 * l'infobulle). Rien n'est recopié ici : le garde-fou (theme-contraste.spec.ts) lit ces fichiers.
 */
import { contraste, ecartCouleur, luminance, melanger } from './comptoir-contraste';

const BLANC = '#ffffff';
const NOIR = '#000000';
const HEX = '#[0-9a-fA-F]{6}';
/** `--pharma-text` et `--pharma-text-muted` des fonds de page (ardoise, le plus clair des trois pour le second). */
const TEXTE_COURANT = '#334155';
const TEXTE_ATTENUE = '#5b6778';

export interface CouleursChrome {
  /** Fond de la barre horizontale (début du dégradé). */
  navbar: string;
  /** Fond du rail latéral. */
  rail: string;
  /** Haut de la barre de titre Tauri (le bas est le rail). */
  barreDeTitre: string;
  /** Fins de dégradé des deux barres. */
  navbarFin: string;
  railFin: string;
  /** Survol d'une entrée de la barre horizontale. */
  navbarSurvol: string;
  /** Texte des barres (blanc sur les barres sombres d'`actuel`, encre de l'accent sur les barres claires). */
  texteBarre: string;
  /** Texte atténué et panneau déroulant : thèmes dérivés seulement (`actuel` n'est pas mesuré). */
  texteBarreAttenue?: string;
  panneau?: string;
  /** Sélecteur à pastilles : thèmes dérivés seulement. */
  pastille?: { piste: string; repos: string; survolFond: string; survolTexte: string; actifDebut: string; actifFin: string; actifTexte: string };
  /** Bandeau d'indicateurs : thèmes dérivés seulement. */
  kpi?: [string, string];
  /** Accent applicatif (phase 6) : bouton primaire, bouton contour, liens. Thèmes dérivés seulement. */
  accentApp?: { bouton: string; boutonSurvol: string; boutonActif: string; encre: string; lienSurvol: string };
  /** Liens de `app-nav-sidebar` : texte et fond au survol, texte et liseré à l'état actif. */
  /** Ligne sélectionnée d'un tableau : texte sur son fond, au repos et au survol, et liseré. */
  ligneSelectionnee?: { fond: string; survol: string; texte: string; liseré: string };
  liensVerticaux?: { survolFond: string; survolTexte: string; actifTexte: string; actifBordure: string; actifFond: string };
  /** Barre d'onglets légère (`app-subtab-bar`) : texte actif sur blanc, pastilles de comptage. */
  sousOnglets?: { actif: string; survol: string; pastilles: Record<string, { bg: string; texte: string }> };
  /** Boutons sémantiques nuancés par thème : fond, texte, survol, actif, contour. Thèmes dérivés seulement. */
  boutons?: Record<string, { bg: string; texte: string; survol: string; actif: string; contour: string }>;
  /** Icônes colorées de navigation (`--nav-hue-*`), assombries : thèmes dérivés seulement. */
  icones?: Record<string, string>;
  /** Fonds sur lesquels elles se posent : barre horizontale, rail, panneau déroulant, survol. */
  fondsIcones?: Record<string, string>;
  /** Dégradés d'en-tête : modale, tableau, toolbar, et la couleur de leur texte. */
  entetes: Record<'modale' | 'tableau' | 'toolbar', [string, string]>;
  enteteTexte: string;
  /** Fond de page imposé par le thème ; absent pour `actuel` (il garde les fonds de page de `_pharma-themes.scss`). */
  fondPage?: string;
  /** Onglet actif (texte) et teinte de survol ; absents = onglets d'avant le chantier, non mesurés. */
  onglet?: { texte: string; survol: string };
  /** Entrée active et repère d'accent, tels qu'affichés sur une barre. */
  active: string;
  accent: string;
}

export interface MesureChrome {
  theme: string;
  paire: string;
  ratio: number;
  seuil: number;
}

export interface ParametresDerives {
  barreTeinte: number;
  navbarTeinte: number;
  survolTeinte: number;
  panneauTeinte: number;
  titreTeinte: number;
  texteAttenueConserve: number;
  encreBarre: number;
  boutonSurvol: number;
  boutonActif: number;
  semHover: number;
  semActive: number;
  semHoverClair: number;
  semActiveClair: number;
  semContour: number;
  semContourClair: number;
  boutonEncre: string;
  encreOnglet: number;
  survolLiensVerticaux: number;
  actifLiensVerticaux: number;
  ligneSelectionnee: number;
  ligneSelectionneeSurvol: number;
  pastillePiste: number;
  pastilleActif: number;
  pastilleActifFin: number;
  pastilleRepos: string;
  teinteIcone: number;
  kpiTeinte: number;
  kpiTeinteFin: number;
  teinteEntete: number;
  teinteEnteteFin: number;
  encreEntete: number;
  degradeConserve: number;
  pageTeinte: number;
  pageBase: string;
}

function lireCouleur(scss: string, nom: string): string {
  const m = new RegExp('\\$' + nom + ':\\s*(' + HEX + ')\\s*;').exec(scss);
  if (!m) {
    throw new Error(`$${nom} introuvable dans _pharma-chrome-themes.scss`);
  }
  return m[1].toLowerCase();
}

function lireNombre(scss: string, nom: string): number {
  const m = new RegExp('\\$' + nom + ':\\s*(\\d+)%\\s*;').exec(scss);
  if (!m) {
    throw new Error(`$${nom} introuvable dans _pharma-chrome-themes.scss`);
  }
  return Number(m[1]);
}

export function lireParametres(scss: string): ParametresDerives {
  return {
    barreTeinte: lireNombre(scss, 'chrome-bar-tint'),
    navbarTeinte: lireNombre(scss, 'chrome-navbar-tint'),
    survolTeinte: lireNombre(scss, 'chrome-hover-tint'),
    panneauTeinte: lireNombre(scss, 'chrome-panel-tint'),
    titreTeinte: lireNombre(scss, 'chrome-title-tint'),
    texteAttenueConserve: lireNombre(scss, 'chrome-fg-muted-keep'),
    encreBarre: lireNombre(scss, 'chrome-bar-ink'),
    boutonSurvol: lireNombre(scss, 'chrome-btn-hover'),
    boutonActif: lireNombre(scss, 'chrome-btn-active'),
    semHover: lireNombre(scss, 'chrome-sem-hover'),
    semActive: lireNombre(scss, 'chrome-sem-active'),
    semHoverClair: lireNombre(scss, 'chrome-sem-hover-clair'),
    semActiveClair: lireNombre(scss, 'chrome-sem-active-clair'),
    semContour: lireNombre(scss, 'chrome-sem-outline'),
    semContourClair: lireNombre(scss, 'chrome-sem-outline-clair'),
    boutonEncre: lireCouleur(scss, 'chrome-btn-ink'),
    encreOnglet: lireNombre(scss, 'chrome-tab-ink'),
    survolLiensVerticaux: lireNombre(scss, 'chrome-navv-hover'),
    actifLiensVerticaux: lireNombre(scss, 'chrome-navv-active'),
    ligneSelectionnee: lireNombre(scss, 'chrome-row-selected'),
    ligneSelectionneeSurvol: lireNombre(scss, 'chrome-row-selected-hover'),
    pastillePiste: lireNombre(scss, 'chrome-pill-track'),
    pastilleActif: lireNombre(scss, 'chrome-pill-active'),
    pastilleActifFin: lireNombre(scss, 'chrome-pill-active-end'),
    pastilleRepos: lireCouleur(scss, 'chrome-pill-idle'),
    teinteIcone: lireNombre(scss, 'nav-hue-keep'),
    kpiTeinte: lireNombre(scss, 'chrome-kpi-tint'),
    kpiTeinteFin: lireNombre(scss, 'chrome-kpi-tint-end'),
    teinteEntete: lireNombre(scss, 'chrome-head-tint'),
    teinteEnteteFin: lireNombre(scss, 'chrome-head-tint-end'),
    encreEntete: lireNombre(scss, 'chrome-head-ink'),
    degradeConserve: lireNombre(scss, 'chrome-gradient-keep'),
    pageTeinte: lireNombre(scss, 'chrome-page-tint'),
    pageBase: lireCouleur(scss, 'chrome-page-base'),
  };
}

/** Accents des thèmes dérivés : la map `$pharma-chrome-themes`. */
export function lireAccentsChrome(scss: string): Record<string, string> {
  const bloc = /\$pharma-chrome-themes:\s*\(([\s\S]*?)\);/.exec(scss)?.[1] ?? '';
  const accents: Record<string, string> = {};
  for (const m of bloc.matchAll(new RegExp("'([\\w-]+)':\\s*(" + HEX + ')', 'g'))) {
    accents[m[1]] = m[2].toLowerCase();
  }
  if (Object.keys(accents).length === 0) {
    throw new Error('$pharma-chrome-themes vide ou introuvable');
  }
  return accents;
}

/** Le bloc `:root { … }` d'`actuel` : le premier du fichier, avant les thèmes dérivés. */
function blocActuel(scss: string): string {
  const debut = scss.indexOf(':root {');
  return scss.slice(debut, scss.indexOf('\n}', debut));
}

export function lireChromeActuel(scss: string): CouleursChrome {
  const bloc = blocActuel(scss);
  const hex = (jeton: string): string => {
    const m = new RegExp('--pharma-chrome-' + jeton + ':\\s*(' + HEX + ')\\s*;').exec(bloc);
    if (!m) {
      throw new Error(`--pharma-chrome-${jeton} introuvable (valeur littérale attendue) dans le thème actuel`);
    }
    return m[1].toLowerCase();
  };
  const degrade = (jeton: string): string[] => {
    const m = new RegExp('--pharma-chrome-' + jeton + ':\\s*linear-gradient\\(([^;]*)\\);').exec(bloc);
    return [...(m?.[1] ?? '').matchAll(new RegExp(HEX, 'g'))].map(c => c[0].toLowerCase());
  };
  const [navbar, , navbarFin] = degrade('gradient-h');
  const [rail, , railFin] = degrade('gradient-v');
  if (!navbar || !navbarFin || !rail || !railFin) {
    throw new Error('Dégradés de barre du thème actuel illisibles');
  }
  return {
    navbar,
    rail,
    barreDeTitre: hex('titlebar-a'),
    navbarSurvol: hex('navbar-hover'),
    texteBarre: BLANC,
    navbarFin,
    railFin,
    entetes: {
      modale: [hex('head-a'), hex('head-b')],
      tableau: [hex('table-a'), hex('table-b')],
      toolbar: [hex('toolbar-a'), hex('toolbar-b')],
    },
    enteteTexte: hex('head-fg'),
    ligneSelectionnee: {
      // « Actuel » : les valeurs d'origine, posées dans `_pharma-tokens.scss`.
      fond: '#cfe6ff',
      survol: '#b9daff',
      texte: '#0a2f5c',
      liseré: '#0d6efd',
    },
    liensVerticaux: {
      survolFond: hex('navv-hover-bg'),
      survolTexte: hex('navv-hover-fg'),
      actifTexte: hex('navv-active-fg'),
      actifBordure: hex('navv-active-border'),
      actifFond: hex('navv-active-bg'),
    },
    sousOnglets: {
      actif: hex('tab-dark'),
      survol: hex('tab-tint'),
      pastilles: {
        primary: { bg: hex('badge-primary'), texte: BLANC },
        success: { bg: hex('badge-success'), texte: BLANC },
        warn: { bg: hex('badge-warn'), texte: hex('badge-warn-fg') },
        danger: { bg: hex('badge-danger'), texte: BLANC },
        info: { bg: hex('badge-info'), texte: BLANC },
      },
    },
    active: hex('active'),
    accent: hex('accent'),
  };
}

/** Rejoue en TypeScript le calcul `color-mix` de la table SCSS pour un accent donné. */
export function deriverChrome(
  accent: string,
  p: ParametresDerives,
  teintes: Record<string, string> = {},
  palette: Record<string, string> = {},
): CouleursChrome {
  const rail = melanger(accent, p.barreTeinte, BLANC);
  const navbar = melanger(accent, p.navbarTeinte, BLANC);
  const debut = melanger(accent, p.teinteEntete, BLANC);
  const fin = melanger(accent, p.teinteEnteteFin, BLANC);
  const encre = melanger(accent, p.encreEntete, NOIR);
  const encreBarre = melanger(accent, p.encreBarre, NOIR);
  const piste = melanger(accent, p.pastillePiste, BLANC);
  return {
    navbar,
    rail,
    barreDeTitre: melanger(accent, p.titreTeinte, BLANC),
    navbarSurvol: melanger(accent, p.survolTeinte, BLANC),
    texteBarre: encreBarre,
    texteBarreAttenue: melanger(encreBarre, p.texteAttenueConserve, rail),
    panneau: melanger(accent, p.panneauTeinte, BLANC),
    navbarFin: melanger(navbar, p.degradeConserve, accent),
    railFin: melanger(rail, p.degradeConserve, accent),
    entetes: { modale: [debut, fin], tableau: [debut, fin], toolbar: [debut, fin] },
    enteteTexte: encre,
    onglet: { texte: encre, survol: debut },
    pastille: {
      piste,
      repos: p.pastilleRepos,
      survolFond: melanger(accent, 12, piste),
      survolTexte: encre,
      actifDebut: melanger(accent, p.pastilleActif, BLANC),
      actifFin: melanger(accent, p.pastilleActifFin, BLANC),
      actifTexte: encre,
    },
    kpi: [melanger(accent, p.kpiTeinte, BLANC), melanger(accent, p.kpiTeinteFin, BLANC)],
    fondPage: melanger(accent, p.pageTeinte, p.pageBase),
    ligneSelectionnee: {
      fond: melanger(accent, p.ligneSelectionnee, BLANC),
      survol: melanger(accent, p.ligneSelectionneeSurvol, BLANC),
      texte: encre,
      liseré: accent,
    },
    liensVerticaux: {
      survolFond: melanger(accent, p.survolLiensVerticaux, BLANC),
      survolTexte: encreBarre,
      actifTexte: encreBarre,
      actifBordure: accent,
      actifFond: melanger(accent, p.actifLiensVerticaux, BLANC),
    },
    sousOnglets: {
      actif: melanger(accent, p.encreOnglet, NOIR),
      survol: debut,
      pastilles: {
        primary: { bg: melanger(accent, p.encreOnglet, NOIR), texte: BLANC },
        success: { bg: palette['success'] ?? accent, texte: BLANC },
        warn: { bg: palette['warning'] ?? accent, texte: p.boutonEncre },
        danger: { bg: palette['danger'] ?? accent, texte: BLANC },
        info: { bg: palette['info'] ?? accent, texte: BLANC },
      },
    },
    accentApp: {
      bouton: accent,
      boutonSurvol: melanger(accent, p.boutonSurvol, NOIR),
      boutonActif: melanger(accent, p.boutonActif, NOIR),
      encre,
      lienSurvol: melanger(accent, p.boutonActif, NOIR),
    },
    boutons: Object.fromEntries(
      Object.entries(palette).map(([sev, bg]) => {
        const clair = sev === 'secondary' || sev === 'warning';
        return [
          sev,
          {
            bg,
            texte: clair ? p.boutonEncre : BLANC,
            survol: melanger(bg, clair ? p.semHoverClair : p.semHover, NOIR),
            actif: melanger(bg, clair ? p.semActiveClair : p.semActive, NOIR),
            contour: melanger(bg, clair ? p.semContourClair : p.semContour, NOIR),
          },
        ];
      }),
    ),
    icones: Object.fromEntries(Object.entries(teintes).map(([nom, c]) => [nom, melanger(c, p.teinteIcone, NOIR)])),
    fondsIcones: {
      'barre de navigation': navbar,
      rail,
      'panneau déroulant': melanger(accent, p.panneauTeinte, BLANC),
      'survol de la barre': melanger(accent, p.survolTeinte, BLANC),
    },
    active: accent,
    accent,
  };
}

export function mesurerChrome(theme: string, c: CouleursChrome): MesureChrome[] {
  const mesures: MesureChrome[] = [];
  const ajouter = (paire: string, a: string, b: string, seuil: number) => mesures.push({ theme, paire, ratio: contraste(a, b), seuil });
  ajouter('texte sur la barre de navigation', c.texteBarre, c.navbar, 4.5);
  ajouter('texte sur la fin de la barre de navigation', c.texteBarre, c.navbarFin, 4.5);
  ajouter('texte sur le survol de la barre de navigation', c.texteBarre, c.navbarSurvol, 4.5);
  ajouter('texte sur le rail', c.texteBarre, c.rail, 4.5);
  ajouter('texte sur la fin du rail', c.texteBarre, c.railFin, 4.5);
  ajouter('texte sur la barre de titre', c.texteBarre, c.barreDeTitre, 4.5);
  if (c.panneau) {
    ajouter('texte sur le panneau déroulant', c.texteBarre, c.panneau, 4.5);
  }
  if (c.texteBarreAttenue) {
    ajouter('texte atténué sur la barre de navigation', c.texteBarreAttenue, c.navbar, 4.5);
    ajouter('texte atténué sur le rail', c.texteBarreAttenue, c.rail, 4.5);
  }
  for (const [surface, [a, b]] of Object.entries(c.entetes)) {
    ajouter(`texte sur l'en-tête de ${surface} (début)`, c.enteteTexte, a, 4.5);
    ajouter(`texte sur l'en-tête de ${surface} (fin)`, c.enteteTexte, b, 4.5);
  }
  if (c.fondPage) {
    ajouter('texte courant sur le fond de page', TEXTE_COURANT, c.fondPage, 4.5);
    ajouter('texte atténué sur le fond de page', TEXTE_ATTENUE, c.fondPage, 4.5);
    // Non-texte : la carte blanche doit se détacher du fond (le cadre et l'ombre font le reste).
    ajouter('carte blanche sur le fond de page', BLANC, c.fondPage, 1.12);
  }
  if (c.onglet) {
    ajouter('onglet actif sur blanc', c.onglet.texte, BLANC, 4.5);
    ajouter('onglet actif sur sa teinte de survol', c.onglet.texte, c.onglet.survol, 4.5);
  }
  if (c.ligneSelectionnee) {
    const l = c.ligneSelectionnee;
    ajouter('ligne sélectionnée : texte sur son fond', l.texte, l.fond, 4.5);
    ajouter('ligne sélectionnée survolée : texte sur son fond', l.texte, l.survol, 4.5);
    ajouter('ligne sélectionnée : liseré sur son fond (non-texte)', l.liseré, l.fond, 3);
  }
  if (c.liensVerticaux) {
    const l = c.liensVerticaux;
    ajouter('lien vertical survolé : texte sur la teinte de survol', l.survolTexte, l.survolFond, 4.5);
    ajouter('lien vertical actif : texte sur son fond', l.actifTexte, l.actifFond, 4.5);
    ajouter('lien vertical actif : liseré sur son fond (non-texte)', l.actifBordure, l.actifFond, 3);
  }
  if (c.sousOnglets) {
    ajouter('sous-onglet actif : texte sur blanc', c.sousOnglets.actif, BLANC, 4.5);
    ajouter('sous-onglet inactif : gris #6b7280 sur blanc', '#6b7280', BLANC, 4.5);
    ajouter('sous-onglet survolé : texte foncé sur la teinte de survol', c.sousOnglets.actif, c.sousOnglets.survol, 4.5);
    for (const [sev, b] of Object.entries(c.sousOnglets.pastilles)) {
      ajouter(`sous-onglet, pastille ${sev} : texte sur fond`, b.texte, b.bg, 4.5);
    }
  }
  if (c.accentApp) {
    const a = c.accentApp;
    ajouter('bouton primaire : blanc sur l’accent', BLANC, a.bouton, 4.5);
    ajouter('bouton primaire survolé : blanc sur l’accent assombri', BLANC, a.boutonSurvol, 4.5);
    ajouter('bouton primaire actif : blanc sur l’accent assombri', BLANC, a.boutonActif, 4.5);
    ajouter('bouton contour : encre sur blanc', a.encre, BLANC, 4.5);
    ajouter('lien : encre sur blanc', a.encre, BLANC, 4.5);
    ajouter('lien survolé sur blanc', a.lienSurvol, BLANC, 4.5);
    if (c.fondPage) {
      ajouter('lien : encre sur le fond de page', a.encre, c.fondPage, 4.5);
      ajouter('lien survolé sur le fond de page', a.lienSurvol, c.fondPage, 4.5);
    }
    ajouter('accent sur blanc (case cochée, filet, non-texte)', a.bouton, BLANC, 3);
  }
  if (c.boutons) {
    for (const [sev, b] of Object.entries(c.boutons)) {
      ajouter(`bouton ${sev} : texte sur fond`, b.texte, b.bg, 4.5);
      ajouter(`bouton ${sev} survolé : texte sur fond`, b.texte, b.survol, 4.5);
      ajouter(`bouton ${sev} actif : texte sur fond`, b.texte, b.actif, 4.5);
      ajouter(`bouton contour ${sev} : texte sur blanc`, b.contour, BLANC, 4.5);
      if (c.fondPage) {
        ajouter(`bouton contour ${sev} : texte sur le fond de page`, b.contour, c.fondPage, 4.5);
      }
      if (sev !== 'secondary' && sev !== 'contrast') {
        // Écart de teinte (ΔE CIE76) au primaire : au-dessous de ~25, deux boutons se confondent.
        mesures.push({ theme, paire: `bouton ${sev} : écart de teinte au primaire (ΔE)`, ratio: ecartCouleur(b.bg, c.accent), seuil: 25 });
      }
    }
  }
  if (c.pastille) {
    ajouter('pastille au repos sur sa piste', c.pastille.repos, c.pastille.piste, 4.5);
    ajouter('pastille survolée sur sa teinte', c.pastille.survolTexte, c.pastille.survolFond, 4.5);
    ajouter('pastille active (début)', c.pastille.actifTexte, c.pastille.actifDebut, 4.5);
    ajouter('pastille active (fin)', c.pastille.actifTexte, c.pastille.actifFin, 4.5);
  }
  if (c.kpi) {
    ajouter('texte courant sur le bandeau d’indicateurs (début)', TEXTE_COURANT, c.kpi[0], 4.5);
    ajouter('texte courant sur le bandeau d’indicateurs (fin)', TEXTE_COURANT, c.kpi[1], 4.5);
  }
  if (c.icones && c.fondsIcones) {
    for (const [nom, icone] of Object.entries(c.icones)) {
      for (const [fond, couleur] of Object.entries(c.fondsIcones)) {
        ajouter(`icône « ${nom} » sur ${fond}`, icone, couleur, 3);
      }
    }
  }
  ajouter('entrée active sur la barre de navigation', c.active, c.navbar, 3);
  ajouter('entrée active sur le rail', c.active, c.rail, 3);
  ajouter('repère d’accent sur le rail', c.accent, c.rail, 3);
  return mesures;
}

/** Empilement des trois bandes : la barre de titre ancre la pile (L barre de titre < L rail < L navbar). */
export function empilementTenu(c: CouleursChrome): boolean {
  return luminance(c.barreDeTitre) < luminance(c.rail) && luminance(c.rail) < luminance(c.navbar);
}

/** Accent de chaque fond de page : `accent: #…` dans `$pharma-themes` de `_pharma-themes.scss`. */
export function lireAccentsFonds(scss: string): Record<string, string> {
  const accents: Record<string, string> = {};
  for (const m of scss.matchAll(new RegExp("'([\\w-]+)':\\s*\\(\\s*app-bg:[\\s\\S]*?\\baccent:\\s*(" + HEX + ')', 'g'))) {
    accents[m[1]] = m[2].toLowerCase();
  }
  if (Object.keys(accents).length === 0) {
    throw new Error('Aucun fond de page lu dans _pharma-themes.scss');
  }
  return accents;
}

/** Infobulle : `color-mix(accent 25 %, #1e293b)` sous texte blanc. */
export function mesurerInfobulle(fond: string, accent: string, scss: string): MesureChrome {
  const m = new RegExp('--bs-tooltip-bg:\\s*color-mix\\(in srgb,\\s*var\\(--pharma-accent\\)\\s*(\\d+)%,\\s*(' + HEX + ')\\)').exec(scss);
  if (!m) {
    throw new Error('--bs-tooltip-bg introuvable dans _pharma-themes.scss');
  }
  return {
    theme: `fond ${fond}`,
    paire: 'blanc sur l’infobulle',
    ratio: contraste(BLANC, melanger(accent, Number(m[1]), m[2])),
    seuil: 7,
  };
}

/** Palette des icônes (`--icon-*`, niveau 600) de `icon-colors-global.scss`, dont les teintes de navigation sont dérivées. */
export function lireTeintesIcones(scss: string): Record<string, string> {
  const teintes: Record<string, string> = {};
  for (const m of scss.matchAll(new RegExp('--icon-(emerald|blue|amber|violet|rose|teal|orange|indigo|lime|fuchsia|cyan|pink):\\s*(' + HEX + ')', 'g'))) {
    teintes[m[1]] = m[2].toLowerCase();
  }
  if (Object.keys(teintes).length !== 12) {
    throw new Error('Les 12 teintes --icon-* sont introuvables dans icon-colors-global.scss');
  }
  return teintes;
}

/** Palette des boutons sémantiques : la table `$pharma-chrome-buttons`, par thème puis par sévérité. */
export function lireBoutons(scss: string): Record<string, Record<string, string>> {
  const bloc = /\$pharma-chrome-buttons:\s*\(([\s\S]*?)\n\);/.exec(scss)?.[1] ?? '';
  const palette: Record<string, Record<string, string>> = {};
  for (const m of bloc.matchAll(new RegExp("'([\\w-]+)-(secondary|success|info|warning|danger|help|contrast)':\\s*(" + HEX + ')', 'g'))) {
    (palette[m[1]] ??= {})[m[2]] = m[3].toLowerCase();
  }
  if (Object.keys(palette).length === 0) {
    throw new Error('$pharma-chrome-buttons vide ou introuvable');
  }
  return palette;
}
