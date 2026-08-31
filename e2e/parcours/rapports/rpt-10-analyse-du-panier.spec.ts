import { expect } from '@playwright/test';
import { rechercher, saisirMontant } from '../../src/actions';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * « Ces deux produits partent souvent ensemble » est une intuition de comptoir. Le rapport en
 * fait une mesure, avec trois grandeurs qu'il ne faut pas confondre :
 *
 *   • le SUPPORT — à quelle fréquence la combinaison apparaît. Un support faible signale une
 *     coïncidence, aussi élégante soit-elle ;
 *   • la CONFIANCE — la probabilité d'acheter B quand on a acheté A. Elle n'est pas
 *     symétrique : tout le monde prend du paracétamol, presque personne ne prend l'autre ;
 *   • le LIFT — combien l'association dépasse le hasard. À 1, il n'y a rien à voir ; c'est le
 *     seul des trois qui dise s'il se passe VRAIMENT quelque chose.
 *
 * Les deux seuils sont réglables parce qu'ils dépendent de l'officine : ce qui est rare dans
 * un quartier est courant dans un autre.
 */
scenario('RPT-10', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await ouvrirRapport(page, 'sales', 'Analyse du Panier');
    await expect(page.getByRole('heading', { name: 'Analyse du Panier' })).toBeVisible();
  });

  await etape(2, async () => {
    // Des seuils bas : sur six mois d'historique, des seuils exigeants ne laisseraient
    // passer aucune association et l'écran paraîtrait vide à tort.
    await saisirMontant(page, '#supportmini', '1');
    await saisirMontant(page, '#confidencemini', '10');
    await rechercher(page);
  });

  await etape(3, async () => {
    await expect(contenu).toContainText(/ASSOCIATIONS TROUVÉES/i);
    // Le lift maximal est mis en avant : c'est lui qui distingue une association réelle
    // d'un simple effet de volume.
    await expect(contenu).toContainText(/LIFT/i);
    await expect(page.getByRole('heading', { name: /Associations de Produits/ })).toBeVisible();
  });
});
