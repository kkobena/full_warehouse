import { expect } from '@playwright/test';
import { ouvrirOnglet, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Avant toute décision, un état des lieux : ce que la période contient réellement.
 *
 * Trois montants, et leur ordre n'est pas décoratif. Le CA RÉEL sans ordonnance donne le
 * point de départ. La part encaissée en ESPÈCES le restreint — ce qui est passé par carte ou
 * par mobile laisse une trace bancaire. L'assiette à TVA 0 le restreint encore.
 *
 * De ces trois-là découle un maximum ponctionnable, que le PLAFOND PAR VENTE vient borner à
 * son tour : sans lui, une réduction se concentrerait sur quelques ventes et s'y verrait.
 *
 * Rien n'est écrit à cette étape : l'écran ne fait que mesurer.
 */
scenario('DCA-07', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const debut = new Date();
  debut.setMonth(debut.getMonth() - 2, 1);
  const fin = new Date(debut.getFullYear(), debut.getMonth() + 1, 0);

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Ponction');
    await expect(contenu).toContainText("Ponction du chiffre d'affaires");
  });

  await etape(2, async () => {
    // Un mois révolu : on ne ponctionne pas une période encore en cours.
    await saisirDate(page, 'ponction-du', debut);
    await saisirDate(page, 'ponction-au', fin);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Afficher le CA' }).click();
    // Les trois mesures qui bornent l'assiette, de la plus large à la plus étroite.
    await expect(contenu).toContainText(/CA réel/i, { timeout: 20000 });
    await expect(contenu).toContainText(/CA espèce/i);
    await expect(contenu).toContainText(/Assiette TVA 0/i);
  });
});
