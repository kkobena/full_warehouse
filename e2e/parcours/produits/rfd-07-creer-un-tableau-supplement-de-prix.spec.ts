import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le tableau est un SUPPLÉMENT DE PRIX, appliqué à la caisse aux produits qui le portent :
 * un article à 1 550 rattaché à un tableau de valeur 100 se vend 1 650.
 *
 * C'est ainsi que se répercute une taxe, une contribution ou une majoration décidée pour
 * toute une catégorie de produits, sans avoir à retoucher chaque prix de vente un par un —
 * et sans le perdre à la réception suivante, qui recalcule le prix depuis celui du
 * fournisseur augmenté du tableau.
 *
 * Deux informations suffisent donc : un CODE, que les fiches produit référencent, et la
 * VALEUR ajoutée au prix. Ce qui compte ensuite est ce qu'on y range (RFD-08), puisque le
 * supplément suit les produits, pas le tableau.
 *
 * Parcours en LECTURE : il remplit sans enregistrer — un tableau de test appliqué par erreur
 * changerait des prix de vente.
 */
scenario('RFD-07', async ({ etape, page }) => {
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/tableaux');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(modal).toBeVisible();
    await modal.locator('#code').fill('TAXE-SANTE');
    // La valeur est le montant AJOUTÉ au prix de vente, pas un rang.
    await modal.locator('#field_value').fill('100');
  });

  await etape(2, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
