/**
 * Accessibilité des styles du comptoir (docs/PLAN-STYLES-COMPTOIR.md, §4) : contrôle axe du contraste
 * pour chaque style × chaque mode, puis captures sous simulation de daltonisme.
 *
 * Lecture seule : aucune vente n'est créée, les écrans sont ouverts à vide. Le style est imposé par
 * `?style=…`, sans rien enregistrer dans le navigateur.
 *
 * Règle d'acceptation : aucun défaut de contraste. Le style Billetage étant le seul, il n'y a plus de témoin à comparer ;
 * le détail des défauts éventuels est écrit dans `target/a11y-comptoir/axe.json`.
 */
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { RACINE } from '../src/config';

const DOSSIER = join(RACINE, 'target', 'a11y-comptoir');
// Le style Billetage est le seul depuis le 2026-10-03 : les trois autres ont été supprimés.
const STYLES = ['billetage'] as const;

const ECRANS = [
  { nom: 'comptant', url: '/sales-home', onglet: null },
  { nom: 'assurance', url: '/sales-home', onglet: /Assurance/ },
  { nom: 'carnet', url: '/sales-home', onglet: /Carnet/ },
  { nom: 'prevente', url: '/sales-home/prevente', onglet: null },
  { nom: 'devis', url: '/sales-home/devis', onglet: null },
] as const;

// Machado, Oliveira, Fernandes (2009), sévérité 1,0 ; `feColorMatrix` travaille en RGB linéaire.
const DEFICIENCES: Record<string, string> = {
  protanopie: '0.152286 1.052583 -0.204868 0 0  0.114503 0.786281 0.099216 0 0  -0.003882 -0.048116 1.051998 0 0  0 0 0 1 0',
  deuteranopie: '0.367322 0.860646 -0.227968 0 0  0.280085 0.672501 0.047413 0 0  -0.01182 0.04294 0.968881 0 0  0 0 0 1 0',
  tritanopie: '1.255528 -0.076749 -0.178779 0 0  -0.078411 0.930809 0.147602 0 0  0.004733 0.691367 0.3039 0 0  0 0 0 1 0',
};

async function ouvrir(page: Page, style: string, ecran: (typeof ECRANS)[number]): Promise<void> {
  await page.goto(ecran.url);
  await expect(page.locator('.pharma-sales-layout')).toBeVisible();
  if (ecran.onglet) {
    await page.getByRole('tab', { name: ecran.onglet }).click();
  }
  // Les transitions et le rendu du dernier onglet se posent après le clic.
  await page.waitForTimeout(800);
}

test.describe.configure({ mode: 'serial' });

const defauts: Record<string, Record<string, number>> = {};

for (const style of STYLES) {
  for (const ecran of ECRANS) {
    test(`contraste axe — ${style} / ${ecran.nom}`, async ({ page }) => {
      await ouvrir(page, style, ecran);

      const resultat = await new AxeBuilder({ page }).include('app-sales-home').withRules(['color-contrast']).analyze();
      const noeuds = resultat.violations.flatMap(v => v.nodes);

      (defauts[ecran.nom] ??= {})[style] = noeuds.length;
      mkdirSync(DOSSIER, { recursive: true });
      writeFileSync(
        join(DOSSIER, `axe-${style}-${ecran.nom}.json`),
        JSON.stringify(noeuds.map(n => ({ cible: n.target, resume: n.failureSummary })), null, 2),
      );
    });
  }
}

test('aucun défaut de contraste sur les écrans à vide', () => {
  mkdirSync(DOSSIER, { recursive: true });
  writeFileSync(join(DOSSIER, 'axe.json'), JSON.stringify(defauts, null, 2));

  for (const ecran of ECRANS) {
    expect(defauts[ecran.nom]?.['billetage'] ?? 0, `${ecran.nom} : défauts de contraste`).toBe(0);
  }
});

test.describe('simulation de daltonisme', () => {
  for (const [deficience, matrice] of Object.entries(DEFICIENCES)) {
    for (const ecran of ECRANS) {
      test(`${deficience} — ${ecran.nom}`, async ({ page }) => {
        await ouvrir(page, 'billetage', ecran);
        await page.evaluate(
          ([nom, valeurs]) => {
            const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="0" height="0" style="position:absolute"><filter id="${nom}"><feColorMatrix type="matrix" values="${valeurs}"/></filter></svg>`;
            document.body.insertAdjacentHTML('beforeend', svg);
            document.documentElement.style.filter = `url(#${nom})`;
          },
          [deficience, matrice],
        );
        mkdirSync(DOSSIER, { recursive: true });
        await page.screenshot({ path: join(DOSSIER, `${deficience}-${ecran.nom}.png`) });
      });
    }
  }
});
