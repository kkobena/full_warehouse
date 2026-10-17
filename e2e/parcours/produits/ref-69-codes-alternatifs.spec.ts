import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { executer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * L'onglet qui rend visible ce que la réception a appris. Un code mémorisé au scan — le code
 * du fabricant, l'EAN d'une autre boîte, le code interne d'un grossiste — vit ici : on voit
 * sous quels codes l'article est reconnu, qui l'a posé et quand.
 *
 * Deux choses à ne pas confondre, et que l'écran dit lui-même : ces codes COMPLÈTENT les
 * codes de la fiche fournisseur et l'EAN laboratoire, ils ne les recopient pas. Un code de la
 * fiche reste reconnu sans figurer ici ; l'inverse n'est pas vrai.
 *
 * On peut aussi en ajouter un sans attendre une réception — le cas du carton déjà rangé dont
 * on s'aperçoit que le code ne passe pas. Rien d'autre à renseigner que le code : sa nature
 * est déduite de sa forme.
 *
 * Parcours ÉCRIVANT : il ajoute un code puis le retire, par le même écran. Le code d'essai
 * est en outre supprimé en base à la fin, pour le cas où le retrait n'aboutirait pas.
 */
const CODE_ESSAI = 'E2E-REF69';

scenario('REF-69', async ({ etape, page }) => {
  // Le cas nommé du jeu de démonstration : un code HORS NOMENCLATURE, celui que le grossiste
  // imprime pour son propre usage (27_codes_alias_datamatrix.sql).
  const produit = 'TRAMADOL ARROW L.P. 150 mg';
  const onglet = page.locator('app-produit-codes-alias-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Codes alternatifs');
    await expect(onglet).toBeVisible();
    // Le code mémorisé, sa nature en clair — l'utilisateur n'a pas à connaître la
    // nomenclature GS1 — et de quoi savoir d'où il vient : la date et son auteur.
    const ligne = onglet.locator('.pca__item').filter({ hasText: 'GROS/A-7788' });
    await expect(ligne).toBeVisible();
    await expect(ligne).toContainText('Code interne');
    await expect(ligne).toContainText(/\d{2}\/\d{2}\/\d{4}/);
    // Ce que l'onglet rappelle, et qui évite de croire la liste exhaustive.
    await expect(onglet).toContainText(/restent reconnus sans figurer ici/i);
  });

  await etape(2, async () => {
    await onglet.locator('#pca-nouveau-code').fill(CODE_ESSAI);
    await onglet.getByRole('button', { name: 'Ajouter' }).click();
    // Le code ajouté passe EN TÊTE : la liste se lit du plus récent au plus ancien, ce qui
    // est l'ordre utile quand on vient d'en poser un.
    await expect(onglet.locator('.pca__item').first()).toContainText(CODE_ESSAI);
    // Nature déduite de la forme : alphanumérique, donc code interne. Rien n'a été demandé.
    await expect(onglet.locator('.pca__item').first()).toContainText('Code interne');
  });

  await etape(3, async () => {
    const ligne = onglet.locator('.pca__item').filter({ hasText: CODE_ESSAI });
    await ligne.getByRole('button', { name: 'Retirer ce code' }).click();
    // La confirmation dit exactement ce que le retrait fait, et ce qu'il ne fait pas : la
    // fiche de l'article n'est pas touchée, le code cesse seulement d'être reconnu.
    const confirmation = page.locator('.modal-content').filter({ hasText: /Retirer ce code/ });
    await expect(confirmation).toContainText(/La fiche produit n’est pas modifiée|La fiche produit n'est pas modifiée/);
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    await expect(ligne).toHaveCount(0);
    // Le code mémorisé par la réception, lui, est toujours là : on n'a retiré que le nôtre.
    await expect(onglet.locator('.pca__item').filter({ hasText: 'GROS/A-7788' })).toBeVisible();
  });

  // ── Remise en état : le code d'essai n'appartient qu'à ce parcours.
  executer(`DELETE FROM produit_code_alias WHERE code = '${CODE_ESSAI}'`);
});
