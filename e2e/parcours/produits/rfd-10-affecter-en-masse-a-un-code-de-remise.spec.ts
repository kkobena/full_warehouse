import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une remise se négocie par rayon entier — toute la parapharmacie, toute l'OTC — jamais
 * produit par produit. Rattacher trois cents fiches une à une est le genre de tâche qu'on ne
 * fait pas : la grille reste alors sur le papier.
 *
 * L'écran prend donc le problème dans l'autre sens : on part du CODE, on filtre les produits
 * par rayon ou par recherche, on coche — ou l'on prend tout d'un geste — et l'on rattache en
 * une action.
 *
 * Le panneau de droite montre ensuite ce que le code couvre réellement : c'est là qu'on
 * vérifie, plutôt que d'ouvrir des fiches au hasard.
 *
 * Parcours en LECTURE : il montre la sélection sans l'enregistrer, un rattachement en masse
 * changeant le prix de vente de centaines de produits.
 */
scenario('RFD-10', async ({ etape, page }) => {
  // L'onglet voisin reste dans le DOM une fois visité : sans porter le sélecteur sur le
  // composant, on désigne les lignes de l'autre tableau.
  const lignes = page.locator('app-code-remise-produit tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/remises');
    await ouvrirOnglet(page, /Codes remise/);
    await expect(lignes.first()).toBeVisible();
    await expect(page.locator('#main-content')).toContainText('Remise VNO');
  });

  await etape(2, async () => {
    // Le crayon d'une ligne ouvre l'affectation pour CE code : le code n'est jamais à
    // ressaisir, il est le point de départ.
    // Le bouton n'a pas de libellé, seulement une icône et une infobulle — que `getByRole`
    // ne lit pas. On le désigne par l'infobulle portée par l'hôte `app-button`.
    await lignes.nth(2).locator('app-button[ngbtooltip="Associer des produits"] button').click();
    await expect(modal).toBeVisible();
    // La liste part vide : sans filtre, on afficherait le catalogue entier pour rien.
    await modal.getByPlaceholder('Taper pour rechercher').fill('DOLIPRANE');
    await expect(modal.locator('tbody tr').first()).toBeVisible({ timeout: 15000 });
  });

  await etape(3, async () => {
    // La case d'en-tête prend toute la page d'un geste — c'est elle qui rend l'opération
    // praticable sur un rayon entier.
    await modal.locator('thead input[type="checkbox"]').first().click();
    await expect(modal.locator('tbody input[type="checkbox"]:checked').first()).toBeVisible();
  });

  await etape(4, async () => {
    await expect(modal.getByRole('button', { name: 'Enregistrer' })).toBeEnabled();
  });
});
