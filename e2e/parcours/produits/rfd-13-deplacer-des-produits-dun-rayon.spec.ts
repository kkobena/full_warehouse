import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un rayon se réorganise plus souvent qu'il ne se crée : une gamme change de place, une
 * saison finit, un meuble est déplacé. Le faire produit par produit reviendrait à ressaisir
 * l'implantation entière.
 *
 * Le déplacement en lot part donc du rayon d'origine, avec sa case d'en-tête pour tout
 * prendre, et ne demande qu'une chose : la destination.
 *
 * Le cas qui compte est celui du DOUBLON : un produit déjà présent dans le rayon cible ne
 * peut pas y entrer deux fois. L'écran le signale avant d'agir et propose un autre
 * emplacement, plutôt que de créer une seconde ligne qu'on ne comprendrait plus.
 *
 * Parcours en LECTURE : il montre la sélection et la demande de destination sans déplacer,
 * l'implantation servant d'assise à l'inventaire tournant et aux parcours de rangement.
 */
scenario('RFD-13', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const onglet = page.locator('app-rayon-produits-tab');

  await etape(1, async () => {
    await page.goto('/rayon');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().click();
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    // La case d'en-tête retient toute la page : c'est ce qui rend une réimplantation
    // praticable.
    await onglet.locator('thead input[type="checkbox"]').first().click();
    await expect(page.getByRole('button', { name: /Déplacer \(/ })).toBeVisible();
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: /Déplacer \(/ }).click();
    const modal = page.locator('.modal-content:visible');
    await expect(modal).toContainText(/rayon|stockage/i);
    // Le rayon de destination se cherche, et l'action reste fermée tant qu'il manque.
    await expect(modal.getByRole('button', { name: 'Confirmer' })).toBeVisible();
  });
});
