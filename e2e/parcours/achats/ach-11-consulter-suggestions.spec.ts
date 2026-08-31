import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La proposition d'achat est le point de départ du réapprovisionnement : l'application calcule
 * ce qu'il faudrait commander à partir des ventes moyennes (VMM) et des seuils, fournisseur
 * par fournisseur. Rien n'est engagé — ce sont des propositions, à valider, à ajuster ou à
 * rejeter.
 *
 * Deux colonnes portent la décision : le nombre d'URGENTS, qui dit combien de références sont
 * déjà sous leur seuil, et le MONTANT ESTIMÉ, qui dit ce que la commande coûtera. C'est leur
 * rapport qu'on regarde avant d'ouvrir une proposition.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-11', async ({ etape, page }) => {
  const liste = page.locator('app-suggestion-fournisseur-list');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    await expect(liste).toBeVisible();
  });

  await etape(2, async () => {
    // Les colonnes qui font la lecture : qui livre, combien de références, combien d'urgentes,
    // et pour quel montant.
    await expect(liste).toContainText('Fournisseur');
    await expect(liste).toContainText('Produits');
    await expect(liste).toContainText('Urgents');
    await expect(liste).toContainText('Montant estimé');
    await expect(liste).toContainText('Statut');
    // Une ligne RÉELLE, et non le message d'écran vide : `tbody tr` matche aussi la ligne
    // « Aucun fournisseur », et l'assertion passerait sur un écran qui ne montre rien.
    await expect(liste.locator('tbody tr').filter({ hasNotText: 'Aucun fournisseur' }).first())
      .toContainText(/\d/);
  });
});
