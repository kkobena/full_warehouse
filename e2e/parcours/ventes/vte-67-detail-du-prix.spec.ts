import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * « Pourquoi ce prix ? » : le menu de la ligne ouvre un détail — prix public, remise éventuelle,
 * prix net unitaire. Le raccourci Espace fait de même sur la ligne sélectionnée.
 *
 * Parcours en LECTURE : la vente commencée est abandonnée.
 */
scenario('VTE-67', async ({ etape, page }) => {
  const produit = 'ADVIL 200 mg, comprimé enrobé';
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await chercherProduit(page, 'ADVIL 200', produit);
  await ajouterAuPanier(page, '1');
  await expect(ligne).toContainText(produit);

  await etape(1, async () => {
    await ligne.getByRole('button', { name: 'Autres actions sur la ligne' }).click();
    await expect(page.locator('.dropdown-menu.show')).toContainText('Détail du prix');
  });

  await etape(2, async () => {
    await page.locator('.dropdown-menu.show').getByRole('button', { name: /Détail du prix/ }).click();
    const detail = page.locator('[aria-label="Détail du prix"]');
    await expect(detail).toContainText(produit);
    await expect(detail).toContainText('Prix public');
    await expect(detail).toContainText('Net unitaire');
    await expect(detail).toContainText('2 860');
  });

  await page.keyboard.press('Escape');
  await assurerPanierVide(page);
});
