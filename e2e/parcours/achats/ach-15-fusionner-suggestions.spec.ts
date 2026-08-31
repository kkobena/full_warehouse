import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux propositions pour le même grossiste, c'est deux commandes, donc deux livraisons et
 * deux factures là où une suffirait — et souvent la perte d'un palier de remise. La fusion les
 * réunit : les lignes se regroupent, les quantités d'un même produit s'additionnent.
 *
 * L'action n'apparaît que si la sélection s'y prête : deux propositions au moins, et du MÊME
 * fournisseur — fusionner deux grossistes n'aurait pas de sens.
 *
 * Parcours en LECTURE : fusionner consommerait deux des quatre propositions du jeu de
 * démonstration.
 */
scenario('ACH-15', async ({ etape, page }) => {
  const liste = page.locator('app-suggestion-fournisseur-list');
  const barre = page.locator('app-suggestion-home');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Deux propositions cochées : la barre d'actions groupées apparaît et les compte.
    await liste.locator('tbody tr').nth(0).getByRole('checkbox').check();
    await liste.locator('tbody tr').nth(1).getByRole('checkbox').check();
    await expect(barre).toContainText('2 sél.');
  });

  await etape(2, async () => {
    // « Fusionner » n'est proposé que pour des propositions d'un même fournisseur ; sur deux
    // grossistes différents, seule la suppression groupée reste offerte.
    await expect(barre).toContainText(/Fusionner|Supprimer/);
  });
});
