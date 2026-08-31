import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Commander deux fois le même produit chez le même grossiste, à trois jours d'intervalle, est
 * l'erreur d'inattention la plus banale de l'approvisionnement : la première commande n'est pas
 * encore livrée, rien ne la rappelle, et le stock double.
 *
 * PharmaSmart ne pose pas la question au niveau du FOURNISSEUR — ce serait grossier, on passe
 * plusieurs commandes par semaine au même grossiste — mais au niveau du PRODUIT. Tout produit
 * inscrit dans une commande non réceptionnée porte la pastille « En commande », visible partout
 * où on le rencontre : catalogue, fiche produit, import d'une proposition.
 *
 * La protection va plus loin que l'affichage : le calcul des suggestions ÉCARTE purement et
 * simplement les produits déjà commandés ou reçus récemment (`EtatProduitService.canSuggere`),
 * de sorte qu'un réapprovisionnement automatique ne peut pas redemander ce qui est en route.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-07', async ({ etape, page }) => {
  let produit = '';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    const liste = page.locator('app-commande-requested-home');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().dblclick();
    const grille = page.locator('app-commande-requested');
    await expect(grille.locator('.ag-row').first()).toBeVisible();
    // Le libellé d'un produit de cette commande en cours : c'est lui qu'on retrouvera signalé.
    produit = (await grille.locator('.ag-row').first().locator('[col-id="produitLibelle"]').innerText())
      .replace(/\s+/g, ' ')
      .trim();
    expect(produit.length).toBeGreaterThan(0);
  });

  await etape(2, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    const ligne = page.locator('tbody tr').filter({ visible: true }).filter({ hasText: produit }).first();
    await expect(ligne).toBeVisible();
    // La pastille est posée sur la ligne du catalogue, sans qu'on ait rien à ouvrir.
    await expect(ligne.locator('.etat-commande')).toBeVisible();
  });

  await etape(3, async () => {
    const ligne = page.locator('tbody tr').filter({ visible: true }).filter({ hasText: produit }).first();
    await ligne.locator('.etat-commande').hover();
    await expect(page.locator('.tooltip-inner')).toContainText(/commande en cours/i);
  });
});
