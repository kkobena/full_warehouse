import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirLaSessionCaissier } from './_caissier';

/**
 * Un caissier fait quatre choses dans sa journée : il vend, il enregistre une entrée ou une
 * sortie d'espèces, il imprime son point de caisse, il ouvre et ferme sa caisse. Les quatre
 * sont en haut de son écran d'accueil, sans un seul passage par le menu.
 *
 * Le bouton de caisse mérite un mot : il n'offre pas deux choix mais un seul, celui que l'état
 * réel de la caisse rend possible. Caisse fermée, il propose de l'ouvrir ; ouverte, de la
 * fermer. On ne peut donc pas ouvrir deux fois la même caisse par inadvertance.
 */
scenario('HOME-12', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirLaSessionCaissier(page);
  });

  await etape(2, async () => {
    const actions = page.locator('.quick-actions');
    await expect(actions.getByRole('button', { name: /Nouvelle Vente/i })).toBeVisible();
    await expect(actions.getByRole('button', { name: /Mvt Caisse/i })).toBeVisible();
    await expect(actions.getByRole('button', { name: /Ticket Z/i })).toBeVisible();
    // Un seul des deux libellés, jamais les deux : le bouton suit l'état de la caisse.
    await expect(actions.getByRole('button', { name: /Ouvrir Caisse|Fermer Caisse/i })).toHaveCount(1);
  });
});
