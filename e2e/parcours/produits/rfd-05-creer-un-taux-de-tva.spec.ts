import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le taux de TVA porté par un produit détermine ce que l'officine reverse à l'État et ce
 * qu'elle facture au tiers payant. Une erreur ici ne se rattrape pas d'un clic : elle se
 * propage à toutes les ventes déjà passées.
 *
 * D'où la règle, sévère mais logique : un taux ne se MODIFIE PAS. Le corriger reviendrait à
 * réécrire la fiscalité de ventes déjà déclarées. Une correction passe donc par une
 * suppression suivie d'une création (RFD-06), ce qui oblige à reclasser explicitement les
 * produits concernés.
 *
 * Parcours en LECTURE : il remplit sans enregistrer, un taux surnuméraire s'invitant ensuite
 * dans toutes les fiches produit.
 */
scenario('RFD-05', async ({ etape, page }) => {
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/tva');
    await page.getByRole('button', { name: 'Nouvelle Tva' }).click();
    await expect(modal).toBeVisible();
  });

  await etape(2, async () => {
    // Le taux, et rien d'autre : c'est tout ce qu'un taux de TVA est.
    await modal.locator('#field_taux').fill('9');
  });

  await etape(3, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
