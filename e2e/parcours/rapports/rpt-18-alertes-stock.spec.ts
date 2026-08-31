import { expect } from '@playwright/test';
import { cocherDansMultiSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Écran volontairement vide à l'ouverture : les trois compteurs sont calculés, mais le détail
 * n'apparaît qu'une fois un type d'alerte choisi. C'est délibéré — la liste complète des
 * ruptures, alertes et péremptions se compte en centaines de lignes — et c'est exactement ce
 * qu'un manuel doit expliquer, sous peine que l'écran passe pour cassé.
 */
scenario('RPT-18', async ({ etape, page }) => {
  const compteur = (libelle: string) => page.locator('app-kpi-item').filter({ hasText: libelle });
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/reports/stock');
    // `exact` : le titre de l'écran et celui de la carte portent le même mot, et tous deux
    // sont désormais de vrais en-têtes — le premier est un `<h1>`, le second un `<h5>`.
    await expect(page.getByRole('heading', { name: 'Alertes Stock', exact: true })).toBeVisible();
  });

  await etape(2, async () => {
    // Les trois compteurs portent chacun un nombre : c'est ce qui les rend comparables, et
    // c'est tout ce que l'étape promet.
    await expect(compteur('Rupture')).toContainText(/\d/);
    await expect(compteur('Stock bas')).toContainText(/\d/);
    await expect(compteur('Péremption')).toContainText(/\d/);
  });

  await etape(3, async () => {
    // « un ou plusieurs » : deux types cochés, ce que l'image doit montrer — une seule
    // pastille laisserait croire que le filtre est exclusif.
    await cocherDansMultiSelect(page, 'saTypeAlerte', 'Rupture de stock');
    await cocherDansMultiSelect(page, 'saTypeAlerte', 'Proche péremption');
    // Le tableau était vide avant ce choix : une première ligne suffit donc à prouver que le
    // filtre a été appliqué, et l'attente d'`expect` couvre le temps de la requête.
    await expect(lignes.first()).toBeVisible();
  });

  await etape(4, async () => {
    // Le détail annoncé, c'est la ligne produit elle-même : un type d'alerte, un libellé et
    // un code CIP. Le tableau mêle les DEUX types cochés et n'en contient aucun autre — ce
    // qui distingue la liste filtrée aussi bien de la liste complète que d'un filtre unique.
    await expect(page.getByRole('columnheader', { name: 'Code CIP' })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
    // Le tableau ne contient QUE les types cochés : le stock bas, troisième compteur de
    // l'écran, n'y figure sur aucune ligne. C'est ce que le filtre promet, et c'est
    // vérifiable ligne à ligne — le tri étant alphabétique, une page donnée peut ne montrer
    // qu'un seul des deux types cochés sans que le filtre soit en cause.
    await expect(lignes.filter({ hasText: /stock bas/i })).toHaveCount(0);
    for (const texte of await lignes.allInnerTexts()) {
      expect(texte).toMatch(/Rupture|Péremption/i);
    }
  });
});
