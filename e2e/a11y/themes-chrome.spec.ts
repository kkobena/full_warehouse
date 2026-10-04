/**
 * Contraste (axe) des thèmes de couleur de l'application (docs/PLAN-THEMES-APPLICATION.md, §6, phase 4).
 *
 * Principe : « Actuel » est la référence. Pour chaque écran et chaque disposition (barre horizontale, rail), on relève les
 * éléments en défaut de contraste sous « Actuel », puis sous chaque autre thème ; **seuls les éléments en défaut sous le
 * thème et pas sous « Actuel » comptent** : ce sont ceux que le thème a introduits. Les défauts déjà présents sous
 * « Actuel » ne sont pas l'objet de ce test.
 *
 * Limite d'axe : il ne sait pas évaluer un texte posé sur un dégradé (en-têtes, barres) ; ces zones sont couvertes par le
 * calcul (`theme-contraste.spec.ts`). Lecture seule : aucune écriture en base.
 *
 * Résultats complets dans `target/a11y-themes/axe-themes.json`.
 */
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { RACINE } from '../src/config';

const DOSSIER = join(RACINE, 'target', 'a11y-themes');
const THEMES = ['actuel', 'comptant', 'assurance', 'prevente-comptant', 'prevente-carnet'] as const;
const DISPOSITIONS = ['navbar', 'sidebar'] as const;

interface Ecran {
  nom: string;
  url: string;
  /** Action facultative avant la mesure (ouvrir une modale…). */
  preparer?: (page: Page) => Promise<void>;
}

const ECRANS: Ecran[] = [
  { nom: 'accueil', url: '/' },
  { nom: 'catalogue-produits', url: '/produits' },
  { nom: 'mouvements-caisse', url: '/mvt-caisse' },
  { nom: 'familles', url: '/famille-produit' },
  { nom: 'clients', url: '/customer' },
  { nom: 'differes', url: '/differes' },
  { nom: 'journal-ventes', url: '/sales-home/gestion' },
  { nom: 'comptoir', url: '/sales-home' },
  {
    nom: 'modale-famille',
    url: '/famille-produit',
    preparer: async page => {
      await page.getByRole('button', { name: /Nouveau/ }).first().click();
      await page.locator('.modal-content').first().waitFor();
    },
  },
];

interface Defaut {
  cible: string;
  ratio: string;
}

async function mesurer(page: Page, ecran: Ecran): Promise<Defaut[]> {
  await page.goto(ecran.url);
  await page.waitForLoadState('networkidle');
  if (ecran.preparer) {
    await ecran.preparer(page);
  }
  await page.waitForTimeout(500);
  const resultat = await new AxeBuilder({ page }).withRules(['color-contrast']).analyze();
  const defauts: Defaut[] = [];
  for (const v of resultat.violations) {
    for (const n of v.nodes) {
      const donnees = (n.any[0]?.data ?? {}) as { contrastRatio?: number };
      defauts.push({ cible: n.target.join(' '), ratio: donnees.contrastRatio?.toFixed(2) ?? '?' });
    }
  }
  return defauts;
}

test('thèmes de couleur : aucun défaut de contraste introduit par un thème', async ({ browser }) => {
  test.setTimeout(900_000);
  mkdirSync(DOSSIER, { recursive: true });
  const rapport: Record<string, unknown> = {};
  const nouveaux: string[] = [];

  // `E2E_ECRAN=journal-ventes` ne rejoue qu'un écran (diagnostic d'un défaut isolé, ou d'une page lue trop tôt).
  const ecrans = ECRANS.filter(e => !process.env.E2E_ECRAN || e.nom === process.env.E2E_ECRAN);
  for (const disposition of DISPOSITIONS) {
    for (const ecran of ecrans) {
      const reference = new Set<string>();
      for (const theme of THEMES) {
        const contexte = await browser.newContext({ storageState: join(RACINE, 'e2e', '.auth', 'session.json'), viewport: { width: 1920, height: 1080 } });
        await contexte.addInitScript(
          ([d, t]) => {
            localStorage.setItem('pharmasmart_layout_mode', d);
            localStorage.setItem('pharmasmart_chrome', t);
            localStorage.setItem('pharmasmart_sidebar_collapsed', 'false');
          },
          [disposition, theme],
        );
        const page = await contexte.newPage();
        try {
          const defauts = await mesurer(page, ecran);
          rapport[`${disposition} | ${ecran.nom} | ${theme}`] = defauts;
          if (theme === 'actuel') {
            defauts.forEach(d => reference.add(d.cible));
          } else {
            for (const d of defauts) {
              if (!reference.has(d.cible)) {
                nouveaux.push(`${disposition} | ${ecran.nom} | ${theme} : ${d.cible} (${d.ratio}:1)`);
              }
            }
          }
        } finally {
          await contexte.close();
        }
      }
    }
  }

  writeFileSync(join(DOSSIER, 'axe-themes.json'), JSON.stringify({ nouveaux, rapport }, null, 2));
  expect(nouveaux, 'défauts de contraste propres à un thème (absents sous « Actuel »)').toEqual([]);
});
