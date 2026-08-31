import { expect } from '@playwright/test';
import { ajouterLigneCommande, chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une commande se construit rarement d'un seul jet : on la commence le matin sur une rupture
 * constatée, on l'enrichit dans la journée. Tant qu'elle n'est pas transmise, elle reste
 * ouverte — chaque ligne ajoutée est enregistrée sur-le-champ, sans bouton « Enregistrer ».
 *
 * C'est ce que le bandeau du haut rend visible : le nombre de lignes et les montants se
 * recalculent à chaque ajout, de sorte qu'on sait ce qu'on engage au fur et à mesure.
 *
 * Parcours ÉCRIVANT dans la base : il ajoute une ligne à une commande en attente, puis la
 * retire — ce que montre aussi ACH-04.
 */
scenario('ACH-02', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 5CH';
  const grille = page.locator('app-commande-requested');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    const liste = page.locator('app-commande-requested-home');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().dblclick();
    await expect(grille).toBeVisible();
  });

  await etape(2, async () => {
    await ajouterLigneCommande(page, produit, '3');
    await expect(grille).toContainText(produit);
  });

  await etape(3, async () => {
    // Rien à enregistrer : la ligne EST dans la commande, et le bandeau compte déjà avec.
    await expect(grille.locator('.grid-caption')).toContainText('ligne(s)');
    await expect(grille.locator('.grid-caption')).toContainText(/Achat/i);
  });

  // ── Remise en état : la ligne ajoutée est retirée de la commande. On la coche dans la
  //    grille, puis on emploie l'action groupée — c'est le chemin qu'illustre ACH-04. ──────
  const ligne = page.locator('.ag-row').filter({ hasText: produit }).first();
  if (await ligne.isVisible().catch(() => false)) {
    await ligne.locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
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
