import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Le tiers payant crée une dépendance qui ne se voit pas : l'officine encaisse d'un petit
 * nombre d'organismes, et un seul d'entre eux peut représenter le tiers de sa trésorerie.
 * Tant qu'il paie, personne ne s'en inquiète.
 *
 * L'onglet mesure cette CONCENTRATION — la part de chaque payeur dans le chiffre d'affaires
 * tiers payant. C'est un rapport de risque plus que de performance : il ne dit pas qui
 * rapporte le plus, il dit ce qu'un retard de paiement coûterait.
 */
scenario('RPT-28', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'finance', 'Créances TP');
    await expect(page.getByRole('tab', { name: 'Concentration payeurs' })).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    await page.getByRole('tab', { name: 'Concentration payeurs' }).click();
    // La part de chacun, et non son seul encours : c'est le pourcentage qui dit le risque.
    await expect(contenu).toContainText(/%/);
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });
});
