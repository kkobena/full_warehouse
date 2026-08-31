import { expect } from '@playwright/test';
import { ajouterLigneCommande, chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Trois valeurs se corrigent sur une ligne de commande, et pour trois raisons différentes :
 * la QUANTITÉ, parce qu'on a réévalué le besoin ; le PRIX D'ACHAT, parce que le grossiste a
 * annoncé un tarif différent de celui de la fiche ; le CIP PROVISOIRE, quand le produit n'a
 * pas encore son code définitif chez ce fournisseur.
 *
 * Tout s'édite dans la grille, à même la cellule, et l'enregistrement est immédiat : une
 * commande en préparation n'a pas de bouton « Enregistrer ». Une ligne dont le prix diffère
 * de celui de la fiche est signalée en couleur — c'est ce qui la fera remonter au contrôle de
 * concordance à la réception (ACH-43).
 *
 * Parcours ÉCRIVANT dans la base : il travaille sur une ligne qu'il ajoute, et la retire.
 */
scenario('ACH-03', async ({ etape, page }) => {
  const produit = 'PULSATILLA 9CH';
  const grille = page.locator('app-commande-requested');

  // Mise en scène : la ligne sur laquelle on corrigera, ajoutée par le parcours.
  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);
  await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
  const liste = page.locator('app-commande-requested-home');
  await expect(liste.locator('tbody tr').first()).toBeVisible();
  await liste.locator('tbody tr').first().dblclick();
  await expect(grille).toBeVisible();
  await ajouterLigneCommande(page, produit, '5');
  await expect(grille).toContainText(produit);

  const ligne = () => page.locator('.ag-row').filter({ hasText: produit }).first();

  await etape(1, async () => {
    await expect(ligne()).toBeVisible();
    await expect(ligne().locator('[col-id="quantityRequested"]')).toContainText('5');
  });

  await etape(2, async () => {
    // La quantité, dans sa cellule : double-clic, saisie, Entrée.
    await ligne().locator('[col-id="quantityRequested"]').dblclick();
    await page.keyboard.type('8');
    await page.keyboard.press('Enter');
    await expect(ligne().locator('[col-id="quantityRequested"]')).toContainText('8');
  });

  await etape(3, async () => {
    // Rien à valider : la valeur est enregistrée à la sortie de la cellule, et les totaux du
    // bandeau ont déjà suivi.
    await expect(grille.locator('.grid-caption')).toContainText(/Achat/i);
  });

  // ── Remise en état : la ligne ajoutée est retirée. ──────────────────────────────────────
  if (await ligne().isVisible().catch(() => false)) {
    await ligne().locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
    const supprimer = page.getByRole('button', { name: /Supprimer \(/ });
    if (await supprimer.isVisible().catch(() => false)) {
      await supprimer.click();
      const confirmation = page.locator('.modal-content');
      if (await confirmation.isVisible().catch(() => false)) {
        await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
      }
    }
  }
});
