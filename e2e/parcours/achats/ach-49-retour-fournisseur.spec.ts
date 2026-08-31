import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un produit arrivé cassé, périmé ou jamais commandé repart chez le grossiste — et il repart
 * AVEC UN MOTIF. Le motif n'est pas une formalité : c'est lui qui décide si l'officine attend
 * un avoir, un remplacement, ou rien.
 *
 * Le retour se construit depuis la réception concernée, ligne à ligne, avec la quantité
 * réellement renvoyée : on ne retourne pas toujours tout ce qu'on a reçu.
 *
 * Parcours en LECTURE : il ouvre la saisie sans valider un retour que les parcours de stock
 * ne s'attendent pas à trouver.
 */
scenario('ACH-49', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').filter({ hasNotText: 'Clôturé' }).first().dblclick();
    await page.getByRole('button', { name: 'Retourner' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Chaque ligne reçue peut repartir, en tout ou partie, et le motif est OBLIGATOIRE —
    // l'astérisque de l'en-tête le dit.
    await expect(modale).toContainText('Qté reçue');
    await expect(modale).toContainText('Qté à retourner');
    await expect(modale).toContainText('Motif');
  });

  etape.horsPortee(
    3,
    'valider créerait un retour et sortirait les quantités du stock ; la saisie et le choix ' +
      'du motif sont illustrés, la validation ne l’est pas.',
  );
});
