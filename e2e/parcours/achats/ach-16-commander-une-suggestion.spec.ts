import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Commander une proposition, c'est la faire changer de nature : ses lignes deviennent un bon
 * de commande chez le grossiste concerné, prêt à être transmis. La proposition disparaît alors
 * des propositions — elle a fait son office.
 *
 * L'action est offerte sur la ligne, sans ouvrir la proposition : c'est le geste de celui qui
 * a déjà arbitré et qui traite ses quatre grossistes à la suite.
 *
 * Parcours en LECTURE : commander transformerait une proposition du jeu de démonstration en
 * commande, et les parcours de propositions comptent sur les quatre qui y figurent.
 */
scenario('ACH-16', async ({ etape, page }) => {
  const liste = page.locator('app-suggestion-fournisseur-list');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().getByRole('button', { name: 'Actions' }).click();
  });

  await etape(2, async () => {
    // « Commander » sur une proposition VALIDÉE, « Valider » puis « Commander » sur une
    // proposition seulement générée : l'écran n'offre que ce qui a un sens pour son état.
    await expect(page.getByRole('button', { name: 'Commander' })).toBeVisible();
  });

  etape.horsPortee(
    3,
    'confirmer créerait la commande fournisseur et consommerait la proposition ; le geste est ' +
      'illustré, la transformation ne l’est pas.',
  );
});
