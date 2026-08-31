import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Une fiche client vieillit : un numéro de téléphone change, un contrat d'assurance est
 * renégocié, un taux de prise en charge évolue.
 *
 * La modification rouvre le même formulaire que la création, pré-rempli. Ce qui compte n'est
 * pas le geste mais sa PORTÉE : changer un taux ici ne réécrit pas les ventes passées, qui ont
 * été calculées avec le taux du jour. La fiche décrit la couverture d'aujourd'hui, pas
 * l'historique.
 *
 * Parcours en LECTURE : il ouvre la fiche et montre qu'elle est modifiable, sans enregistrer.
 */
scenario('CLI-03', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('app-button[ngbtooltip="Editer"] button').first().click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // La fiche s'ouvre telle qu'elle est : on corrige, on ne ressaisit pas.
    await expect(modale.locator('#field_lastName')).not.toHaveValue('');
    await modale.locator('#field_phone').fill('0708091012');
  });

  await etape(3, async () => {
    await expect(modale.getByRole('button', { name: /Enregistrer/ }).first()).toBeVisible();
  });
});
