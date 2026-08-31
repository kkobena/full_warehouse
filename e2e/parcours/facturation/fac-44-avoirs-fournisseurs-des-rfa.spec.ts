import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une remise de fin d'année promise ne vaut rien tant qu'elle n'est pas arrivée. Elle se
 * matérialise par un AVOIR du fournisseur, que l'on déduira d'une facture à venir.
 *
 * Le second onglet sert donc à rapprocher : pour chaque palier atteint, l'avoir correspondant
 * est-il reçu, ou encore attendu ? Un avoir « en attente » plusieurs mois après la clôture de
 * l'exercice est une créance qu'on est en train d'oublier.
 */
scenario('FAC-44', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Remises & RFA/);
    await page.getByRole('tab').filter({ hasText: /Avoirs reçus/ }).first().click();
    await expect(contenu).toContainText('N° Avoir');
  });

  await etape(2, async () => {
    // Le statut est ce qu'on vient lire : reçu, ou toujours attendu.
    await expect(contenu).toContainText('Fournisseur');
    await expect(contenu).toContainText('Montant');
    await expect(contenu).toContainText('Statut');
  });
});
