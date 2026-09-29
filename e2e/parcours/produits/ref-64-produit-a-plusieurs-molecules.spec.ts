import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * AUGMENTIN n'est pas « une » molécule : c'est de l'amoxicilline ET de l'acide clavulanique.
 * Le produit porte donc ses deux molécules, dans un ordre — la première est la principale.
 * C'est ce qui permet de le retrouver par l'une OU l'autre, et, demain, de contrôler une
 * interaction portée par la seconde, qu'une DCI unique « AMOXICILLINE/ACIDE CLAVULANIQUE »
 * rendait invisible.
 *
 * Le jeu de démonstration décompose ses deux associations (AUGMENTIN, SERETIDE) en molécules.
 *
 * Parcours en LECTURE : la fiche est ouverte en modification mais jamais enregistrée.
 */
scenario('REF-64', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const selectDci = page.locator('ng-select', { has: page.locator('#f_dci') });

  await etape(1, async () => {
    await page.goto('/produits');
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    // La SECONDE molécule : le filtre doit retrouver l'association, pas seulement les produits
    // dont elle serait la molécule principale.
    await chercherDansSelect(page, 'produitFiltreDci', 'ACIDE CLAVULANIQUE');
    await expect(lignes.first()).toContainText('AUGMENTIN');
    // Chaque présentation n'apparaît qu'une fois, malgré ses deux molécules : le filtre ne
    // duplique pas les lignes. Et seul AUGMENTIN porte l'acide clavulanique dans la démo.
    const libelles = await lignes.locator('td').filter({ hasText: /AUGMENTIN/ }).allInnerTexts();
    expect(libelles.length).toBeGreaterThan(0);
    expect(new Set(libelles).size).toBe(libelles.length);
    for (const texte of await lignes.allInnerTexts()) {
      expect(texte).toContain('AUGMENTIN');
    }
  });

  await etape(2, async () => {
    await lignes.first().click();
    await ouvrirOnglet(page, 'Synthèse');
    // Les molécules jointes par « + », principale en tête.
    await expect(page.locator('app-produit-synthese-tab')).toContainText('AMOXICILLINE + ACIDE CLAVULANIQUE');
  });

  await etape(3, async () => {
    await page.goto('/produits');
    await chercherDansSelect(page, 'produitFiltreDci', 'ACIDE CLAVULANIQUE');
    await lignes.first().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Éditer' }).click();
    // La fiche se remplit en une fois : un libellé posé veut dire que les molécules le sont aussi.
    await expect(page.locator('#f_libelle')).not.toHaveValue('');
    await ouvrirOnglet(page, /Classification/);
    // Les deux molécules, dans leur ordre : c'est la fiche qui fixe la principale.
    await expect(selectDci.locator('.ng-value-label')).toHaveText(['AMOXICILLINE', 'ACIDE CLAVULANIQUE']);
  });
});
