import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un produit ne porte pas UN stock mais deux, et les confondre fausse toute décision : le
 * RAYON est ce qui est à portée de main au comptoir, la RÉSERVE ce qui attend derrière. Un
 * rayon vide avec une réserve pleine n'est pas une rupture — c'est un réassort interne à
 * faire, et l'écran le dit en montrant les deux côte à côte, chacun avec son seuil, son stock
 * maxi et son niveau de remplissage.
 *
 * Parcours en LECTURE.
 */
scenario('REF-48', async ({ etape, page }) => {
  // Cas NOMMÉ : `07_stock.sql` garantit une réserve à ce produit précis, et
  // `99_verification.sql` échoue au chargement s'il n'en a pas. Sans cela, la réserve
  // dépendrait d'un « un produit sur cinq » calé sur l'ordre d'insertion, et la fiche
  // illustrée changerait d'un chargement à l'autre.
  const produit = 'ARNICA MONTANA 9CH';
  const onglet = page.locator('app-produit-stock-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Stock');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    // Les deux cartes, nommées, chacune avec ses paramètres propres : le seuil du rayon
    // n'est pas celui de la réserve, et c'est ce qui permet de les piloter séparément.
    await expect(onglet).toContainText('Rayon');
    await expect(onglet).toContainText('Réserve');
    await expect(onglet.getByText('Seuil mini').first()).toBeVisible();
    await expect(onglet).toContainText('Stock maxi');
  });
});
