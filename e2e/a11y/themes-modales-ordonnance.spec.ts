/**
 * Contraste (axe) des fenêtres « Ordonnance requise » et « Prescripteurs » sous chaque thème de couleur
 * (docs/PLAN-ORDONNANCE-RESTE-A-FAIRE.md §1.6 ; méthode de themes-panneau-ordonnance.spec.ts).
 *
 * « Actuel » est la référence : seuls les éléments en défaut sous un thème et pas sous « Actuel » comptent. La mesure se limite
 * à la fenêtre ouverte (`.modal-content`). Le thème change à chaud (attribut `data-chrome` de `<html>`).
 *
 * Parcours ÉCRIVANT dans la base : une vente en cours (abandonnée à la fin) et un prescripteur d'essai (supprimé).
 */
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, rattacherUnClient } from '../src/actions';
import { executer, rejouer } from '../src/base-de-donnees';

const THEMES = ['actuel', 'comptant', 'assurance', 'prevente-comptant', 'prevente-carnet'] as const;
const PRODUIT = 'AMOXICILLINE TEVA 1 g';

let elementsVerifies = 0;

async function mesurer(page: Page, theme: string): Promise<{ cible: string; ratio: string }[]> {
  await page.evaluate(t => document.documentElement.setAttribute('data-chrome', t), theme);
  await page.waitForTimeout(400);
  const resultat = await new AxeBuilder({ page }).include('.modal-content').withRules(['color-contrast']).analyze();
  elementsVerifies += resultat.passes.reduce((n, r) => n + r.nodes.length, 0) + resultat.violations.reduce((n, r) => n + r.nodes.length, 0);
  return resultat.violations.flatMap(v =>
    v.nodes.map(n => ({ cible: n.target.join(' '), ratio: ((n.any[0]?.data ?? {}) as { contrastRatio?: number }).contrastRatio?.toFixed(2) ?? '?' })),
  );
}

/** Défauts propres à un thème : présents sous lui, absents sous « Actuel ». */
async function defautsParTheme(page: Page, fenetre: string): Promise<string[]> {
  const reference = new Set((await mesurer(page, 'actuel')).map(d => d.cible));
  const nouveaux: string[] = [];
  for (const theme of THEMES.filter(t => t !== 'actuel')) {
    for (const d of await mesurer(page, theme)) {
      if (!reference.has(d.cible)) {
        nouveaux.push(`${fenetre} | ${theme} : ${d.cible} (${d.ratio}:1)`);
      }
    }
  }
  await page.evaluate(() => document.documentElement.setAttribute('data-chrome', 'actuel'));
  return nouveaux;
}

test.afterAll(() => {
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
});

test("thèmes de couleur : les fenêtres « Ordonnance requise » et « Prescripteurs » n'introduisent aucun défaut de contraste", async ({ page }) => {
  test.setTimeout(300_000);
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
  executer("INSERT INTO prescripteur (nom, prenom) VALUES ('ESSAI E2E A11Y', 'Dr');");

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, PRODUIT);
  await ajouterAuPanier(page, '1');
  await rattacherUnClient(page);

  const nouveaux: string[] = [];
  const modale = page.locator('.modal-content');

  // « Ordonnance requise » : refus de la clôture d'un produit sur ordonnance, sans ordonnance ni prescripteur.
  const recherche = page.locator('#produitbox');
  await recherche.click();
  await recherche.press('Enter');
  await expect(page.locator('#CASH')).toBeVisible();
  await page.locator('#CASH').fill('100000');
  await page.getByRole('button', { name: 'Finaliser' }).click();
  await expect(modale).toContainText('Ordonnance requise');
  nouveaux.push(...(await defautsParTheme(page, 'ordonnance requise')));
  await modale.getByRole('button', { name: 'Revenir à la vente' }).click();
  await expect(modale).toBeHidden();

  // « Prescripteurs » : ouverte depuis le panneau Ordonnance du client.
  await page
    .locator('app-customer-overlay-panel')
    .filter({ visible: true })
    .first()
    .getByRole('button', { name: 'Ordonnances du client' })
    .click();
  const panneau = page.locator('.offcanvas.show');
  await panneau.getByRole('button', { name: 'Prescripteurs' }).click();
  await expect(modale).toContainText('Prescripteurs');
  await expect(modale).toContainText('ESSAI E2E A11Y');
  nouveaux.push(...(await defautsParTheme(page, 'prescripteurs')));
  await modale.locator('.btn-close').click();
  await panneau.getByRole('button', { name: 'Fermer' }).click();

  await assurerPanierVide(page);
  expect(elementsVerifies, 'axe doit avoir examiné les deux fenêtres').toBeGreaterThan(30);
  console.log(`éléments examinés par axe : ${elementsVerifies}`);
  expect(nouveaux, 'défauts de contraste propres à un thème (absents sous « Actuel »)').toEqual([]);
});
