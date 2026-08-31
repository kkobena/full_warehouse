import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Reprendre le catalogue d'un confrère, ou celui d'un logiciel qu'on quitte, ne se fait pas à
 * la main : l'import lit un fichier fournisseur ligne par ligne. Le point qui rassure — et
 * qu'il faut connaître avant de se lancer — c'est que les lignes en erreur N'ARRÊTENT PAS
 * l'import : elles sont mises de côté dans un rapport de rejet, et les lignes valides entrent.
 *
 * Trois natures d'import, et le choix n'est pas cosmétique : « Nouvelle installation » monte
 * un catalogue vide, « Basculement » reprend l'existant d'un autre logiciel, « Basculement
 * prestige » celui d'un format particulier. Le fournisseur choisi est celui dont les codes CIP
 * et les prix d'achat seront rattachés aux produits importés.
 *
 * Parcours en LECTURE : lancer un import réel créerait des centaines de produits dans la base
 * de démonstration, ce que la troisième étape déclare hors portée.
 */
scenario('REF-06', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/produits');
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    // Le bouton « Importer » est un bouton à choix : c'est la NATURE de l'import qui se
    // décide en premier, avant même de désigner le fichier.
    await page.locator('app-split-button').getByRole('button').last().click();
    await expect(page.getByRole('button', { name: 'Nouvelle installation' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Basculement', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Basculement prestige' })).toBeVisible();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Nouvelle installation' }).click();
    await expect(modale).toBeVisible();
    // Deux renseignements, et le second ne sert à rien sans le premier : le fournisseur donne
    // aux produits importés leurs codes et leurs prix d'achat.
    await expect(modale).toContainText('Fournisseurs');
    await expect(modale.locator('input[type="file"]')).toHaveCount(1);
  });

  etape.horsPortee(
    3,
    'un import réel créerait des centaines de produits dans la base de démonstration ; le ' +
      'rapport de rejet ne peut donc pas être produit sans détruire le jeu de données.',
  );
});
