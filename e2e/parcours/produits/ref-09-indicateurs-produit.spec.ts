import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le modèle annonçait un onglet « Indicateurs » : il n'existe pas, ces chiffres ouvrent
 * l'onglet Synthèse. Corrigé dans cahier-recette.model.ts en écrivant ce parcours.
 *
 * Six mesures répondent à la seule question qui vaille devant une fiche produit : faut-il en
 * recommander, et combien ? Le stock actuel se lit partout ; ce qui se lit ici et nulle part
 * ailleurs, c'est la CONSOMMATION mensuelle moyenne, la rotation annuelle et la couverture en
 * jours — c'est-à-dire le temps qu'il reste avant la rupture.
 *
 * Parcours en LECTURE.
 */
scenario('REF-09', async ({ etape, page }) => {
  const produit = 'DOLIPRANE';
  const synthese = page.locator('app-produit-synthese-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await expect(page.getByRole('tab', { name: 'Synthèse' })).toBeVisible();
  });

  await etape(2, async () => {
    const carte = synthese.locator('.synthese-section').filter({ hasText: 'INDICATEURS CLÉS' }).first();
    await expect(carte).toBeVisible();
    // Les six mesures, nommées : c'est leur ensemble qui fait la décision de réappro.
    await expect(carte).toContainText('Stock actuel');
    await expect(carte).toContainText('Seuil mini');
    await expect(carte).toContainText('Jours de stock');
    await expect(carte).toContainText('CMM');
    await expect(carte).toContainText('Rotation');
    await expect(carte).toContainText('Marge brute');
  });
});
