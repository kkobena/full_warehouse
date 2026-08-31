import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tableau du pharmacien est le document que l'officine sort pour son expert-comptable :
 * une ligne par jour (ou par mois), les ventes d'un côté, les achats par grossiste de
 * l'autre, et les deux ratios qui les relient. C'est l'écran le plus large de
 * l'application — d'où l'intérêt d'une image plutôt que d'un paragraphe.
 */
scenario('CPT-06', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Tableau pharmacien/);
    await expect(page.getByText('Tableau de bord pharmacien')).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
    await rechercher(page);
    // Le regroupement par défaut est journalier : sur trois mois, plusieurs lignes de dates
    // distinctes. Une seule ligne signifierait que la période n'a pas été prise en compte.
    await expect(lignes.nth(1)).toBeVisible();
    await expect(lignes.first()).toContainText(/\d{2}\/\d{2}\/\d{4}/);
  });

  await etape(3, async () => {
    // Les deux regroupements sont des boutons radio étiquetés : `getByLabel` les atteint par
    // leur libellé visible, sans avoir à connaître leur identifiant.
    await page.getByLabel('Mensuel').check();
    await rechercher(page);
    // Le mensuel condense : la colonne DATE ne porte plus un jour mais un mois. C'est le
    // seul changement visible, et donc la seule chose à affirmer.
    await expect(lignes.first()).not.toContainText(/\d{2}\/\d{2}\/\d{4}/);
    await expect(lignes.first()).toContainText(/\d{4}/);
  });
});
