import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le récapitulatif sort en deux formats parce qu'il sert deux publics : le PDF pour la
 * réunion et le dossier, l'Excel pour le comptable qui rapprochera ligne à ligne.
 *
 * Comme partout ailleurs dans la facturation, l'export reprend le périmètre affiché — année,
 * mois, organisme — plutôt que l'historique entier.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-14', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Récapitulatif/);
    // Le périmètre : l'année, le mois, éventuellement un organisme.
    await expect(contenu).toContainText(/Année/);
    await expect(contenu).toContainText(/Mois/);
    await rechercher(page);
  });

  await etape(2, async () => {
    await expect(page.getByRole('button', { name: 'PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Excel' })).toBeVisible();
  });
});
