import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Compte de résultat analytique : le même chiffre d'affaires décomposé selon deux angles —
 * par type de vente (le segment : comptant, assurance) et par famille de produits. Les deux
 * totaux se recoupent, ce qui n'est évident qu'une fois les deux vues mises côte à côte ;
 * c'est précisément ce qu'un manuel peut montrer et qu'un paragraphe explique mal.
 */
scenario('RPT-29', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/reports/finance');
    // La vue d'entrée est le P&L par type de vente : sa colonne « Taux marge » n'existe pas
    // sur les autres onglets de la famille Finance.
    await expect(page.getByRole('columnheader', { name: /Taux marge/i })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // L'exercice est un choix, pas une période libre : le rapport compare des années
    // pleines. Le millésime en cours est le seul qui porte des ventes dans la démonstration.
    await choisirDansSelect(page, 'pnlExercice', String(new Date().getFullYear()));
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: /Par famille$/ }).click();
    // Le basculement change l'axe d'analyse : la colonne de tête n'est plus le type de
    // vente mais la famille. C'est ce qui distingue les deux vues, dont les autres colonnes
    // sont identiques.
    await expect(page.getByRole('columnheader', { name: 'Famille' })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(4, async () => {
    await page.getByRole('button', { name: /Évolution 12 mois$/ }).click();
    // La vue évolution abandonne l'exercice pour douze mois glissants : le sélecteur
    // d'année disparaît, remplacé par la mention de la période. Le contrôle porte donc sur
    // ce basculement, et non sur un tableau qui existerait dans les deux vues.
    await expect(page.getByText(/12 derniers mois glissants/)).toBeVisible();
    await expect(page.locator('#pnlExercice')).toHaveCount(0);
  });
});
