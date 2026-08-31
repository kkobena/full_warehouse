import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un grossiste national livre depuis plusieurs dépôts, et l'officine commande à CELUI qui la
 * dessert. Dupliquer la fiche principale pour chaque agence ferait éclater le suivi : le
 * compte, la remise de fin d'année et les conditions commerciales se négocient avec la
 * maison mère, pas avec le dépôt.
 *
 * L'agence est donc une fiche à part entière — elle a son délai de livraison, ses codes EDI —
 * mais rattachée : elle apparaît regroupée sous son principal, et le compte reste unique.
 *
 * C'est cette distinction qui décide où passer une commande et où lire un solde.
 *
 * Parcours en LECTURE : il ouvre la création d'agence sans enregistrer.
 */
scenario('RFD-19', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/fournisseur');
    await expect(lignes.first()).toBeVisible();
    // La colonne « Agences » compte ce que chaque principal dessert.
    await expect(page.getByRole('columnheader', { name: 'Agences' })).toBeVisible();
  });

  await etape(2, async () => {
    // L'ajout part de la ligne du PRINCIPAL : le rattachement est ainsi implicite, et l'on
    // ne peut pas créer une agence orpheline par distraction.
    await lignes.first().locator('app-button[ngbtooltip="Ajouter une agence"] button').click();
    await expect(modal).toBeVisible();
  });

  await etape(3, async () => {
    // La fiche d'agence ne redemande pas les conditions commerciales : elles sont celles du
    // principal. Elle ne porte que ce qui lui est propre — à commencer par son délai.
    await expect(modal).toContainText(/Fournisseur principal|propre à cette agence/);
    await modal.locator('#field_libelle').fill('AGENCE ABIDJAN SUD');
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeVisible();
  });
});
