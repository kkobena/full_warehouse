import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirLaSessionCaissier } from './_caissier';

/**
 * Le bloc ne montre pas la caisse du jour mais MA session : ce que cet utilisateur-là a encaissé
 * depuis qu'il a ouvert sa caisse. Deux caissiers qui se relaient sur le même poste y lisent
 * chacun ses propres chiffres.
 *
 * La ventilation par mode de règlement est ce que la clôture demandera de justifier. Le carnet
 * et le différé apparaissent à part, car ils n'ont rien mis dans le tiroir ; et « à recouvrer »
 * recense les ventes qui ne sont pas soldées, celles qu'on aurait tort de compter comme
 * encaissées.
 */
scenario('HOME-14', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirLaSessionCaissier(page);
  });

  await etape(2, async () => {
    const bloc = page.locator('.card.data-card', { has: page.getByText('Encaissements — Ma Session') });
    await expect(bloc).toBeVisible();
    await expect(bloc).toContainText(/Total encaissé|Aucune donnée de session/);
  });
});
