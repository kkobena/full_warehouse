import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Un client d'officine ne résilie rien : il cesse simplement de venir, et personne ne le
 * remarque. C'est la perte la plus silencieuse du commerce de proximité.
 *
 * Le rapport la rend visible en classant chaque client par l'ancienneté de son dernier achat :
 * ACTIF sous trente jours, À RISQUE entre un et trois mois, PERDU au-delà. La colonne
 * « absence » donne le nombre de jours, celle du CA total ce que la perte coûterait.
 *
 * Les clients à risque sont ceux sur lesquels un appel change encore quelque chose.
 */
scenario('RPT-09', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Rétention Clients');
    await expect(contenu).toContainText(/CLIENTS ACTIFS/i, { timeout: 20000 });
  });

  await etape(2, async () => {
    // Les trois âges, et leur part : c'est la comparaison des périodes successives d'achat.
    await expect(contenu).toContainText(/À RISQUE/i);
    await expect(contenu).toContainText(/PERDUS/i);
    // Le détail nomme les clients concernés : un taux sans liste ne se traite pas.
    await expect(page.getByRole('heading', { name: /Répartition par segment/ })).toBeVisible();
    await expect(contenu.locator('tbody tr')).not.toHaveCount(0);
  });
});
