import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'état de rapprochement est la pièce qu'on envoie à l'assureur avec la relance : facturé,
 * réglé, écart, facture par facture. L'exporter évite d'avoir à le recopier, et fige ce qui
 * était constaté ce jour-là — le dossier de relance ne bouge plus, même si un règlement
 * arrive ensuite.
 *
 * L'export suit le périmètre affiché : le tiers payant retenu, la période, le statut. On
 * envoie à chacun son propre état, et rien d'autre.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-18', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    // Le périmètre : tiers payant, dates, statut.
    await expect(contenu).toContainText(/Tiers-payant/);
    await expect(contenu).toContainText(/Statut/);
    await rechercher(page);
  });

  await etape(2, async () => {
    await expect(page.getByRole('button', { name: 'Excel' })).toBeVisible();
  });
});
