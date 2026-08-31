import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un retour transmis électroniquement est traité plus vite qu'un carton accompagné d'un papier :
 * le grossiste l'enregistre avant même de le recevoir, et l'avoir suit.
 *
 * L'envoi n'est offert que sur un retour encore modifiable et chez un fournisseur compatible
 * PharmaML — les deux conditions se lisent sur la ligne : le bouton n'apparaît pas ailleurs.
 * Une fois parti, le retour se fige : il ne se modifie plus (ACH-50), puisque le grossiste en
 * a désormais connaissance.
 *
 * Parcours en LECTURE, et c'est une contrainte ferme : chaque envoi part chez un vrai
 * grossiste. Le parcours montre le point d'entrée sans transmettre.
 */
scenario('ACH-55', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Retours fournisseurs/);
    await expect(lignes.first()).toBeVisible();
  });

  await etape(2, async () => {
    // L'envoi vit sur la ligne, à côté de la modification : les deux disparaissent ensemble
    // au moment où le fournisseur prend connaissance du retour.
    const envoyable = lignes.filter({ has: page.locator('app-button[ngbtooltip="Envoyer via PharmaML"]') });
    const modifiable = lignes.filter({ has: page.locator('app-button[ngbtooltip="Modifier le retour"]') });
    // Le jeu de démonstration n'a pas forcément de fournisseur compatible : ce qui doit
    // rester vrai, c'est que l'envoi n'est jamais offert sans la modification.
    if ((await envoyable.count()) > 0) {
      await expect(envoyable.first().locator('app-button[ngbtooltip="Modifier le retour"]')).toBeVisible();
    } else {
      await expect(modifiable.first()).toBeVisible();
    }
  });
});
