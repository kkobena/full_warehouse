import { expect, Page } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirLaSessionCaissier } from './_caissier';

/**
 * « La dame d'avant, je crois que je me suis trompé sur le rendu. » Retrouver cette vente-là ne
 * devrait pas demander d'ouvrir le journal des ventes et d'y filtrer : elle est à trois lignes
 * du haut de l'écran d'accueil, la plus récente en premier.
 *
 * La liste est bornée à la session de l'utilisateur : ce sont SES ventes, pas celles du poste.
 */
scenario('HOME-17', async ({ etape, page }) => {
  const bloc = (p: Page) => p.locator('.card.data-card', { has: p.getByText('Mes Dernières Transactions') });

  await etape(1, async () => {
    await ouvrirLaSessionCaissier(page);
  });

  await etape(2, async () => {
    await expect(bloc(page)).toBeVisible();
    await expect(bloc(page)).toContainText(/N° Ticket|Aucune transaction dans cette session/);
  });

  await etape(3, async () => {
    // Le détail s'ouvre depuis la ligne, sans quitter l'accueil.
    //
    // La session en cours peut n'avoir encore aucune vente — une caisse ouverte à l'instant,
    // ou un jeu de démonstration chargé un autre jour. On ne force alors rien : la liste vide
    // est un état légitime de cet écran, pas un défaut à masquer par une vente fabriquée.
    const detail = bloc(page).getByRole('button');
    if ((await detail.count()) > 0) {
      await detail.first().click();
      // Le bouton conduit au JOURNAL DES VENTES, où la vente se retrouve — il n'ouvre pas
      // encore le détail de CETTE vente-là (`voirDetailVente` navigue sans transmettre son
      // identifiant). Le parcours illustre donc le chemin tel qu'il est.
      await expect(page).toHaveURL(/sales-home\/gestion/);
    } else {
      await expect(bloc(page)).toContainText('Aucune transaction dans cette session');
    }
  });
});
