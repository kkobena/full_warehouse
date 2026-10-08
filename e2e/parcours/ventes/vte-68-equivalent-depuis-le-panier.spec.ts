import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client veut moins cher, ou le princeps manque : depuis la ligne du panier, « Équivalents… »
 * liste les génériques de même molécule avec leur prix, l'écart avec le produit demandé et leur
 * stock. En choisir un remplace le produit sur la ligne.
 *
 * Parcours en LECTURE : la vente commencée est abandonnée.
 */
scenario('VTE-68', async ({ etape, page }) => {
  const produit = 'ADVIL 200 mg, comprimé enrobé';
  const fenetre = page.locator('.modal-content');
  const ligne = page.locator('tbody tr').filter({ visible: true }).first();

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await chercherProduit(page, 'ADVIL 200', produit);
  await ajouterAuPanier(page, '1');
  await expect(ligne).toContainText(produit);

  await etape(1, async () => {
    await ligne.getByRole('button', { name: 'Autres actions sur la ligne' }).click();
    await page.locator('.dropdown-menu.show').getByRole('button', { name: /Équivalents/ }).click();
    await expect(fenetre).toContainText('Équivalents disponibles');
    // Chaque proposition dit ce qu'elle coûte de moins et ce qu'il en reste.
    await expect(fenetre).toContainText('IBUPROFENE');
    await expect(fenetre).toContainText('Générique');
    await expect(fenetre).toContainText('en stock');
  });

  await etape(2, async () => {
    await fenetre.getByRole('button', { name: 'Choisir' }).first().click();
    await expect(fenetre).toBeHidden();
    await expect(ligne).toContainText('IBUPROFENE');
    await expect(ligne).toContainText('1 860');
  });

  await assurerPanierVide(page);
});
