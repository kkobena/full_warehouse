import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le solde d'un fournisseur n'a de sens que décomposé : ce qu'on lui a commandé, ce qu'on lui
 * a déjà réglé, et ce qui reste dû bon par bon. Le détail met les trois côte à côte, puis
 * ouvre le règlement dans le même écran — c'est ce qui évite de recopier un montant d'un
 * onglet à l'autre.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-61', async ({ etape, page }) => {
  const ecran = page.locator('app-comptes-fournisseurs');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Comptes fournisseurs/);
    await expect(ecran.locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    await ecran.locator('tbody tr').first().click();
    // L'en-tête du détail porte les trois montants du raisonnement.
    await expect(ecran).toContainText('Commandé');
    await expect(ecran).toContainText('Réglé');
    await expect(ecran).toContainText('Solde');
  });

  await etape(3, async () => {
    // Le détail bon par bon : ce que chacun totalise, ce qui a été réglé dessus, et ce qui
    // reste — la somme de cette colonne est le solde affiché plus haut.
    await expect(ecran).toContainText('N° Bon');
    await expect(ecran).toContainText('Restant dû');
    await expect(ecran).toContainText(/Date échéance/i);
  });
});
