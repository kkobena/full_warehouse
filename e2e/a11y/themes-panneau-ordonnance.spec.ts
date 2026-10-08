/**
 * Contraste (axe) du panneau Ordonnance de l'écran de vente sous chaque thème de couleur
 * (docs/PLAN-EXTRACTION-ORDONNANCE-OCR.md §21 ; méthode de themes-chrome.spec.ts).
 *
 * « Actuel » est la référence : seuls les éléments en défaut sous un thème et pas sous « Actuel » comptent. La mesure se limite
 * au panneau (`.offcanvas.show`), sur ses deux vues : la liste (une ordonnance posée en base) et la saisie. Le thème change à
 * chaud (attribut `data-chrome` de `<html>`), la vente et l'ordonnance n'étant préparées qu'une fois.
 *
 * Parcours ÉCRIVANT dans la base : une vente en cours (abandonnée à la fin), un prescripteur et une ordonnance d'essai (supprimés).
 */
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide, ajouterAuPanier, chercherProduit, rattacherUnClient } from '../src/actions';
import { executer, lire, rejouer } from '../src/base-de-donnees';

const THEMES = ['actuel', 'comptant', 'assurance', 'prevente-comptant', 'prevente-carnet'] as const;

let elementsVerifies = 0;

async function mesurer(page: Page, theme: string): Promise<{ cible: string; ratio: string }[]> {
  await page.evaluate(t => document.documentElement.setAttribute('data-chrome', t), theme);
  await page.waitForTimeout(400);
  const resultat = await new AxeBuilder({ page }).include('.offcanvas.show').withRules(['color-contrast']).analyze();
  elementsVerifies += resultat.passes.reduce((n, r) => n + r.nodes.length, 0) + resultat.violations.reduce((n, r) => n + r.nodes.length, 0);
  return resultat.violations.flatMap(v =>
    v.nodes.map(n => ({ cible: n.target.join(' '), ratio: ((n.any[0]?.data ?? {}) as { contrastRatio?: number }).contrastRatio?.toFixed(2) ?? '?' })),
  );
}

test.afterAll(() => {
  rejouer('scripts/essai-ordonnance/nettoyer.sql');
});

test('thèmes de couleur : le panneau ordonnance n\'introduit aucun défaut de contraste', async ({ page }) => {
  test.setTimeout(300_000);
  rejouer('scripts/essai-ordonnance/nettoyer.sql');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.goto('/sales-home');
  await chercherProduit(page, 'DOLIPRANE 500');
  await ajouterAuPanier(page, '1');
  await rattacherUnClient(page);

  const client = lire("SELECT customer_id FROM sales WHERE statut = 'ACTIVE' AND customer_id IS NOT NULL ORDER BY created_at DESC LIMIT 1");
  expect(client, 'la vente en cours porte le client choisi').not.toBe('');
  executer(`
    WITH p AS (INSERT INTO prescripteur (nom, prenom) VALUES ('ESSAI E2E A11Y', 'Dr') RETURNING id),
         o AS (INSERT INTO ordonnance (customer_id, prescripteur_id, date_prescription, renouvellements, date_fin_validite)
               SELECT ${Number(client)}, p.id, current_date, 1, current_date + 90 FROM p RETURNING id)
    INSERT INTO ordonnance_ligne (ordonnance_id, rang, produit_id, posologie, duree_jours, quantite_prescrite)
    SELECT o.id, 1, (SELECT id FROM produit WHERE libelle LIKE 'AMOXICILLINE TEVA 1 g%' ORDER BY id LIMIT 1), '1 cp x 3 /j', 7, 2 FROM o;`);

  await page
    .locator('app-customer-overlay-panel')
    .filter({ visible: true })
    .first()
    .getByRole('button', { name: 'Ordonnances du client' })
    .click();
  const panneau = page.locator('.offcanvas.show');
  await expect(panneau).toContainText('AMOXICILLINE');

  const nouveaux: string[] = [];
  for (const vue of ['liste', 'saisie']) {
    if (vue === 'saisie') {
      await panneau.getByRole('button', { name: 'Nouvelle ordonnance' }).click();
      await expect(panneau).toContainText('Nouvelle ordonnance');
    }
    const reference = new Set((await mesurer(page, 'actuel')).map(d => d.cible));
    for (const theme of THEMES.filter(t => t !== 'actuel')) {
      for (const d of await mesurer(page, theme)) {
        if (!reference.has(d.cible)) {
          nouveaux.push(`${vue} | ${theme} : ${d.cible} (${d.ratio}:1)`);
        }
      }
    }
  }

  await panneau.getByRole('button', { name: 'Fermer' }).click();

  // La pastille du bouton (hors panneau) sous chaque thème : même règle, comparée à « Actuel ».
  const bouton = page.locator('app-ordonnance-bouton').filter({ visible: true }).first();
  await expect(bouton).toContainText('1');
  const referenceBouton = new Set<string>();
  for (const theme of THEMES) {
    await page.evaluate(t => document.documentElement.setAttribute('data-chrome', t), theme);
    await page.waitForTimeout(400);
    const r = await new AxeBuilder({ page }).include('app-ordonnance-bouton').withRules(['color-contrast']).analyze();
    for (const v of r.violations) {
      for (const n of v.nodes) {
        const cible = n.target.join(' ');
        if (theme === 'actuel') {
          referenceBouton.add(cible);
        } else if (!referenceBouton.has(cible)) {
          nouveaux.push(`pastille | ${theme} : ${cible}`);
        }
      }
    }
  }

  await assurerPanierVide(page);
  expect(elementsVerifies, 'axe doit avoir examiné le panneau').toBeGreaterThan(50);
  console.log(`éléments examinés par axe : ${elementsVerifies}`);
  expect(nouveaux, 'défauts de contraste propres à un thème (absents sous « Actuel »)').toEqual([]);
});
