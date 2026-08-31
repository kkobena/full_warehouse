import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un client doit 7 400 et tend 10 000. La monnaie se calcule de tête, et c'est exactement là
 * qu'on se trompe — d'autant que la somme due n'est presque jamais ronde.
 *
 * L'écran la pose donc lui-même, en haut du panneau, dès la saisie et avant toute validation.
 * Elle n'apparaît que si le montant remis dépasse le dû : sur un versement partiel, il n'y a
 * rien à rendre et rien à afficher.
 *
 * Parcours en LECTURE : il saisit un montant sans encaisser.
 */
scenario('CLI-20', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const panneau = page.locator('.detail-column');
  const formulaire = page.locator('app-reglement-differe-form');

  await etape(1, async () => {
    await page.goto('/differes');
    await lignes.first().click();
    await panneau.getByRole('tab', { name: /Régler/ }).click();
    await expect(formulaire).toBeVisible();
    // Rien à rendre tant que rien ne dépasse le solde.
    await expect(panneau).not.toContainText('Monnaie à rendre');
  });

  await etape(2, async () => {
    // Un montant délibérément très supérieur au solde : le billet qu'on tend au comptoir.
    const montant = formulaire.locator('app-input-number input').first();
    await montant.fill('10000000');
    await montant.blur();
  });

  await etape(3, async () => {
    // La monnaie s'affiche AVANT la validation : c'est ce qui la rend utile.
    await expect(panneau).toContainText('Monnaie à rendre');
  });
});
