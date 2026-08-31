import { expect } from '@playwright/test';
import { assurerCaisseOuverte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * On ne ferme pas une caisse en annonçant un montant : on compte les coupures, et
 * l'application fait l'addition. C'est ce qui rend le comptage opposable — le caissier
 * déclare ce qu'il a en main, pas ce qu'il devrait avoir.
 *
 * Parcours ÉCRIVANT dans la base : il ouvre la caisse si besoin, puis la clôture.
 */
scenario('VTE-37', async ({ etape, page }) => {
  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await page.goto('/my-cash-register');
    await expect(page.getByText(/Ma caisse en cours/i)).toBeVisible();
    await page.getByRole('button', { name: /Fermer la caisse/i }).click();
    // Le billetage est un écran à part entière, et le seul chemin vers la clôture.
    await expect(page.getByText('Billetage de caisse')).toBeVisible();
    await expect(page.locator('#numberOf10Thousand')).toBeVisible();
  });

  await etape(2, async () => {
    // Cinq billets de 10 000 et trois de 1 000 : le total doit se faire tout seul.
    await page.locator('#numberOf10Thousand').fill('5');
    await page.locator('#numberOf1Thousand').fill('3');
    // 5 × 10 000 + 3 × 1 000 = 53 000. C'est le calcul que la légende promet, et il ne
    // dépend d'aucune saisie de total.
    await expect(page.locator('#main-content')).toContainText(/53\s?000/);
  });

  // ── Remise en état, hors étapes : la caisse est effectivement clôturée. ───────────────
  await page.getByRole('button', { name: /Valider le billetage/i }).click();
  const confirmation = page.locator('.modal-content');
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Oui' }).click();
  await expect(page.getByRole('button', { name: 'Ouvrir la caisse' })).toBeVisible();
});
