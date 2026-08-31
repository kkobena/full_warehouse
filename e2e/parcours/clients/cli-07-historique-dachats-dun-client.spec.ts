import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * « Qu'est-ce que je vous avais donné la dernière fois ? » — la question tombe au comptoir, et
 * chercher dans le journal général des ventes prendrait dix minutes.
 *
 * L'historique de la fiche répond immédiatement : toutes les ventes rattachées au client, de
 * la plus récente à la plus ancienne. C'est le suivi thérapeutique autant que la réclamation :
 * un patient qui revient pour un effet indésirable, un renouvellement, un produit qu'il ne
 * retrouve plus.
 *
 * Le rattachement se fait à la vente : ce qui n'a pas été enregistré au nom du client n'y
 * figure pas, et c'est ce qui justifie de créer une fiche même sommaire (CLI-02).
 */
scenario('CLI-07', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(lignes.first()).toBeVisible();
    await lignes.first().locator('app-button[ngbtooltip="Voir détails"] button').first().click();
    await expect(contenu).toContainText('Achats');
  });

  await etape(2, async () => {
    // Trois colonnes suffisent à retrouver une visite : son numéro, sa date, son montant.
    // Les en-têtes sont traduits à l'affichage — « Numéro », « Modifiée le », « Montant
    // net » — et non rendus depuis les clés du gabarit.
    await expect(contenu).toContainText('Numéro');
    await expect(contenu).toContainText('Modifiée le');
    await expect(contenu).toContainText('Montant net');
  });
});
