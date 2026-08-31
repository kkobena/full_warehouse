import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tiers payant pèse deux fois sur la trésorerie : il diffère l'encaissement des ventes, et
 * il faut néanmoins régler les achats qui les ont permises.
 *
 * Le rapport met donc les deux côte à côte sur la même période : ce que les organismes ont
 * réglé, et ce que l'officine a acheté. C'est le rapprochement le plus parlant pour qui gère
 * une trésorerie tendue — un mois de fortes ventes assurance peut être un mois de caisse
 * difficile.
 */
scenario('CPT-13', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Rapport d'activité/);
    await saisirDate(page, 'du', debut);
    await saisirDate(page, 'au', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/Total des achats/);
  });

  await etape(3, async () => {
    // Les règlements des organismes, ventilés par tiers payant.
    await expect(contenu).toContainText(/Règlements tiers payants|Tiers-payant/);
  });
});
