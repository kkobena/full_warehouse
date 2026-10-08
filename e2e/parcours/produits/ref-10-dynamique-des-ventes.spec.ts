import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un produit ne se juge pas sur un total mais sur une FORME : vend-il régulièrement, par
 * à-coups, ou plus du tout depuis trois mois ? Le graphique répond en un coup d'œil là où le
 * tableau demande une lecture ligne à ligne — et c'est cette forme qui décide du
 * réapprovisionnement, du déréférencement ou d'une mise en avant.
 *
 * Les raccourcis de période sont les mêmes sur les trois onglets chiffrés (ventes, achats,
 * mouvements) : aujourd'hui, hier, 7 jours, ce mois, 3 mois, 1 an. Les dates restent
 * modifiables à la main pour une période qui ne tombe dans aucun de ces cas.
 *
 * Parcours en LECTURE.
 */
scenario('REF-10', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500 mg';
  const onglet = page.locator('app-produit-ventes-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Ventes');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    await onglet.getByRole('button', { name: '1 an' }).click();
    // La période choisie recharge la liste d'elle-même : plus de bouton Actualiser.
    await expect(onglet.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    await onglet.locator('app-button[ngbtooltip="Afficher/masquer le graphique"] button').click();
    await expect(onglet.locator('canvas')).toBeVisible();
  });
});
