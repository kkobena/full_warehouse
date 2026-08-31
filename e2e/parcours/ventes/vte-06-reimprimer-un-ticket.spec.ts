import { expect } from '@playwright/test';
import { ouvrirJournalDuJour } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le ticket se perd, se déchire, s'oublie sur le comptoir — et le client en a besoin pour son
 * assurance ou son employeur. La réimpression part du journal : on retrouve la vente, on
 * relance le ticket. Rien n'est recalculé, rien n'est réencaissé : c'est le même document.
 *
 * Parcours en LECTURE : réimprimer ne modifie pas la vente.
 */
scenario('VTE-06', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const venteServie = lignes.filter({ hasNotText: 'Annulée' }).first();

  await etape(1, async () => {
    await ouvrirJournalDuJour(page);
    // Le journal est l'entrée : type de vente, caissier, période. C'est là qu'on retrouve la
    // vente d'un client qui revient une heure plus tard.
    await expect(venteServie).toBeVisible();
  });

  await etape(2, async () => {
    await venteServie.getByRole('button', { name: 'Actions' }).click();
    // « Imprimer ticket » est la première entrée du menu : c'est la demande la plus fréquente
    // au comptoir.
    await expect(page.getByRole('button', { name: 'Imprimer ticket' })).toBeVisible();
    await page.getByRole('button', { name: 'Imprimer ticket' }).click();
    // Le ticket part à l'imprimante du poste ; l'écran, lui, ne bouge pas — la vente reste
    // telle quelle dans le journal.
    await expect(venteServie).toBeVisible();
  });
});
