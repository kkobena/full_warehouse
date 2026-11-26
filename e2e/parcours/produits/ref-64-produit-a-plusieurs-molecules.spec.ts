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
  // Les lignes du CATALOGUE, et non tous les `tbody tr` de la page : l'écran porte aussi un
  // panneau de produits favoris, dont les lignes se glisseraient devant celles du catalogue.
  const lignes = page.locator('app-produit-list tbody tr').filter({ visible: true });
  const selectDci = page.locator('ng-select', { has: page.locator('#f_dci') });

  /**
   * Filtre le catalogue sur une molécule et n'en rend la main qu'une fois la liste RECHARGÉE.
   *
   * Deux attentes, pour deux pièges distincts.
   *
   * On attend d'abord que la liste NON filtrée soit peinte. Filtrer avant que sa requête ne
   * soit revenue donnait une liste filtrée correcte… que la réponse initiale, arrivée en
   * retard, écrasait ensuite. La fiche ouverte était alors celle du premier produit du
   * catalogue — ABILIFY, premier dans l'ordre alphabétique — qui ne porte qu'une molécule.
   * Cela ressemblait à un défaut du formulaire alors que la fiche n'était pas la bonne.
   *
   * On attend ensuite la liste filtrée : le helper rend la main dès l'option cliquée, mais le
   * tableau ne se repeint que sur la réponse du serveur.
   */
  const filtrerSurLaMolecule = async (): Promise<void> => {
    await expect(lignes.first()).toBeVisible({ timeout: 20_000 });
    await chercherDansSelect(page, 'produitFiltreDci', 'ACIDE CLAVULANIQUE');
    await expect(lignes.first()).toContainText('AUGMENTIN', { timeout: 20_000 });
  };

  await etape(1, async () => {
    await page.goto('/produits');
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    // La SECONDE molécule : le filtre doit retrouver l'association, pas seulement les produits
    // dont elle serait la molécule principale.
    // La molécule est nommée en entier : le référentiel porte AUSSI une DCI composée
    // « AMOXICILLINE ACIDE CLAVULANIQUE », que ce filtre ne doit pas confondre avec la molécule
    // seule — c'est précisément la décomposition que ce parcours défend.
    await filtrerSurLaMolecule();
    // Chaque présentation n'apparaît qu'une fois, malgré ses deux molécules : le filtre ne
    // duplique pas les lignes. Et seul AUGMENTIN porte l'acide clavulanique dans la démo.
    const libelles = await lignes
      .locator('td')
      .filter({ hasText: /AUGMENTIN/ })
      .allInnerTexts();
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
    await filtrerSurLaMolecule();
    await lignes.first().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Éditer' }).click();
    // C'est bien l'association qui est ouverte : sans ce contrôle, une fiche voisine ne
    // montrerait qu'une molécule et le parcours accuserait le formulaire.
    await expect(page.locator('#f_libelle')).toHaveValue(/AUGMENTIN/);
    await ouvrirOnglet(page, /Classification/);
    // Les deux molécules, dans leur ordre : c'est la fiche qui fixe la principale.
    await expect(selectDci.locator('.ng-value-label')).toHaveText(['AMOXICILLINE', 'ACIDE CLAVULANIQUE']);
  });
});
