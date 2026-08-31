import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La quantité proposée est un calcul, pas une décision : elle ignore la promotion qui arrive,
 * le colisage du grossiste, la trésorerie du mois. On la corrige donc directement dans la
 * grille, à même la cellule.
 *
 * Une quantité corrigée est VERROUILLÉE — un cadenas la marque — et le prochain recalcul ne
 * l'écrasera pas. Deux avertissements accompagnent la saisie : la quantité qui n'est pas un
 * multiple du colisage, et celle qui passe sous le minimum de commande du fournisseur ; ni
 * l'une ni l'autre n'est bloquante, ce sont des choses qui se négocient.
 *
 * Le modèle annonçait un bouton « Réinitialiser » : il existe dans l'écran mais reste masqué,
 * la fonction n'étant pas implémentée côté serveur. Corrigé dans cahier-recette.model.ts.
 *
 * Parcours ÉCRIVANT dans la base : il rétablit la quantité d'origine.
 */
scenario('ACH-13', async ({ etape, page }) => {
  const panneau = page.locator('app-suggestion-produit-panel');
  let quantiteOrigine = '';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    const liste = page.locator('app-suggestion-fournisseur-list');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().click();
    await expect(panneau.locator('.ag-row').first()).toBeVisible();
  });

  await etape(2, async () => {
    // La cellule s'édite sur place : double-clic, saisie, Entrée. C'est le geste d'un
    // tableur, et c'est ce qui permet de reprendre vingt lignes sans quitter le clavier.
    const cellule = panneau.locator('.ag-row').first().locator('[col-id="quantite"]');
    quantiteOrigine = (await cellule.innerText()).replace(/\D/g, '');
    await cellule.dblclick();
    await page.keyboard.type('42');
    await page.keyboard.press('Enter');
    await expect(cellule).toContainText('42');
  });

  // ── Remise en état : la quantité proposée reprend sa valeur. ────────────────────────────
  if (quantiteOrigine) {
    const cellule = panneau.locator('.ag-row').first().locator('[col-id="quantite"]');
    await cellule.dblclick();
    await page.keyboard.type(quantiteOrigine);
    await page.keyboard.press('Enter');
  }
});
