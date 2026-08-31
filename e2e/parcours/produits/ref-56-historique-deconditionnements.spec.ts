import { expect } from '@playwright/test';
import { chercherAuCatalogue, choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le stock d'une boîte déconditionnable baisse sans qu'aucune vente ne l'explique : c'est un
 * déconditionnement, une boîte ouverte pour servir des unités. L'onglet « Déconditionnés »
 * rend ce mouvement lisible — il montre l'unité rattachée à la boîte, son facteur de
 * conversion, ses prix et son stock.
 *
 * L'onglet n'apparaît QUE sur les boîtes déconditionnables : sur les autres, il n'aurait rien
 * à dire, et sa présence laisserait croire qu'un déconditionnement est possible.
 *
 * Parcours en LECTURE.
 */
scenario('REF-56', async ({ etape, page }) => {
  const boite = 'AMOXICILLINE SIROP';
  const onglet = page.locator('app-produit-deconditions-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await choisirDansSelect(page, 'produitFiltreEtat', 'Déconditionnables');
    await chercherAuCatalogue(page, boite, boite);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await expect(page.getByRole('tab', { name: 'Synthèse' })).toBeVisible();
  });

  await etape(2, async () => {
    await ouvrirOnglet(page, 'Déconditionnés');
    await expect(onglet).toBeVisible();
    // Les colonnes qui expliquent le mouvement : combien d'unités par boîte, à quels prix, et
    // ce qu'il reste en unités.
    await expect(onglet).toContainText('Unités / boîte');
    await expect(onglet).toContainText('Prix achat');
    await expect(onglet).toContainText('Prix vente');
    await expect(onglet).toContainText('Stock');
  });
});
