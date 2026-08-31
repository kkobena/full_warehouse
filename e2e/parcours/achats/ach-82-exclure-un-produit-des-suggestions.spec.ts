import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un produit en rupture chez tous les grossistes, un article qu'on arrête, une référence dont
 * le fournisseur change : le calcul continue pourtant d'en proposer, et la suggestion revient
 * chaque semaine.
 *
 * L'exclusion la fait taire — mais TEMPORAIREMENT, et c'est tout l'intérêt : elle porte une
 * durée et un motif. À l'échéance, le produit revient de lui-même dans les suggestions, sans
 * que personne ait à s'en souvenir. Une exclusion définitive, elle, se serait oubliée et le
 * produit ne serait jamais recommandé.
 *
 * Le motif n'est pas décoratif : trois mois plus tard, c'est lui qui dit s'il faut réintégrer
 * ou prolonger.
 *
 * Parcours en LECTURE : il montre les exclusions actives et le geste de réintégration, sans
 * modifier les suggestions dont vivent les autres parcours d'achats.
 */
scenario('ACH-82', async ({ etape, page }) => {
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/semois/suggestions');
    await expect(page.getByRole('button', { name: /Exclusions/ })).toBeVisible({ timeout: 20000 });
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: /Exclusions/ }).click();
    await expect(modale).toContainText('Exclusions SEMOIS actives');
  });

  await etape(3, async () => {
    // Ce qui rend l'exclusion relisable : sa durée, ce qu'il en reste, et pourquoi elle a
    // été posée.
    await expect(modale).toContainText('Durée');
    await expect(modale).toContainText('Jours restants');
    await expect(modale).toContainText('Motif');
  });

  await etape(4, async () => {
    // La levée anticipée : on n'attend pas l'échéance si le produit revient plus tôt.
    await expect(modale.getByRole('button', { name: 'Réintégrer' }).first()).toBeVisible();
  });
});
