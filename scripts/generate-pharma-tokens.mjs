/**
 * Génère `content/scss/_pharma-tokens.scss` — snapshot figé des variables CSS `--p-*`
 * produites au runtime par `providePrimeNG({ theme: { preset: Aura } })`.
 *
 * Cf. docs/PLAN-MIGRATION-PRIMENG-VERS-NGBOOTSTRAP.md §8.2 / §8.3.
 *
 * Contrairement à la procédure §8.3 (copier/coller depuis la console navigateur), ce
 * script rejoue le moteur de theming de PrimeNG (`@primeuix/styled`) hors navigateur :
 * même preset, mêmes options par défaut, donc mêmes valeurs — et c'est reproductible.
 *
 * ┌─ Cycle de vie ────────────────────────────────────────────────────────────┐
 * │ Pendant la migration (Phases 0→3) : jeu COMPLET (défaut).                  │
 * │   Le Design System en cours d'écriture consomme de nouveaux tokens à       │
 * │   chaque composant — les avoir tous évite de devoir régénérer sans cesse.  │
 * │   Surcoût : ~1,8 Ko gzip, invisible tant que PrimeNG est encore installé.  │
 * │                                                                            │
 * │ En Phase 4, AVANT `npm uninstall primeng @primeuix/themes` :               │
 * │   relancer avec `--used-only` pour élaguer. Le jeu de tokens est alors     │
 * │   stable, l'élagage est donc sûr.                                          │
 * │                                                                            │
 * │ ⚠ L'ordre est impératif : une fois PrimeNG désinstallé, ce script n'a plus │
 * │   de source de valeurs et `_pharma-tokens.scss` devient la seule vérité,   │
 * │   figée définitivement. Élaguer après coup est impossible.                 │
 * │   (Oublier `--used-only` est en revanche sans danger : on garde 1,8 Ko.)   │
 * └────────────────────────────────────────────────────────────────────────────┘
 *
 * Usage : node scripts/generate-pharma-tokens.mjs [--used-only]
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join, relative } from 'node:path';
import { execFileSync } from 'node:child_process';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const WEBAPP = join(ROOT, 'pharmaSmart-app/src/main/webapp');
const OUTPUT = join(WEBAPP, 'content/scss/_pharma-tokens.scss');

// Options par défaut de providePrimeNG — app.config.ts ne surcharge que `theme.preset`.
const THEME_OPTIONS = { prefix: 'p', darkModeSelector: 'system', cssLayer: false };

/** Rejoue le moteur de theming et retourne le CSS des variables. */
async function generateCss() {
  // On utilise le `@primeuix/styled` imbriqué dans primeng : c'est celui qui tourne à l'exécution.
  // pathToFileURL : sur Windows, un chemin absolu `D:\…` n'est pas une URL ESM valide.
  const load = path => import(pathToFileURL(join(ROOT, path)).href);

  let Theme, Aura;
  try {
    ({ Theme } = await load('node_modules/primeng/node_modules/@primeuix/styled/dist/index.mjs'));
    ({ default: Aura } = await load('node_modules/@primeuix/themes/dist/aura/index.mjs'));
  } catch (cause) {
    throw new Error(
      'PrimeNG / @primeuix/themes est introuvable dans node_modules.\n\n' +
        "Si c'est parce que la Phase 4 est passée et que PrimeNG a été désinstallé, c'est\n" +
        'normal et définitif : ce script n\'a plus de source de valeurs.\n' +
        `${relative(ROOT, OUTPUT)} est désormais la source de vérité — éditez-le à la main.\n\n` +
        'Sinon, lancez `npm install`.',
      { cause },
    );
  }

  Theme.setTheme({ preset: Aura, options: THEME_OPTIONS });

  // getCommonStyleSheet() renvoie des blocs `<style …>…</style>` concaténés.
  return Theme.getCommonStyleSheet()
    .split('</style>')
    .map(block => block.slice(block.indexOf('>') + 1))
    .join('\n')
    .trim();
}

