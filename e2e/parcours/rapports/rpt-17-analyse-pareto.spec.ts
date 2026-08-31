import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Le classement ABC répartit ; le Pareto RACONTE. Le tableau ajoute au rang de chaque produit
 * sa contribution et surtout le CUMUL de celles qui le précèdent : on lit ligne à ligne
 * jusqu'à voir la colonne franchir 80 %, et l'on sait combien de références font l'essentiel
 * du chiffre.
 *
 * Le filtre par classe sert à traiter une catégorie à la fois — les A+ pour ne jamais en
 * manquer, les D pour décider de ce qu'on arrête de référencer.
 */
scenario('RPT-17', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Analyse ABC');
    // Le titre de l'écran est un `<h1>` porté par `app-toolbar` ; la carte du tableau
    // porte un `<h5>` de même libellé. On vise le titre d'écran.
    await expect(page.getByRole('heading', { level: 1, name: /Analyse ABC Pareto/ })).toBeVisible();
  });

  await etape(2, async () => {
    // L'onglet ouvert par défaut : c'est celui du Pareto par chiffre d'affaires.
    await page.getByRole('tab', { name: /Par chiffre d'affaires/ }).click();
    await expect(contenu).toContainText(/CA CUMULÉ/i);
  });

  await etape(3, async () => {
    // La classe A+ isolée : les références qui font les six premiers dixièmes du chiffre.
    await choisirDansSelect(page, 'abcClasse', 'A+ — Top 60% du CA');
    await expect(contenu.locator('tbody tr').first()).toBeVisible();
  });

  await etape(4, async () => {
    // La colonne du cumul, qui est tout l'intérêt du Pareto : elle ne redescend jamais.
    await expect(contenu).toContainText(/CONTRIBUTION/i);
    await expect(contenu).toContainText(/CLASSE PARETO/i);
  });
});
