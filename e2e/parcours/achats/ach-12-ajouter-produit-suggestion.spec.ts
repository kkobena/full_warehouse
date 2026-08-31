import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le calcul ne sait pas tout : une rupture constatée au comptoir, une promotion à venir, une
 * commande d'un client — autant de raisons d'ajouter un produit que la VMM n'a pas proposé.
 * La proposition reste donc ouverte à la main, avec le même geste que la commande : chercher
 * le produit, donner la quantité.
 *
 * La ligne ajoutée est marquée comme SAISIE MANUELLEMENT : le prochain recalcul ne l'écrasera
 * pas, ce qui est exactement ce qu'on attend d'un ajout délibéré.
 *
 * Parcours ÉCRIVANT dans la base : il retire la ligne qu'il a ajoutée.
 */
scenario('ACH-12', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 9CH';
  const panneau = page.locator('app-suggestion-produit-panel');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    const liste = page.locator('app-suggestion-fournisseur-list');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().click();
    await expect(panneau).toBeVisible();
  });

  await etape(2, async () => {
    await chercherDansSelect(page, 'produitbox', produit, produit);
    await panneau.locator('input[placeholder="Qté"]').fill('6');
    await panneau.locator('input[placeholder="Qté"]').press('Enter');
    await expect(panneau).toContainText(produit);
  });

  // ── Remise en état : la ligne ajoutée est retirée de la proposition. ────────────────────
  const ligne = page.locator('.ag-row').filter({ hasText: produit }).first();
  if (await ligne.isVisible().catch(() => false)) {
    await ligne.locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
    const supprimer = panneau.getByRole('button', { name: 'Supprimer' });
    if (await supprimer.isEnabled().catch(() => false)) {
      await supprimer.click();
      const confirmation = page.locator('.modal-content');
      if (await confirmation.isVisible().catch(() => false)) {
        await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
      }
    }
  }
});
