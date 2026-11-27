import { expect, test } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le bilan d'une promotion : ce qu'elle a coûté, ce qu'elle a fait vendre, et la marge de ses articles
 * comparée à la période précédente de même durée. Il se lit dans le détail de la promotion dès son
 * premier jour, et s'exporte en CSV.
 *
 * Point de départ posé en base (`scripts/essai-promotion/preparer.sql`). Le parcours n'encaisse pas :
 * une vente clôturée garderait la promotion d'essai pour toujours (le nettoyage refuse, à dessein, de
 * réécrire l'historique des ventes). Les montants sont vérifiés par l'essai d'intégration.
 */
const LIBELLE = 'ESSAI E2E Pansements -20 %';

test.beforeAll(() => rejouer('scripts/essai-promotion/preparer.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-31', async ({ etape, page }) => {
  const bilan = page.locator('app-detail-section', { hasText: 'Bilan' });

  await etape(1, async () => {
    await page.goto('/promotions');
    const ligne = page.locator('tbody tr', { hasText: LIBELLE });
    await ligne.getByRole('button', { name: 'Voir les articles' }).click();
    await expect(bilan).toBeVisible();
  });

  await etape(2, async () => {
    await expect(bilan).toContainText("Coût de l'opération");
    await expect(bilan).toContainText('Marge');
    // En cours depuis hier : deux jours, comparés aux deux jours d'avant.
    await expect(bilan).toContainText('même durée, juste avant');
  });

  await etape(3, async () => {
    const telechargement = page.waitForEvent('download');
    await bilan.getByRole('button', { name: 'Exporter' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/^bilan-.*\.csv$/);
    const contenu = readFileSync(await fichier.path(), 'utf-8');
    expect(contenu).toContain('Unités vendues;Unités avant');
    expect(contenu).toContain('PANSEMENT ADHESIF B/20');
  });
});