/**
 * Tokens `--p-*` réellement référencés dans le code applicatif.
 *
 * `--untracked` est indispensable : les fichiers SCSS nouvellement créés (et pas encore
 * commités) référencent des tokens qu'il faut conserver. Le fichier de sortie est exclu
 * du balayage, sinon l'ensemble s'auto-entretiendrait et plus rien ne serait jamais élagué.
 */
function collectUsedTokens() {
  const out = execFileSync(
    'git',
    [
      'grep', '--untracked', '-hoI', '-E', '--', '--p-[a-zA-Z0-9-]+',
      '--', relative(ROOT, WEBAPP), `:(exclude)${relative(ROOT, OUTPUT).replace(/\\/g, '/')}`,
    ],
    { cwd: ROOT, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 },
  );
  // Un nom se terminant par `-` provient d'un glob écrit en commentaire (`--p-form-field-*`),
  // pas d'une vraie référence : on l'écarte pour ne pas polluer le rapport des orphelins.
  return new Set(out.split('\n').filter(name => name && !name.endsWith('-')));
}

/**
 * Découpe le CSS en règles `{ selector, media, declarations }`, **en préservant
 * l'ordre source** — la cascade en dépend : le bloc `@media (prefers-color-scheme: dark)`
 * doit rester après les définitions claires qu'il surcharge.
 */
