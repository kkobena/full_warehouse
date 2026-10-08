import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les produits qu'on vend toute la journée n'ont pas à être cherchés à chaque fois : une étoile
 * sur la ligne du panier les épingle à la grille des favoris, un second clic les retire.
 *
 * Parcours ÉCRIVANT dans la base, mais qui se remet en état : le favori épinglé est retiré à la fin.
 */
scenario('VTE-64', async ({ etape, page }) => {
  const produit = 'SIROP APPETIT ENFANT 150ML';
  const grille = page.getByRole('region', { name: 'Produits favoris du comptoir' });
  const compteur = grille.getByRole('button', { name: /^Favoris/ });
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await expect(compteur).toContainText('8');

  await etape(1, async () => {
    await chercherProduit(page, 'SIROP APPETIT');
    await ajouterAuPanier(page, '1');
    await expect(ligne).toContainText(produit);
  });

  await etape(2, async () => {
    // L'étoile est vide tant que le produit n'est pas épinglé.
    await ligne.getByRole('button', { name: new RegExp(`Épingler ${produit} aux favoris`) }).click();
  });

  await etape(3, async () => {
    await expect(compteur).toContainText('9');
    await expect(grille).toContainText(produit);
    // L'étoile est maintenant pleine : le même geste retire le favori.
    await expect(ligne.getByRole('button', { name: new RegExp(`Retirer ${produit} des favoris`) })).toBeVisible();
  });

  // Remise en état : le favori est retiré, la vente abandonnée.
  await ligne.getByRole('button', { name: new RegExp(`Retirer ${produit} des favoris`) }).click();
  await expect(compteur).toContainText('8');
  await assurerPanierVide(page);
});
