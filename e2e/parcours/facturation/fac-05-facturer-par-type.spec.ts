import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Assurance et carnet ne se facturent pas de la même façon ni au même rythme : l'assurance
 * règle sur bordereau à échéance, le carnet est un compte client qu'on relance autrement. Le
 * mode « par type » sépare les deux d'un seul geste.
 *
 * La mécanique est celle de la génération unitaire — mêmes bons éligibles, même numérotation —
 * appliquée à tous les tiers payants partageant le type retenu.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-05', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Édition de factures/);
    await choisirDansSelect(page, 'edMode', 'Par type tiers-payant');
    await expect(contenu).toContainText(/Type tiers-payant/);
  });

  await etape(2, async () => {
    // Le type : assurance ou carnet — deux circuits de recouvrement distincts.
    await choisirDansSelect(page, 'edType', 'Assurance');
    await page.keyboard.press('Escape');
  });

  await etape(3, async () => {
    await rechercher(page);
    await expect(page.getByRole('button', { name: 'Éditer' }).first()).toBeVisible();
  });
});
