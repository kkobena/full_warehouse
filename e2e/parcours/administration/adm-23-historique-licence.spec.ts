import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Deux questions se posent le jour où la licence pose problème : « depuis quand ? » et « qui
 * j'appelle ? ». L'écran répond aux deux sans quitter l'application — l'historique journalise
 * les activations, expirations et refus, et les coordonnées du revendeur voyagent avec
 * l'abonnement lui-même.
 *
 * Parcours en LECTURE.
 */
scenario('ADM-23', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/licence');
    await expect(contenu).toContainText('Gérer ma licence');
  });

  await etape(2, async () => {
    // L'historique dit la date, l'événement, son détail et l'utilisateur : une activation
    // n'est jamais anonyme. Tant qu'aucun événement n'a eu lieu, le panneau le dit plutôt
    // que d'afficher un tableau vide.
    const historique = contenu.locator('app-card').filter({ hasText: 'Historique' }).first();
    await expect(historique).toBeVisible();
    await expect(historique).toContainText(/Date|Aucun événement enregistré/);
  });

  await etape(3, async () => {
    // Les coordonnées du support sont portées par la licence : elles n'ont pas à être
    // ressaisies à chaque poste. Quand un revendeur a vendu l'abonnement, ce sont les
    // siennes ; sinon, celles de l'éditeur — dans les deux cas un contact cliquable.
    await expect(contenu.locator('a[href^="mailto:"]').first()).toBeVisible();
  });
});
