import { expect, test } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { lire, rejouer } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Une promotion invisible en rayon ne fait rien vendre. L'étiquette « PROMO » porte le prix
 * normal barré, le prix remisé et la date de fin, sur les mêmes formats que les étiquettes de
 * produit (ref-66) : planches A4 ou rouleaux.
 *
 * Point de départ posé en base : une promotion validée et en cours (`preparer.sql`). Parcours
 * ÉCRIVANT : la promotion est supprimée à la fin, et vérifié.
 */
const LIBELLE = 'ESSAI E2E Pansements -20 %';

test.beforeAll(() => rejouer('scripts/essai-promotion/preparer.sql'));
test.afterAll(() => {
  rejouer('scripts/essai-promotion/nettoyer.sql');
  expect(lire("SELECT count(*) FROM promotion WHERE libelle LIKE 'ESSAI E2E%'")).toBe('0');
});

scenario('RFD-25', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/promotions');
    const ligne = page.locator('tbody tr', { hasText: LIBELLE });
    await expect(ligne).toContainText('En cours');
    await ligne.getByRole('button', { name: 'Étiquettes de rayon' }).click();
    await expect(modale).toContainText('Étiquettes de rayon');
    // L'aperçu dit ce que portera chaque étiquette, avant d'imprimer.
    await expect(modale.locator('.et-apercu li')).toContainText('PANSEMENT ADHESIF B/20');
    await expect(modale.locator('.et-prix-avant')).toBeVisible();
  });

  await etape(2, async () => {
    await modale.locator('#promoEtiquetteQty').fill('3');
    await expect(modale).toContainText('3 étiquette(s) en tout');
    const telechargement = page.waitForEvent('download');
    await modale.getByRole('button', { name: 'Imprimer' }).click();
    const fichier = await telechargement;
    // Le nom proposé par le navigateur n'est pas fiable (URL de blob) : c'est le contenu qui
    // prouve l'impression, et un PDF commence par sa signature.
    const contenu = readFileSync(await fichier.path());
    expect(contenu.subarray(0, 4).toString()).toBe('%PDF');
    await expect(modale).toHaveCount(0);
  });
});
