import { expect } from '@playwright/test';
import { ouvrirOnglet, traverserConfirmations } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le prestataire ne vient pas pour un lot : il emporte le bac entier. La destruction se
 * constate donc en bloc, et le récapitulatif reprend les mêmes chiffres que le retrait groupé
 * — lots, unités, valeur d'achat — parce que c'est la même perte, à un mois d'intervalle.
 *
 * « Irréversible » est ici littéral : après ce geste, plus rien ne rappelle que ces produits
 * ont existé, sinon leur trace datée et le procès-verbal (STK-45).
 *
 * Parcours ÉCRIVANT dans la base : il marque plusieurs lots comme détruits.
 */
scenario('STK-44', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await ouvrirOnglet(page, /Lots à détruire/);
    await expect(lignes.first()).toBeVisible();
    // Deux lots non encore détruits.
    const candidats = lignes.filter({ has: page.getByRole('button', { name: 'Détruire ce lot' }) });
    await candidats.nth(0).locator('input[type="checkbox"]').first().check();
    await candidats.nth(1).locator('input[type="checkbox"]').first().check();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Tout retirer' }).click();
    const question = page.locator('.modal-content:visible').first();
    await expect(question).toContainText(/Détruire définitivement/);
    await expect(question).toContainText(/Quantité totale/);
    await expect(question).toContainText(/Valeur achat estimée/);
  });

  await etape(3, async () => {
    await expect(page.locator('.modal-content:visible').first()).toContainText(/irréversible/i);
    await traverserConfirmations(page, { limite: 1 });
  });

  await etape(4, async () => {
    // Les lots détruits restent listés, datés : la traçabilité survit à la destruction.
    await expect(lignes.first()).toBeVisible();
  });
});
