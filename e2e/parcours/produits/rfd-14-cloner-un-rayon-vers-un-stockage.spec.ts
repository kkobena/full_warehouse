import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Une officine qui ouvre un dépôt, ou qui range enfin sa réserve, veut y retrouver
 * l'organisation qu'elle connaît. Tout ressaisir — les rayons puis, pour chacun, ses
 * centaines de produits — condamne le projet avant qu'il ne commence.
 *
 * Le clonage répond à deux besoins distincts, et l'écran les sépare :
 *
 *   • cloner les RAYONS eux-mêmes, depuis la liste : la structure, vide ;
 *   • cloner les AFFECTATIONS d'un rayon, depuis sa fiche : son contenu, vers le rayon de
 *     même nom d'un autre stockage.
 *
 * Parcours en LECTURE : il ouvre les deux chemins sans cloner, une duplication touchant des
 * milliers de lignes.
 */
scenario('RFD-14', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/rayon');
    await expect(lignes.first()).toBeVisible();
    // Depuis la liste : la structure des rayons.
    await page.getByRole('button', { name: 'Cloner' }).first().click();
    await expect(modal).toBeVisible();
  });

  await etape(2, async () => {
    // La cible se dit en deux temps — le magasin, puis l'emplacement à l'intérieur : une
    // officine et son dépôt sont deux magasins, chacun avec ses stockages.
    await expect(modal).toContainText(/Magasin destination/i);
    await expect(modal).toContainText(/Emplacement destination/i);
    await modal.getByRole('button', { name: 'Annuler' }).click();
  });

  await etape(3, async () => {
    // Depuis la fiche d'un rayon : son contenu.
    await lignes.first().click();
    await page.getByRole('button', { name: 'Cloner vers...' }).click();
    await expect(page.locator('.modal-content:visible')).toContainText(/stockage|emplacement|destination/i);
  });
});
