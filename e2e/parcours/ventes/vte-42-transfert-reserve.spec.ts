import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le rayon est vide, la réserve ne l'est pas : la vente ne doit pas s'arrêter là. L'écran
 * l'annonce en chiffres — combien en rayon, combien en réserve, combien au total — et propose
 * le transfert qui débloque la situation, sans quitter la vente.
 *
 * Le mouvement n'est pas cosmétique : c'est un vrai transfert entre stockages, celui-là même
 * que le module de réassort ferait plus tard. Il est simplement déclenché au moment où le
 * besoin apparaît.
 *
 * La proposition ne s'affichait QUE sur la deuxième ligne d'une vente : pour la première, le
 * cas n'était pas routé vers ce dialogue et la vente échouait sur un message d'erreur, alors
 * que la marchandise était disponible deux mètres plus loin. Corrigé.
 *
 * Parcours en LECTURE : il décline le transfert, les stocks restent en place.
 */
scenario('VTE-42', async ({ etape, page }) => {
  // SULFUR 30CH : 12 en rayon, 6 en réserve. Demander 15 dépasse le rayon sans dépasser le
  // total — c'est exactement la situation que le transfert résout.
  const produit = 'SULFUR 30CH';
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await expect(page.locator('#main-content')).toContainText(/Rayon\s*:/);
    await ajouterAuPanier(page, '15');
  });

  await etape(2, async () => {
    // Les trois chiffres sont dans la question : le caissier sait ce qu'il y a, où, et ce
    // que le transfert va déplacer.
    await expect(modale).toContainText('Stock en réserve');
    await expect(modale).toContainText('rayon');
    await expect(modale).toContainText('réserve');
    await expect(modale).toContainText('transfert');
    await expect(modale.getByRole('button', { name: 'Oui' })).toBeVisible();
  });

  // ── Remise en état : on décline, aucun stock n'est déplacé. ─────────────────────────────
  await modale.getByRole('button', { name: 'Non' }).click();
  await expect(modale).toBeHidden();
});
