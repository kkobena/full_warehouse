import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client reprend « une autre boîte ». Plutôt que de repasser par la recherche, le menu de la
 * ligne ajoute une unité (Ctrl+D fait de même sur la ligne sélectionnée) : la quantité et le total
 * de la ligne suivent.
 *
 * Parcours en LECTURE : la vente commencée est abandonnée.
 */
scenario('VTE-66', async ({ etape, page }) => {
  const produit = 'ADVIL 200 mg, comprimé enrobé';
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await chercherProduit(page, 'ADVIL 200', produit);
  await ajouterAuPanier(page, '1');
  await expect(ligne).toContainText(produit);
  await expect(ligne).toContainText('2 860');

  await etape(1, async () => {
    await ligne.getByRole('button', { name: 'Autres actions sur la ligne' }).click();
    const menu = page.locator('.dropdown-menu.show');
    await expect(menu).toContainText('Ajouter une unité');
    await expect(menu).toContainText('Ctrl+D');
  });

  await etape(2, async () => {
    await page.locator('.dropdown-menu.show').getByRole('button', { name: /Ajouter une unité/ }).click();
    // Deux boîtes à 2 860 : 5 720.
    await expect(ligne).toContainText('5 720');
  });

  await assurerPanierVide(page);
});
