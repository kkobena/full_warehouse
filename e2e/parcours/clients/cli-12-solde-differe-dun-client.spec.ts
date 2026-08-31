import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La question se pose au comptoir, un client devant soi : « je peux lui faire crédit ? ». Elle
 * appelle un seul chiffre — ce qu'il doit déjà — et l'écran le met en tête du panneau, avant
 * le détail des ventes.
 *
 * Le solde n'est pas un total de ventes : c'est ce qui reste après les versements. Le panneau
 * l'affiche donc en trois temps — le total à crédit, ce qui a été payé, et le reste — pour
 * qu'un client qui doit beaucoup mais paye régulièrement ne soit pas confondu avec celui qui
 * ne rembourse rien.
 */
scenario('CLI-12', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const panneau = page.locator('.detail-column');

  await etape(1, async () => {
    await page.goto('/differes');
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    await lignes.first().click();
    await expect(panneau).toBeVisible();
    // Les trois chiffres qui font la décision, dans l'ordre où on les lit.
    await expect(panneau).toContainText(/Reste :/);
    await expect(panneau).toContainText(/Total :/);
    await expect(panneau).toContainText(/Payé :/);
    // Et la ventilation vente par vente, qui dit d'où vient le solde.
    await expect(panneau).toContainText('Restant');
  });
});
