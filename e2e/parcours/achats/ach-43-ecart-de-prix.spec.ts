import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Entre le jour où la commande part et le jour où elle arrive, le tarif du grossiste a pu
 * changer. Valider la réception les yeux fermés, c'est accepter la hausse sans l'avoir vue —
 * et la retrouver plus tard dans la marge, sans savoir d'où elle vient.
 *
 * PharmaSmart compare donc, ligne à ligne, le prix d'achat COMMANDÉ au tarif actuel du couple
 * produit/fournisseur. Au-delà d'un seuil configurable — `APP_SEUIL_VARIATION_PRIX`, 20 % par
 * défaut — la ligne est signalée : un avertissement s'affiche dans la colonne P.A avec le
 * tarif catalogue en regard, et le panneau de concordance compte les écarts.
 *
 * Le signalement ne bloque pas : à la finalisation, l'écart est rappelé et l'utilisateur
 * tranche (ACH-47). C'est un garde-fou, pas un verrou.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-43', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await ouvrirBonDeReception(page, 'aucun');
    await page.getByRole('button', { name: 'Grille' }).click();
    await expect(page.locator('.ag-row').first()).toBeVisible();
  });

  await etape(2, async () => {
    // La ligne en écart porte l'avertissement DANS sa cellule de prix d'achat, avec le
    // tarif catalogue à côté : la comparaison est faite pour l'œil, pas à faire de tête.
    const alerte = page.locator('[col-id="orderCostAmount"] .pi-exclamation-triangle').first();
    await expect(alerte).toBeVisible();
    // Le panneau de concordance en tient le compte, pour ne pas avoir à parcourir la grille.
    const concordance = page.locator('app-reception-concordance');
    if (await concordance.isVisible().catch(() => false)) {
      await expect(concordance).toContainText(/Écarts prix/);
    }
  });
});
