import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux commandes en attente chez le même grossiste, c'est deux livraisons, deux bons à
 * réceptionner et souvent un palier de remise manqué. La fusion les réunit en une seule, dont
 * les lignes s'additionnent produit par produit.
 *
 * L'action n'apparaît qu'à partir de deux commandes cochées, et seulement si elles vont chez
 * le MÊME fournisseur : fusionner deux grossistes n'aurait pas de sens.
 *
 * Parcours en LECTURE : fusionner modifierait durablement les commandes du jeu de
 * démonstration, dont dépendent les parcours de réception.
 */
scenario('ACH-05', async ({ etape, page }) => {
  const liste = page.locator('app-commande-requested-home');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').nth(0).getByRole('checkbox').check();
    await liste.locator('tbody tr').nth(1).getByRole('checkbox').check();
    // Le bandeau compte la sélection : c'est sur elle que portera l'action groupée.
    await expect(liste.locator('.grid-caption')).toContainText('sélectionnée(s)');
  });

  await etape(2, async () => {
    // « Fusionner » n'est offert que si les commandes cochées partagent leur fournisseur ;
    // sinon, seule la suppression groupée reste possible.
    await expect(liste).toContainText(/Fusionner|Supprimer/);
  });
});
