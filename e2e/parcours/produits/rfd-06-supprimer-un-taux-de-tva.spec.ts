import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un taux de TVA ne se modifie pas (RFD-05) : une correction passe donc obligatoirement par
 * sa suppression, puis par la création du taux juste.
 *
 * Ce détour n'est pas une lourdeur : il oblige à reclasser explicitement les produits qui
 * portaient l'ancien taux. Supprimer un taux encore utilisé est d'ailleurs refusé — l'écran
 * dit combien de fiches s'y rattachent — parce qu'un produit sans taux ne se vend plus.
 *
 * Parcours en LECTURE : il montre la demande de confirmation sans supprimer, tous les taux du
 * jeu de démonstration étant portés par des produits.
 */
scenario('RFD-06', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/tva');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('app-button[ngbtooltip="Supprimer"] button').first().click();
  });

  await etape(2, async () => {
    const question = page.locator('.modal-content:visible');
    await expect(question).toContainText(/supprimer cet enregistrement/i);
  });
});
