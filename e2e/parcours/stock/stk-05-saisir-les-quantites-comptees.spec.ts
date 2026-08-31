import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Compter, c'est confronter deux chiffres : ce que la base affirme, et ce que l'étagère
 * montre. La saisie se fait dans la seule colonne modifiable de la grille — « Qté constatée »
 * — et l'écart se calcule aussitôt, sous les yeux de celui qui compte.
 *
 * La ligne saisie change de couleur et se marque « Saisi » : dans un inventaire de plusieurs
 * centaines de lignes, comptées à plusieurs et sur plusieurs heures, savoir ce qui a déjà été
 * traité vaut autant que le chiffre lui-même.
 *
 * Rien n'est appliqué au stock à ce stade : la saisie se corrige autant qu'on veut, et seule
 * la clôture (STK-12) transformera les écarts en mouvements.
 *
 * Parcours ÉCRIVANT dans la base : il saisit une quantité comptée sur un inventaire ouvert.
 */
scenario('STK-05', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const grille = page.locator('app-inventory-lot-grid, app-inventory-grid').first();

  await etape(1, async () => {
    await page.goto('/inventaire');
    // L'onglet « En cours » réunit les inventaires ouverts : ceux qui viennent d'être créés
    // comme ceux dont le comptage a commencé.
    await expect(lignes.first()).toContainText(/en cours|créé/i);
    await lignes.first().getByRole('button', { name: 'Ouvrir' }).click();
    await expect(grille.locator('.ag-row').first()).toBeVisible();
    // Les deux chiffres à confronter : le stock initial, et la case où porter le comptage.
    await expect(grille).toContainText('Stock initial');
    await expect(grille).toContainText('Qté constatée');
  });

  await etape(2, async () => {
    // La saisie se fait dans la cellule : double-clic, valeur, Entrée.
    const cellule = grille.locator('.ag-row').first().locator('[col-id="quantityOnHand"]');
    await cellule.dblclick();
    await page.keyboard.type('7');
    await page.keyboard.press('Enter');
    await expect(cellule).toContainText('7');
    // L'écart se calcule seul, et la ligne se marque comme traitée.
    await expect(grille.locator('.ag-row').first().locator('[col-id="gap"]')).toBeVisible();
  });
});
