import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les unités gratuites sont une remise déguisée en marchandise : le grossiste en livre
 * treize et n'en facture douze. Elles ENTRENT en stock — sans elles, l'inventaire serait
 * faux — mais ne comptent pas dans le montant du bon, sans quoi la marge du produit serait
 * sous-évaluée à chaque réception.
 *
 * D'où deux champs distincts sur la même ligne, « Reçu » et « UG », et non un seul total.
 *
 * Parcours ÉCRIVANT dans la base : il saisit une UG sur un bon en cours, que la restauration
 * de l'instantané efface avant la campagne suivante.
 */
scenario('ACH-41', async ({ etape, page }) => {
  const ecran = page.locator('app-reception-sequential');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    // Un bon dont rien n'a encore été compté : c'est là que la saisie commence.
    await ouvrirBonDeReception(page, 'aucun');
    await expect(ecran).toBeVisible();
  });

  await etape(2, async () => {
    // Le champ UG est à côté du champ Reçu, et la tabulation y mène : la saisie des gratuites
    // fait partie du geste de réception, pas d'une correction ultérieure.
    await page.locator('#rh-qty').fill('2');
    await page.locator('#rh-ug').fill('1');
    await expect(page.locator('#rh-ug')).toHaveValue('1');
  });
});
