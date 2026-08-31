import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le modèle SEMOIS ne calcule rien tant que les produits n'ont pas de classe. Sur un
 * catalogue de plusieurs milliers de références, les classer à la main n'arrivera jamais.
 *
 * Deux chemins, et ils ne se valent pas :
 *
 *   • INITIALISER met tous les produits actifs non configurés en classe B — le milieu, le
 *     moins risqué. C'est le démarrage à froid, quand l'historique manque encore ;
 *   • CLASSER par ABC/Pareto propose une répartition déduite du chiffre d'affaires cumulé et
 *     de la rotation. C'est le vrai classement, celui qui distingue les 20 % de références
 *     qui font 80 % du chiffre.
 *
 * Le second se PRÉVISUALISE avant de s'appliquer, et l'écran affiche le compte par classe :
 * on voit combien de produits passeraient en A+, en A, en D, avant de valider. Un classement
 * appliqué à l'aveugle changerait la politique de commande de tout le catalogue.
 *
 * Parcours en LECTURE : il prévisualise sans appliquer.
 */
scenario('ACH-25', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/semois/config-masse');
    await expect(contenu).toContainText('Classification Auto (ABC)');
  });

  await etape(2, async () => {
    // Le vrai classement, et son garde-fou : on regarde le compte par classe avant
    // d'appliquer quoi que ce soit.
    await expect(page.getByRole('button', { name: 'Prévisualiser la classification' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Appliquer la classification ABC' })).toBeVisible();
  });

  await etape(3, async () => {
    // Le démarrage à froid vit dans l'onglet voisin : tout en B, le milieu prudent, quand
    // l'historique manque encore pour classer.
    await page.getByRole('tab', { name: /Configuration Manuelle/ }).click();
    await expect(page.getByRole('button', { name: 'Initialiser tous les produits actifs' })).toBeVisible();
  });

  etape.horsPortee(
    4,
    'appliquer un classement réécrirait la classe de tout le catalogue, et avec elle les ' +
      'suggestions illustrées par les autres parcours.',
  );
});
