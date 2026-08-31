import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rapprochement porte ses propres indicateurs, et ils ne répètent pas ceux du portefeuille
 * de factures (FAC-45) : ici, le regard va à l'ÉCART GLOBAL et au nombre de lignes EN RETARD,
 * c'est-à-dire à ce qui doit être réclamé et à ce qui l'est trop tard.
 *
 * Les quatre chiffres suivent les filtres — un tiers payant, une période, un statut. C'est ce
 * qui permet de préparer un rendez-vous : on filtre sur l'assureur qu'on va voir, et le
 * bandeau donne les trois chiffres de la conversation.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-50', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    await expect(contenu).toContainText('Écart global');
    await expect(contenu).toContainText('En retard');
  });

  await etape(2, async () => {
    // Restreindre à un statut : les impayés, ceux qui font la conversation.
    await choisirDansSelect(page, 'rapr-statut', 'Impayé');
    await page.keyboard.press('Escape');
    await rechercher(page);
  });

  await etape(3, async () => {
    // Les indicateurs suivent le filtre : ils décrivent le périmètre affiché, pas l'année.
    await expect(contenu).toContainText('Total facturé');
    await expect(contenu).toContainText('Total réglé');
    await expect(contenu).toContainText('Taux');
  });
});
