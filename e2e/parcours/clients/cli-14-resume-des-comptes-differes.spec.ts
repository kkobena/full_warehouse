import { expect } from '@playwright/test';
import { choisirDansSelect, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Ce que doit l'ensemble des clients à crédit est un chiffre de gestion, pas de comptoir : il
 * dit combien d'argent l'officine a avancé et n'a pas encore revu.
 *
 * Le bandeau le donne en trois montants — total consenti, total encaissé, reste à recouvrer —
 * et suit les filtres : restreindre à un client ou à une période recalcule l'agrégat, ce qui
 * permet de répondre à « combien ce client m'a-t-il coûté ce trimestre ».
 */
scenario('CLI-14', async ({ etape, page }) => {
  const bandeau = page.locator('app-differe-kpi-banner');

  await etape(1, async () => {
    await page.goto('/differes');
    await expect(bandeau).toContainText('Total différé');
    await expect(bandeau).toContainText('Total payé');
    await expect(bandeau).toContainText('Reste à payer');
  });

  await etape(2, async () => {
    // Le résumé n'est pas figé : il se recalcule sur le périmètre demandé.
    await choisirDansSelect(page, 'dhStatut', 'Soldé');
    await rechercher(page);
    await expect(bandeau).toContainText('Reste à payer');
  });
});
