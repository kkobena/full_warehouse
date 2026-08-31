import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'exclusion de rayons porte sur des PRODUITS, non sur des ventes : c'est ce qui la rend
 * commode — un article nouveau hérite du sort de son rayon — et c'est aussi ce qui la rend
 * difficile à vérifier de tête.
 *
 * Ce journal la rend lisible : quelles lignes sont sorties, pour quelle quantité et quelle
 * valeur. Un produit qui change de rayon change de sort ; sans ce journal, on ne s'en
 * apercevrait qu'à la déclaration suivante.
 */
scenario('DCA-06', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/declaration-ca');
    await ouvrirOnglet(page, 'Produits de rayons exclus');
    await expect(contenu).toContainText('Produits de rayons exclus');
  });

  await etape(2, async () => {
    await expect(contenu).toContainText(/lignes concernées/i);
    await expect(contenu).toContainText(/quantité vendue/i);
    // Le retrait est TOTAL sur ces lignes, à la différence des unités gratuites.
    await expect(contenu).toContainText(/sortent entièrement du chiffre d'affaires à déclarer/);
  });
});
