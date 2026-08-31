import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Rattacher un produit à un tableau, c'est renchérir son prix de vente du montant que le
 * tableau porte : à 1 550 pour un tableau de valeur 100, l'article passe à 1 650 en caisse.
 *
 * Le geste est donc commercial, et il est en MASSE par nature — une majoration ne vise jamais
 * un article isolé mais une catégorie entière. L'écran à deux colonnes le rend lisible : à
 * gauche ce qui n'est pas rattaché, à droite ce qui l'est, et le passage se fait dans les deux
 * sens. « Tout associer » sert quand la mesure couvre l'ensemble d'une sélection.
 *
 * Dissocier est tout aussi nécessaire : une majoration levée doit redescendre les prix.
 *
 * Parcours en LECTURE : il montre les deux colonnes et leurs actions sans rien déplacer,
 * chaque mouvement changeant un prix de vente.
 */
scenario('RFD-08', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/tableaux');
    await expect(lignes.first()).toBeVisible();
    // L'écran d'association s'ouvre depuis la ligne du tableau : celui-ci est le point de
    // départ, jamais à ressaisir.
    await lignes.first().locator('app-button[ngbtooltip="Associer des produits"] button').first().click();
  });

  await etape(2, async () => {
    // Les deux colonnes, côte à côte : le stock de produits libres, et ceux qui subissent
    // déjà la majoration.
    await expect(contenu).toContainText('Code CIP');
    await expect(page.getByRole('button', { name: 'Tout associer' })).toBeVisible();
  });

  await etape(3, async () => {
    // Et le sens inverse : une majoration levée doit pouvoir redescendre les prix.
    await expect(page.getByRole('button', { name: 'Tout retirer' })).toBeVisible();
  });
});
