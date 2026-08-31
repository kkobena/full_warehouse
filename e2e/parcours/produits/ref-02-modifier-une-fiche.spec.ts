import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Modifier une fiche produit ne réécrit PAS le passé : les ventes déjà encaissées gardent le
 * prix qui était le leur au moment où elles ont eu lieu. C'est le point que ce parcours doit
 * établir, parce qu'il rassure sur un geste que l'on hésite souvent à faire — corriger un
 * prix de vente en cours de mois.
 *
 * Le formulaire est le même qu'à la création, servi avec les valeurs existantes ; il est
 * découpé en onglets, et l'on n'a donc à traverser que la section concernée.
 *
 * Parcours ÉCRIVANT dans la base : il rétablit la valeur d'origine.
 */
scenario('REF-02', async ({ etape, page }) => {
  const produit = 'ARNICA';
  const nomCommercial = 'ARNIGEL DEMONSTRATION';
  let valeurOrigine = '';

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first()
      .getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Éditer' }).click();
    // Le titre de la barre dit dans quel sens on travaille : « Modifier », et non « Nouveau ».
    await expect(page.getByRole('heading', { name: /Modifier Produit/i })).toBeVisible();
    // La fiche est servie remplie : le code CIP y est déjà, on ne le ressaisit pas.
    await expect(page.locator('#f_codeCip')).not.toHaveValue('');
  });

  await etape(2, async () => {
    // Le nom commercial est le nom de marque, distinct de la dénomination officielle : c'est
    // celui que le client emploie au comptoir, et celui qu'on corrige le plus souvent.
    valeurOrigine = await page.locator('#f_nomCommercial').inputValue();
    await page.locator('#f_nomCommercial').fill(nomCommercial);
    await expect(page.locator('#f_nomCommercial')).toHaveValue(nomCommercial);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    // La modification vaut pour la suite : les tickets déjà émis, eux, gardent leurs prix.
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first()
      .getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Éditer' }).click();
    await expect(page.locator('#f_nomCommercial')).toHaveValue(nomCommercial);
  });

  // ── Remise en état : la fiche retrouve son nom commercial d'origine. ────────────────────
  await page.locator('#f_nomCommercial').fill(valeurOrigine);
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
});
