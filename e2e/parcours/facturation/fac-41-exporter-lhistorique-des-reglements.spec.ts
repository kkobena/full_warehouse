import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'historique des règlements sort de l'application pour deux usages : le PDF qu'on joint à
 * une relance — « voici ce que vous nous avez versé, voici ce qui reste » — et l'Excel qu'on
 * rapproche du relevé bancaire.
 *
 * L'export reprend le PÉRIMÈTRE FILTRÉ : période, organisme, recherche libre. C'est ce qui
 * permet de produire l'état d'un seul assureur sans exporter les règlements de tous.
 *
 * Parcours en LECTURE : il montre les deux formats sans produire de fichier.
 */
scenario('FAC-41', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Historique règlements/);
    // Le périmètre se cadre avant : dates et recherche libre.
    await expect(contenu).toContainText(/Recherche libre/);
    await rechercher(page);
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toBeVisible();
  });

  await etape(2, async () => {
    // Deux formats, deux usages : le PDF pour la relance, l'Excel pour le rapprochement.
    // « Imprimer » nomme aussi le reçu de chaque ligne : c'est celui de la BARRE D'OUTILS
    // qu'on vise, celui qui emporte la liste filtrée.
    await expect(page.getByRole('button', { name: 'Imprimer', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excel' })).toBeVisible();
  });
});
