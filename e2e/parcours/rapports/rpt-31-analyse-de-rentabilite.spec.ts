import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Le chiffre d'affaires flatte, la marge paie. Le rapport classe les références par ce
 * qu'elles RAPPORTENT réellement, et non par ce qu'elles encaissent.
 *
 * Le filtre « faible marge » est le geste utile de l'écran : il isole d'un clic les produits
 * sous 10 % de taux de marge. Beaucoup s'y trouvent pour de bonnes raisons — un prix
 * réglementé, un produit d'appel — mais chacun devrait pouvoir être justifié, et c'est
 * rarement le cas de tous.
 *
 * La colonne ROTATION/AN, à côté, empêche la mauvaise conclusion : une petite marge sur un
 * produit qui tourne vingt fois l'an vaut mieux qu'une grosse marge sur un produit qui dort.
 */
scenario('RPT-31', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'finance', 'Analyse de Rentabilité');
    await expect(page.getByRole('heading', { name: /Analyse de Rentabilité des Produits/ })).toBeVisible();
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/Famille de produit/i);
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: /Faible marge/ }).click();
    await expect(contenu).toContainText(/marge insuffisante|Faible marge/i);
  });

  await etape(4, async () => {
    await expect(contenu).toContainText(/TAUX DE MARGE MOYEN/i);
    await expect(contenu).toContainText(/MARGE BRUTE GLOBALE/i);
    // La rotation à côté du taux : les deux colonnes qui doivent être lues ensemble.
    await expect(contenu).toContainText(/ROTATION\/AN/i);
  });
});
