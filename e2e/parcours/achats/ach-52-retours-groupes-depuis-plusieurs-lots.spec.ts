import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le tri des périmés se fait par brassée : on vide un rayon, on en sort quinze lots dépassés,
 * et les renvoyer un par un serait quinze fois le même geste.
 *
 * Le traitement groupé applique à chaque lot exactement la mécanique individuelle d'ACH-51 —
 * résolution de la commande d'origine, sortie de stock — mais en une seule opération. Le
 * compte rendu final distingue les succès des échecs : un lot dont la commande d'origine est
 * introuvable ne fait pas capoter les quatorze autres.
 *
 * Parcours en LECTURE : il prépare le lot d'opérations sans le lancer.
 */
scenario('ACH-52', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(lignes.first()).toBeVisible();
    // Deux lots mono-emplacement : le groupé n'a pas à choisir d'emplacement pour eux.
    const candidats = lignes.filter({ has: page.locator('.pharma-badge-info .pi-map-marker') });
    await candidats.nth(0).locator('input[type="checkbox"]').first().check();
    await candidats.nth(1).locator('input[type="checkbox"]').first().check();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: /Retour fournisseur groupé/ }).click();
    await expect(modale).toBeVisible();
    // Un bon par lot, mais un seul geste — et un motif commun, puisqu'ils repartent
    // ensemble pour la même raison.
    await expect(modale).toContainText(/Motif de retour \(commun à tous les lots\)/);
  });
});
