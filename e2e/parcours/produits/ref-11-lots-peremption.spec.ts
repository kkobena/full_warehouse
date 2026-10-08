import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le modèle annonçait un onglet « Lots / péremption » : il s'appelle « Stock ». Corrigé dans
 * cahier-recette.model.ts en écrivant ce parcours.
 */
scenario('REF-11', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/produits');
    // Un produit à lots multiples : 06_lots.sql en pose plusieurs par produit, avec des
    // péremptions échelonnées — c'est ce qui rend l'illustration parlante.
    //
    // Le terme cherché désigne un NOM COMMERCIAL, non une molécule : la recherche porte aussi
    // sur la DCI (CustomizedProductRepository.produitPredicate), si bien que « PARACETAMOL »
    // remonte d'abord DOLIPRANE et EFFERALGAN — qui l'ont pour DCI — et que l'assertion sur la
    // première ligne échouait. Chercher par molécule est un besoin réel de l'officine : c'est
    // l'attente qu'il fallait corriger, pas la recherche.
    await chercherAuCatalogue(page, 'AUGMENTIN');
    await page.locator('tbody tr').first().click();
    await expect(page.getByRole('tab', { name: 'Synthèse' })).toBeVisible();
  });

  await etape(2, async () => {
    await ouvrirOnglet(page, 'Stock');
    // L'en-tête FEFO est la marque de cet onglet : elle prouve qu'on regarde les lots, et
    // non la synthèse restée affichée.
    await expect(page.getByText(/Premier expirant, premier sorti/i)).toBeVisible();

    // Au moins un numéro de lot réel. La liste n'est pas un tableau — « PÉREMPTION » n'y est
    // qu'un span de légende, sans rôle d'en-tête — donc on s'ancre sur la donnée elle-même :
    // le format « L » + AAMM + 4 chiffres posé par 06_lots.sql.
    await expect(page.locator('#main-content')).toContainText(/L\d{8}/);
  });
});
