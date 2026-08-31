import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un retour se prépare produit par produit, mais il se NÉGOCIE fournisseur par fournisseur :
 * le grossiste vient reprendre un carton, pas trois lignes éparses. La vue groupée rassemble
 * donc tous les retours en attente d'un même fournisseur, avec leur montant total — c'est ce
 * qu'on lui présente, et ce qui deviendra un avoir.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-54', async ({ etape, page }) => {
  const ecran = page.locator('app-retour-fournisseur');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Retours fournisseurs/);
    await expect(ecran).toBeVisible();
    // Quatre vues du même stock de retours : ce qui attend, ce qui est devenu avoir, le
    // regroupement par fournisseur, et l'historique.
    await expect(ecran).toContainText('En attente');
    await expect(ecran).toContainText('Avoirs');
    await expect(ecran).toContainText('Groupé');
    await expect(ecran).toContainText('Historique');
    await ecran.getByRole('tab', { name: /Groupé/ }).click();
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
  });
});
