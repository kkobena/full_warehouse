import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un client vient payer son carnet. Le geste est court, et l'écran le raccourcit encore : le
 * montant est pré-rempli avec la totalité du solde, parce que c'est le cas le plus fréquent.
 * Le modifier suffit pour un versement partiel.
 *
 * Le règlement porte sur le COMPTE, pas sur une vente : il s'impute sur l'ensemble des ventes
 * à crédit du client, de la plus ancienne à la plus récente. C'est pourquoi le formulaire ne
 * demande jamais de choisir une vente.
 *
 * Parcours en LECTURE jusqu'à la validation : encaisser pour de bon viderait le compte et
 * priverait les parcours suivants de leur créance.
 */
scenario('CLI-15', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const panneau = page.locator('.detail-column');

  await etape(1, async () => {
    await page.goto('/differes');
    await lignes.first().click();
    await expect(panneau).toBeVisible();
    await expect(panneau).toContainText(/Reste :/);
  });

  await etape(2, async () => {
    await panneau.getByRole('tab', { name: /Régler/ }).click();
    const formulaire = page.locator('app-reglement-differe-form');
    await expect(formulaire).toBeVisible();
    // Trois champs, dans l'ordre du geste : quand, comment, combien. Le mode est déjà sur
    // « espèces » et le montant sur le solde entier — la saisie courante ne touche à rien.
    await expect(formulaire.locator('#rdDatePaiement')).toBeVisible();
    await expect(formulaire.locator('ng-select')).toBeVisible();
    await expect(formulaire.locator('app-input-number input')).not.toHaveValue('');
  });

  await etape(3, async () => {
    // Le bouton n'est actif que sur un montant saisi : un règlement de zéro n'existe pas.
    await expect(page.getByRole('button', { name: 'Valider' })).toBeEnabled();
  });
});
