import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Supprimer un client n'a rien d'anodin : ses ventes, ses différés et ses avoirs y sont
 * rattachés. C'est pourquoi l'écran offre DEUX gestes distincts, et pas un seul.
 *
 * DÉSACTIVER retire le client des sélections du comptoir sans toucher à son passé — c'est le
 * geste courant, pour un patient qui a déménagé ou un compte qu'on n'ouvre plus.
 *
 * SUPPRIMER efface la fiche, et ne se justifie que sur une saisie erronée du jour même.
 *
 * Parcours en LECTURE : il montre la demande de confirmation sans supprimer un client dont
 * vivent les parcours de vente et de différés.
 */
scenario('CLI-04', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(lignes.first()).toBeVisible();
    // Les deux gestes cohabitent sur la ligne : le cadenas et la corbeille.
    await expect(lignes.first().locator('app-button[ngbtooltip="Désactiver"] button')).toBeVisible();
    await lignes.first().locator('app-button[ngbtooltip="Supprimer"] button').first().click();
  });

  await etape(2, async () => {
    const question = page.locator('.modal-content:visible');
    await expect(question).toContainText(/supprimer/i);
  });
});
