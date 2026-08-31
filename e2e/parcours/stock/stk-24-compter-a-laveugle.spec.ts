import { expect } from '@playwright/test';
import { seConnecterEnTantQue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Compter en voyant la quantité attendue, c'est se convaincre de la trouver : l'œil valide le
 * chiffre affiché plutôt que de compter le rayon. Le mode « à l'aveugle » retire donc le stock
 * théorique de la grille — celui qui compte ne voit que le produit, son lot, et la case à
 * remplir.
 *
 * Ce n'est pas une option d'écran mais un PRIVILÈGE : voir le stock pendant l'inventaire
 * (`PR_VOIR_STOCK_INVENTAIRE`) se donne au responsable, pas à qui compte. Un même inventaire
 * se présente donc différemment selon qui l'ouvre — et l'aveugle n'est pas contournable en
 * cliquant ailleurs.
 *
 * L'écart, qui trahirait le stock théorique par soustraction, disparaît avec lui.
 *
 * Parcours en LECTURE, sous un autre compte que l'administrateur.
 */
scenario('STK-24', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const grille = page.locator('app-inventory-lot-grid, app-inventory-grid').first();

  await etape(1, async () => {
    // Le responsable stock : il ouvre les inventaires, mais n'a pas le privilège de voir
    // le stock théorique pendant le comptage.
    await seConnecterEnTantQue(page, 'rkouassi', 'admin');
    await page.goto('/inventaire');
    await expect(lignes.first()).toContainText(/en cours|créé/i);
  });

  await etape(2, async () => {
    await lignes.first().getByRole('button', { name: 'Ouvrir' }).click();
    await expect(grille.locator('.ag-row').first()).toBeVisible();
    // La case à remplir est là ; le stock attendu et l'écart n'y sont pas.
    await expect(grille).toContainText('Qté constatée');
    await expect(grille).not.toContainText('Stock initial');
    await expect(grille).not.toContainText('Écart');
  });
});
