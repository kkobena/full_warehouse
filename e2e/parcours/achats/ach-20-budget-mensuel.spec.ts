import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une officine achète avec sa trésorerie : le budget mensuel de commande est le garde-fou qui
 * empêche de valider en fin de mois des propositions qu'on ne pourra pas payer. L'écran de
 * réapprovisionnement le rappelle EN TÊTE, avant toute décision.
 *
 * Trois montants suffisent à trancher : ce que les propositions en cours coûteraient, le
 * plafond du mois, et ce qui a déjà été engagé. C'est leur addition — pas le seul plafond —
 * qui dit s'il reste de la place.
 *
 * Le plafond se règle dans le paramétrage (ADM-14) ; à zéro, il vaut « illimité » et
 * l'indicateur ne s'affiche pas.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-20', async ({ etape, page }) => {
  const ecran = page.locator('app-suggestion-home');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Propositions d'achat/ }).click();
    await expect(ecran).toContainText(/[Bb]udget mensuel/);
  });

  await etape(2, async () => {
    // Les trois montants de l'arbitrage, dans le même bandeau.
    await expect(ecran).toContainText('Montant estimé');
    await expect(ecran).toContainText('Budget mensuel');
    await expect(ecran).toContainText(/Déjà commandé/);
  });
});
