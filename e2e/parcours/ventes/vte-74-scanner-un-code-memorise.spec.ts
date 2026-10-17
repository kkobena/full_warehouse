import { expect } from '@playwright/test';
import { assurerPanierVide } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le gain du chantier des codes alternatifs ne se voit pas à la réception, où l'on trouve
 * normal de trancher une fois : il se voit ICI. La boîte conditionnée localement porte un
 * EAN-13 que la fiche fournisseur ignore ; il a été mémorisé une fois, au déballage du
 * carton, et il ouvre maintenant la vente comme n'importe quel code du catalogue.
 *
 * Ce que le parcours démontre au fond : la recherche du comptoir a QUATRE sources — les deux
 * codes de la fiche fournisseur, l'EAN laboratoire, et les codes alternatifs —, et un code
 * alternatif y pèse autant qu'un code de la fiche. L'article remonte en tête, pas en queue de
 * liste.
 *
 * Le code employé est un cas nommé du jeu de démonstration : 6181234567898, l'EAN d'une boîte
 * conditionnée en Côte d'Ivoire — préfixe GS1 618, clé de contrôle calculée.
 *
 * Parcours en LECTURE : il retient un article sans l'ajouter au panier, donc sans créer de
 * vente en cours.
 */
const CODE_BOITE = '6181234567898';

scenario('VTE-74', async ({ etape, page }) => {
  const produit = 'EFFERALGAN';

  // Le code doit exister en base AVANT d'ouvrir l'écran : sans lui, l'échec ressemblerait à
  // une recherche cassée alors que c'est le jeu de démonstration qui serait incomplet.
  const memorise = lire(`SELECT count(*) FROM produit_code_alias WHERE code = '${CODE_BOITE}'`);
  expect(
    Number(memorise),
    `Le code ${CODE_BOITE} n'est pas mémorisé : recharger 27_codes_alias_datamatrix.sql.`,
  ).toBeGreaterThan(0);

  await etape(1, async () => {
    await assurerPanierVide(page);
    await expect(page.locator('#produitbox')).toBeVisible();
  });

  await etape(2, async () => {
    // Le code tel qu'il est imprimé sur la boîte — ni le CIP du grossiste, ni l'EAN
    // laboratoire : rien, dans la fiche, ne le porte.
    await page.locator('#produitbox').fill(CODE_BOITE);
    await expect(page.locator('.ng-option').first()).toContainText(produit);
  });

  await etape(3, async () => {
    await page.locator('.ng-option').first().click();
    // Le prix affiché est la preuve que l'article est RETENU, et non seulement trouvé : la
    // vente peut continuer comme après n'importe quel scan.
    await expect(page.locator('#main-content')).toContainText(/Prix\s*:\s*[\d\s]*\d/);
    await expect(page.locator('#main-content')).toContainText(produit);
  });

  // Rien à défaire : aucun produit n'a été ajouté au panier.
});
