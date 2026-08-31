import { expect } from '@playwright/test';
import {
  ajouterAuPanier,
  assurerPanierVide,
  chercherProduit,
  mettreEnAttente,
  ouvrirVentesEnAttente,
} from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le geste qui sauve le comptoir : un client oublie sa carte, celui d'après n'attend pas. La
 * vente est mise de côté avec ses lignes, la caisse enchaîne, et on la reprend ensuite. Le
 * compteur de la barre supérieure dit combien de ventes patientent.
 *
 * Parcours ÉCRIVANT dans la base : il supprime sa vente en attente après la dernière capture.
 */
scenario('VTE-14', async ({ etape, page }) => {
  const produit = 'PARACETAMOL 1G';
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await assurerPanierVide(page);
  await page.goto('/sales-home');
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '3');
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(2, async () => {
    // Le bouton « En attente » de la zone d'encaissement met la vente de côté ; celui de la
    // barre du haut, homonyme, ouvre au contraire la liste de celles qui patientent. D'où
    // l'ancrage sur le bouton voisin de « Finaliser ».
    await mettreEnAttente(page);
    // L'écran revient au panier vide, prêt pour le client suivant — et le compteur de la
    // barre supérieure s'incrémente : c'est là que la vente est partie.
    await expect(page.locator('#main-content')).toContainText(/Panier vide|Ajoutez des produits/i);
  });

  // ── Remise en état : la vente est reprise puis abandonnée. Il n'existe pas de
  //    suppression directe depuis la liste d'attente — la reprendre est le seul chemin. ──
  await ouvrirVentesEnAttente(page);
  const liste = page.locator('app-pending-sales-list');
  await expect(liste).toBeVisible();
  // Le double-clic sur la ligne, geste que le panneau annonce lui-même (« Double-cliquez sur
  // une ligne pour reprendre la vente »). Le bouton d'action de la ligne, lui, se dérobe :
  // le panneau reste dans le DOM une fois refermé, et Playwright attend alors indéfiniment un
  // bouton présent mais invisible.
  const ligneEnAttente = liste.locator('tbody tr').filter({ visible: true }).first();
  await expect(ligneEnAttente).toBeVisible();
  await ligneEnAttente.dblclick();
  // On ne vérifie pas QUEL panier revient : d'autres parcours peuvent avoir laissé une vente
  // en attente, et la liste est ordonnée par date. Ce qui compte est qu'un panier soit repris
  // — donc annulable.
  await expect(lignes.first()).toBeVisible();
  // La vente reprise redevient la vente EN COURS : la nettoyer, c'est exactement ce que fait
  // `assurerPanierVide`, en boucle et sans supposer sur quel écran la reprise a mené. Écrire
  // ici une annulation à la main revenait à redécouvrir ses pièges un par un.
  await assurerPanierVide(page);
});
