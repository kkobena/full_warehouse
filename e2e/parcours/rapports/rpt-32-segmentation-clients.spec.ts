import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Segmentation RFM : Récence, Fréquence, Montant. Les trois colonnes « R », « F », « M »
 * portent les notes sur 5 dont la classification est déduite — c'est ce qui rend l'écran
 * difficile à lire sans image, et donc digne du manuel.
 */
scenario('RPT-32', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/reports/partners');
    // La colonne « RFM » n'appartient qu'à ce rapport ; le titre, lui, se contenterait d'un
    // écran encore vide.
    await expect(page.getByRole('columnheader', { name: 'RFM', exact: true })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'segClassification', 'Champions');
    // Le filtre promet l'absence des autres segments, pas la présence d'un champion — qui
    // était déjà vraie sur la liste complète, triée par score décroissant.
    await expect(lignes.first()).toContainText(/champion/i);
    await expect(lignes.filter({ hasText: /inactif|à risque/i })).toHaveCount(0);
  });

  await etape(3, async () => {
    // Les indicateurs par client : la dernière date d'achat, le nombre d'achats sur l'année
    // et le panier moyen. On s'ancre sur une valeur de la première ligne plutôt que sur les
    // seuls en-têtes, qui existent même quand le tableau est vide.
    await expect(page.getByRole('columnheader', { name: 'Panier moy.' })).toBeVisible();
    await expect(lignes.first()).toContainText(/\d{2}\/\d{2}\/\d{4}/);
  });
});
