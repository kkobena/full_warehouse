import { expect } from '@playwright/test';
import { saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Un rapport qui ne débouche sur rien se lit une fois. Celui-ci débouche.
 *
 * Le périmètre qu'on vient de construire à coups de filtres — les produits qui se vendent sous
 * leur prix d'achat, ceux dont le stock est sous le seuil, ceux qui n'ont rien vendu depuis
 * trois mois — est exactement la liste qu'on voudrait recompter ou recommander. Le menu
 * « Actions » l'envoie telle quelle vers un nouvel inventaire ou vers une suggestion de
 * réapprovisionnement.
 *
 * Sans cela, il faudrait ressaisir la liste ailleurs, et l'on ne le ferait pas.
 *
 * Parcours en LECTURE de l'action : la déclencher créerait un inventaire dans le jeu de
 * démonstration, ce que les parcours du module Stock font déjà de leur côté.
 */
scenario('RPT-35', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirRapport(page, 'stock', 'Récap Produits Vendus/Invendus');
    await expect(page.getByRole('heading', { name: /Récapitulatif Produits/ })).toBeVisible();

    const fin = new Date();
    const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);
    await saisirDate(page, 'startDate', debut);
    await saisirDate(page, 'endDate', fin);
    await page.getByRole('button', { name: 'Rechercher' }).click();
    await expect(page.locator('#main-content').locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Le chevron ouvre les trois destinations possibles du périmètre filtré.
    await page.getByRole('button', { name: 'Autres actions' }).first().click();
    const menu = page.locator('.dropdown-menu.show').first();
    await expect(menu).toContainText('Créer Inventaire');
    await expect(menu).toContainText(/Créer suggestion/);
  });
});
