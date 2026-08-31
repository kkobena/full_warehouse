import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La classe de criticité décide de la prudence appliquée à chaque produit (ACH-23). Or
 * l'importance commerciale d'une référence bouge : une nouveauté monte, un ancien best-seller
 * s'éteint. Sans reclassement, la protection reste calée sur le catalogue d'il y a deux ans.
 *
 * Le reclassement s'appuie sur le PARETO — pourcentage de chiffre d'affaires cumulé — croisé
 * avec la fréquence de vente. Trois garde-fous l'empêchent de faire n'importe quoi : un
 * produit de garde reste A+, un produit essentiel conserve un plancher, et les surcharges
 * manuelles sont préservées.
 *
 * La prévisualisation montre la répartition proposée classe par classe AVANT d'appliquer :
 * c'est elle qui permet de refuser un classement qui viderait la classe A.
 *
 * Parcours en LECTURE : appliquer réécrirait la classe de tout le catalogue et, dans la
 * foulée, toutes les suggestions.
 */
scenario('ACH-80', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  etape.horsPortee(
    1,
    'les seuils Pareto et la fréquence minimale sont des paramètres de calcul sans écran : ' +
      'ils vivent dans la configuration, pas dans l’application.',
  );

  await etape(2, async () => {
    await page.goto('/semois/config-masse');
    await expect(contenu).toContainText('Classification Auto (ABC)');
    await expect(page.getByRole('button', { name: 'Prévisualiser la classification' })).toBeVisible();
  });

  await etape(3, async () => {
    // La règle est écrite avant d'être appliquée : chaque classe dit sur quoi elle repose —
    // rotation annuelle et z-score — de sorte qu'un classement inattendu s'explique.
    await expect(contenu).toContainText(/Classe A .*rotation/is);
    await expect(contenu).toContainText(/Classe D .*Rotation < 1x\/an/is);
    // Deux garde-fous à portée de main : ne pas écraser ce qui a été réglé, et décider du
    // sort des produits sans vente.
    await expect(contenu).toContainText(/Écraser les configurations SEMOIS existantes/);
    await expect(contenu).toContainText(/Inclure les produits sans vente/);
  });

  await etape(4, async () => {
    // L'application enchaîne le recalcul SEMOIS : les nouvelles classes prennent effet sur
    // les suggestions sans second geste.
    await expect(page.getByRole('button', { name: 'Appliquer la classification ABC' })).toBeVisible();
  });
});
