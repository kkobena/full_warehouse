import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une livraison qui déborde du rayon n'a nulle part où aller : les boîtes s'empilent devant,
 * on ne voit plus les péremptions, et la réserve reste vide alors qu'elle est faite pour ça.
 *
 * Le rangement se déclenche donc SEUL, à la finalisation de la réception, et seulement quand
 * les deux conditions sont réunies : le rayon dépasse son stock maxi ET la réserve est sous
 * son seuil. Seul l'excédent au-delà du maxi descend — le rayon reste plein, ce qui est le
 * but ; ce n'est pas un vidage.
 *
 * Les lots suivent en FEFO : ce qui périme en premier reste devant, en rayon.
 *
 * L'écran de répartition donne les deux lectures — le réassort suggéré pour la réserve, et
 * l'historique de ce qui a été rangé automatiquement.
 */
scenario('ACH-71', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Répartition & Transferts/);
    await expect(contenu).toContainText('Réassort suggéré');
  });

  await etape(2, async () => {
    // La suggestion RÉSERVE est celle que le rangement automatique consomme : elle calcule
    // l'excédent transférable depuis le rayon.
    await expect(contenu).toContainText('Réassort suggéré – Réserve');
  });

  await etape(3, async () => {
    // Et le transfert manuel demeure, pour ce que l'automatisme ne couvre pas.
    await expect(contenu).toContainText('Transfert manuel stock Rayon / Réserve');
  });
});
