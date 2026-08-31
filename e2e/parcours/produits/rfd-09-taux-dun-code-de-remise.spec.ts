import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une remise commerciale ne s'accorde pas au même taux selon l'origine de la vente : sur une
 * ordonnance, la part du patient est déjà réduite par son organisme, et l'officine rogne
 * moins. D'où DEUX taux par code, et non un seul.
 *
 * Le code n'est pas la remise : il est l'étiquette qu'on pose sur des produits (RFD-10), et
 * c'est cette étiquette qui porte les deux pourcentages. Les changer se répercute aussitôt
 * sur tous les produits qui la portent, sans toucher à une seule fiche.
 *
 * Parcours en LECTURE : il remplit le formulaire sans enregistrer, une grille modifiée
 * changeant le prix de toutes les ventes des parcours suivants.
 */
scenario('RFD-09', async ({ etape, page }) => {
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/remises');
    await ouvrirOnglet(page, /Remises produit/);
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(modal).toBeVisible();
    await modal.locator('#valeur').fill('Grille saisonnière');
  });

  await etape(2, async () => {
    // Les deux taux n'apparaissent qu'une fois le code choisi : ils lui appartiennent.
    //
    // La liste ne propose que les codes ENCORE LIBRES — 6 à 9 dans le jeu de démonstration :
    // un code déjà porteur d'une grille se modifie depuis sa ligne, il ne se réattribue pas.
    await choisirDansSelect(page, 'codeRemise', '6');
    await expect(modal).toContainText(/ventes VNO/);
    await expect(modal).toContainText(/ventes VO/);

    const taux = modal.locator('input[type="number"]');
    await taux.nth(0).fill('15');
    await taux.nth(1).fill('10');
  });

  await etape(3, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
