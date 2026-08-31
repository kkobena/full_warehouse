import { expect } from '@playwright/test';
import { cocherDansMultiSelect, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le journal s'ouvre sur la JOURNÉE en cours — trois ventes dans le jeu de démonstration. Ce
 * choix est bon pour un comptoir et trompeur pour un manuel : il donne à croire que l'écran
 * ne sait montrer qu'un jour. Le parcours élargit donc à la période, ce qui est aussi le
 * premier geste d'un pharmacien qui cherche une vente passée.
 */
scenario('VTE-17', async ({ etape, page }) => {
  const aujourdhui = new Date();
  const debutDuMois = new Date(aujourdhui.getFullYear(), aujourdhui.getMonth(), 1);
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/sales-home/gestion');
    await expect(page.getByRole('columnheader', { name: 'Référence' })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'fromDate', debutDuMois);
    await saisirDate(page, 'toDate', aujourdhui);
    await cocherDansMultiSelect(page, 'jvTypeVente', 'Assurance');
    await rechercher(page);

    // Ce que le filtre promet est une ABSENCE : plus aucune vente au comptant. L'affirmer
    // sur la seule première ligne ne prouverait rien — le journal est trié par date, et une
    // vente assurance s'y trouve déjà en tête avant tout filtrage.
    await expect(lignes.first()).toContainText(/ASSURANCE/i);
    await expect(lignes.filter({ hasText: /COMPTANT/i })).toHaveCount(0);
  });
});
