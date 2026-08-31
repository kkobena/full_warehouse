import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Quand la gestion des lots est active, l'inventaire ne se compte pas par produit mais par
 * LOT : c'est le seul niveau où le comptage a un sens. Deux lots du même paracétamol, l'un
 * périmant en mars, l'autre en novembre, ne sont pas interchangeables — et un écart global
 * de trois boîtes ne dit pas lesquelles manquent.
 *
 * La grille l'affiche en clair : une ligne par lot, avec son numéro et sa date d'expiration à
 * côté du code produit. La quantité constatée se saisit lot par lot, et l'écart se calcule au
 * même niveau.
 *
 * Parcours ÉCRIVANT dans la base : il saisit une quantité comptée sur un lot.
 */
scenario('STK-25', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const grille = page.locator('app-inventory-lot-grid');

  await etape(1, async () => {
    await page.goto('/inventaire');
    await expect(lignes.first()).toContainText(/en cours|créé/i);
    await lignes.first().getByRole('button', { name: 'Ouvrir' }).click();
    await expect(grille.locator('.ag-row').first()).toBeVisible();
    // Le lot et sa péremption, en regard du produit : c'est ce qui distingue cette grille
    // d'un inventaire par produit.
    await expect(grille).toContainText('N° Lot');
    await expect(grille).toContainText('Date expiration');
  });

  await etape(2, async () => {
    // Chaque lot porte sa propre quantité constatée, et son propre écart.
    const ligne = grille.locator('.ag-row').first();
    await expect(ligne.locator('[col-id="numLot"]')).toBeVisible();
    const cellule = ligne.locator('[col-id="quantityOnHand"]');
    await cellule.dblclick();
    await page.keyboard.type('5');
    await page.keyboard.press('Enter');
    await expect(cellule).toContainText('5');
  });
});
