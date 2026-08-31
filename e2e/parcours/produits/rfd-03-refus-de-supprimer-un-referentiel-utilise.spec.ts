import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Supprimer une famille encore portée par trois cents fiches n'est pas une erreur de
 * manipulation : c'est une perte de classement irrattrapable, et les rapports par famille en
 * garderaient la trace pendant des années.
 *
 * Le logiciel compte donc AVANT d'agir, et refuse en disant combien de produits s'y
 * rattachent — le nombre est ce qui permet de décider : trois fiches se reclassent à la main,
 * trois cents non.
 *
 * Le refus vient du serveur, seul à connaître ce compte. L'écran, lui, ne l'affichait pas :
 * l'appel de suppression n'avait aucun gestionnaire d'erreur, la ligne restait en place, et
 * l'on croyait à une suppression silencieuse.
 *
 * Parcours en LECTURE au sens où rien n'est détruit : la suppression est bel et bien tentée,
 * et c'est son refus qui fait la démonstration.
 */
scenario('RFD-03', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/famille-produit');
    await expect(lignes.first()).toBeVisible();

    // Les familles du jeu de démonstration portent toutes des produits : n'importe laquelle
    // fait l'affaire.
    await lignes.first().locator('app-button[ngbtooltip="Supprimer"] button').first().click();
    await page.locator('.modal-content:visible').getByRole('button', { name: 'Oui' }).click();

    // Le refus nomme la cause ET le nombre, sans quoi il n'aiderait pas à décider.
    await expect(
      page.locator('[role="alert"], .toast').filter({ hasText: /encore utilisée par/i }).first(),
    ).toBeVisible({ timeout: 15000 });
  });
});
