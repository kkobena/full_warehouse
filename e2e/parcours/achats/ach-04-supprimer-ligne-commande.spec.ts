import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une ligne ajoutée par erreur, un produit finalement disponible : la commande se corrige tant
 * qu'elle n'est pas transmise. On coche la ou les lignes, et l'action groupée les retire —
 * les montants du bandeau se recalculent aussitôt.
 *
 * Une commande ENTIÈRE se supprime de la même façon, depuis la liste. Après transmission, en
 * revanche, la commande se verrouille : ce qui est parti chez le grossiste ne s'efface pas
 * d'un clic (ACH-28).
 *
 * Parcours ÉCRIVANT dans la base : il ajoute la ligne qu'il supprime, et laisse donc la
 * commande telle qu'il l'a trouvée.
 */
scenario('ACH-04', async ({ etape, page }) => {
  const produit = 'BELLADONNA 15CH';
  const grille = page.locator('app-commande-requested');

  // Mise en scène : la ligne à supprimer, ajoutée par le parcours lui-même.
  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);
  await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
  const liste = page.locator('app-commande-requested-home');
  await expect(liste.locator('tbody tr').first()).toBeVisible();
  await liste.locator('tbody tr').first().dblclick();
  await expect(grille).toBeVisible();
  await chercherDansSelect(page, 'produitbox', produit, produit);
  await page.locator('input[placeholder="Qté"]').fill('2');
  await page.locator('input[placeholder="Qté"]').press('Enter');
  await expect(grille).toContainText(produit);

  await etape(1, async () => {
    // La sélection se fait dans la grille : plusieurs lignes peuvent partir d'un coup.
    const ligne = page.locator('.ag-row').filter({ hasText: produit }).first();
    await ligne.locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
    await expect(page.getByRole('button', { name: /Supprimer \(/ })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: /Supprimer \(/ }).click();
    const confirmation = page.locator('.modal-content');
    if (await confirmation.isVisible().catch(() => false)) {
      await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
    }
    // La ligne a quitté la commande, et le bandeau ne la compte plus.
    await expect(page.locator('.ag-row').filter({ hasText: produit })).toHaveCount(0);
  });
});
