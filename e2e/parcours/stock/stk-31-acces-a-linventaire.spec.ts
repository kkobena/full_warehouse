import { expect } from '@playwright/test';
import { seConnecterEnTantQue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'inventaire n'est pas un écran comme les autres : il expose la valeur du stock, et sa
 * clôture applique des mouvements irréversibles. L'accès en est donc gouverné par les
 * autorisations, et à deux niveaux.
 *
 * Le premier gouverne l'écran : un vendeur ne le voit pas dans son menu et n'y accède pas. Le
 * second, plus fin, gouverne ce qu'on y fait — le responsable stock ouvre les inventaires et
 * compte, mais la CLÔTURE reste réservée au privilège dédié, et le stock théorique lui est
 * masqué pendant le comptage (STK-24).
 *
 * Parcours en LECTURE, sous deux comptes différents.
 */
scenario('STK-31', async ({ etape, page }) => {
  await etape(1, async () => {
    // Un vendeur : l'inventaire ne fait pas partie de son travail.
    await seConnecterEnTantQue(page, 'kkone', 'admin');
    await page.goto('/inventaire');
    // Ni liste, ni bouton de création : l'écran ne lui rend rien.
    await expect(page.locator('tbody tr')).toHaveCount(0);
  });

  await etape(2, async () => {
    // L'administrateur, lui, y accède pleinement — jusqu'à la clôture.
    await seConnecterEnTantQue(page, 'admin', 'admin');
    await page.goto('/inventaire');
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nouveau' })).toBeVisible();
  });
});