function parseRules(css) {
  const rules = [];
  // Un seul balayage séquentiel : `@media …{ … }` ou `sel{ … }`, dans l'ordre.
  const re = /@media\s*([^{]+)\{((?:[^{}]*\{[^{}]*\})*)\}|([^{}@]+)\{([^{}]*)\}/g;

  const pushRule = (selector, body, media) => {
    const declarations = body
      .split(';')
      .map(d => d.trim())
      .filter(d => d.startsWith('--'))
      .map(d => {
        const i = d.indexOf(':');
        return [d.slice(0, i).trim(), d.slice(i + 1).trim()];
      });
    if (declarations.length) rules.push({ selector: selector.trim(), media, declarations });
  };

  let m;
  while ((m = re.exec(css)) !== null) {
    if (m[1] !== undefined) {
      const media = m[1].trim();
      const innerRe = /([^{}]+)\{([^{}]*)\}/g;
      let inner;
      while ((inner = innerRe.exec(m[2])) !== null) pushRule(inner[1], inner[2], media);
    } else {
      pushRule(m[3], m[4], null);
    }
  }

  return rules;
}

/**
 * Étend l'ensemble des tokens voulus avec leurs dépendances transitives :
 * `--p-primary-color: var(--p-primary-500)` impose de garder `--p-primary-500`.
 */
function withTransitiveDeps(wanted, rules) {
  const valuesByName = new Map();
  for (const rule of rules) {
    for (const [name, value] of rule.declarations) {
      if (!valuesByName.has(name)) valuesByName.set(name, []);
      valuesByName.get(name).push(value);
    }
  }

  const kept = new Set();
  const queue = [...wanted].filter(name => valuesByName.has(name));
  while (queue.length) {
    const name = queue.pop();
    if (kept.has(name)) continue;
    kept.add(name);
    for (const value of valuesByName.get(name) ?? []) {
      for (const ref of value.matchAll(/var\(\s*(--p-[a-zA-Z0-9-]+)/g)) {
        if (!kept.has(ref[1])) queue.push(ref[1]);
      }
    }
  }
  return kept;
}

/**
 * Bloc documentaire listant les tokens référencés par le code mais qu'Aura ne produit pas.
 * Ce sont des reliquats du theming PrimeNG ≤ 17 : ils sont **déjà** non résolus
 * aujourd'hui. On ne les définit pas ici — les définir changerait le rendu actuel,
 * ce que la migration s'interdit (plan §1.3 : « pas de refonte visuelle »).
 */
function renderOrphansComment(orphans) {
  if (!orphans.length) return '';
  const lines = orphans.map(name => ` *   ${name}`).join('\n');
  return `
/**
 * ⚠ Tokens référencés dans le code mais NON produits par le preset Aura.
 *
 * Ils ne résolvent donc à rien aujourd'hui non plus (sauf usage avec fallback
 * \`var(--x, valeur)\`) : ce sont des reliquats du theming PrimeNG ≤ 17, dont les
 * noms ont changé en v18+ (ex. \`--p-surface-border\` → \`--p-content-border-color\`).
 *
 * Volontairement NON définis ici : les définir corrigerait le rendu actuel, donc le
 * modifierait — hors périmètre de la migration. À traiter dans un chantier dédié.
 *
${lines}
 */
`;
}

function renderScss(rules, kept, stats) {
  const header = `/**
 * Tokens PharmaSmart — snapshot figé des variables CSS \`--p-*\`.
 *
 * NE PAS ÉDITER À LA MAIN — généré par \`node scripts/generate-pharma-tokens.mjs\`
 * depuis le preset Aura de @primeuix/themes (options par défaut de providePrimeNG :
 * prefix "p", darkModeSelector "system", cssLayer false).
 *
 * Rôle : reproduire à l'identique, en CSS statique, ce que \`providePrimeNG\` injecte
 * aujourd'hui au runtime — pour que le retrait de PrimeNG soit invisible pour les
 * ~380 références \`--p-*\` du code (cf. plan de migration §8.2, décision §14.4 :
 * les tokens \`--p-*\` sont conservés définitivement comme convention de nommage).
 *
${
    stats.usedOnly
      ? ` * Mode élagué (--used-only) : ${stats.kept} / ${stats.total} tokens produits par Aura
 * (${stats.used} référencés par le code + ${stats.kept - stats.used} dépendances transitives).
 * Élaguer suppose que le jeu de tokens est stable — à ne faire qu'en Phase 4.`
      : ` * Mode complet : les ${stats.kept} tokens produits par Aura, y compris ceux qu'aucun
 * code ne référence encore. Volontaire pendant la migration — le Design System en
 * cours d'écriture en consomme de nouveaux à chaque composant.
 * Élagage prévu en Phase 4 via \`--used-only\` (${stats.used} référencés à ce jour).`
  }
 *
 * L'ordre des règles reproduit celui du CSS injecté au runtime — la cascade en dépend.
 */
`;

  const blocks = [];
  for (const rule of rules) {
    const declarations = rule.declarations.filter(([name]) => kept.has(name));
    if (!declarations.length) continue;

    const body = declarations.map(([name, value]) => `    ${name}: ${value};`).join('\n');
    const inner = `  ${rule.selector} {\n${body}\n  }`;
    blocks.push(rule.media ? `@media ${rule.media} {\n${inner}\n}` : inner.replace(/^ {2}/gm, ''));
  }

  return `${header}\n${blocks.join('\n\n')}\n${renderOrphansComment(stats.orphans)}`;
}

// Défaut : jeu complet. `--used-only` n'est à employer qu'en Phase 4 (cf. cycle de vie en tête).
const usedOnly = process.argv.includes('--used-only');

const css = await generateCss();
const rules = parseRules(css);
const allNames = new Set(rules.flatMap(r => r.declarations.map(([name]) => name)));

const referenced = collectUsedTokens();
const used = new Set([...referenced].filter(name => allNames.has(name)));
const kept = usedOnly ? withTransitiveDeps(used, rules) : allNames;
const orphans = [...referenced].filter(name => !allNames.has(name)).sort();

const scss = renderScss(rules, kept, {
  kept: kept.size,
  total: allNames.size,
  used: used.size,
  orphans,
  usedOnly,
});
writeFileSync(OUTPUT, scss, 'utf8');

console.log(`✔ ${relative(ROOT, OUTPUT)}`);
console.log(`  mode : ${usedOnly ? 'élagué (--used-only)' : 'complet'}`);
console.log(`  ${kept.size} tokens écrits sur ${allNames.size} produits par Aura (${used.size} référencés par le code)`);
if (!usedOnly) {
  console.log('  → élagage prévu en Phase 4 : relancer avec --used-only AVANT de désinstaller primeng');
}
console.log(`  ⚠ ${orphans.length} référencés mais non produits par Aura (déjà non résolus aujourd'hui) :`);
for (const name of orphans) console.log(`      ${name}`);
