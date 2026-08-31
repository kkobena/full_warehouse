import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide, chercherProduit, seConnecterEnTantQue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le seul parcours joué avec un compte AUTRE que l'administrateur, et il ne peut pas en être
 * autrement : la demande d'autorisation n'apparaît que pour un utilisateur DÉPOURVU du
 * privilège. En administrateur, la ligne partirait sans un mot, et l'image ne montrerait
 * rien du contrôle qu'il s'agit justement d'illustrer.
 *
 * KOFFI KONE (kkone) est vendeur ET caissier — il peut donc tenir le comptoir, la vente
 * étant réservée aux caissiers — mais depuis V1.9.8 ni l'un ni l'autre de ces rôles ne
 * détient la suppression de ligne. La clé attendue est celle d'un utilisateur habilité — le
 * back-office accepte indifféremment mot de passe ou clé (`getUserByPwdOrSecurityKey`), et
 * consigne l'usage dans `utilisation_cle_securite`.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre une utilisation de clé de sécurité.
 */
scenario('VTE-46', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await seConnecterEnTantQue(page, 'kkone', 'admin');
  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await chercherProduit(page, produit);
    await ajouterAuPanier(page, '1');
    await expect(lignes.first()).toContainText(produit);

    await lignes.first().getByRole('button', { name: 'Supprimer' }).click();
    // Deux fenêtres s'enchaînent : la confirmation ordinaire, puis — et seulement pour un
    // utilisateur non habilité — la demande d'autorisation.
    await expect(modale).toContainText('Voulez-vous supprimer');
    await modale.getByRole('button', { name: 'Oui' }).click();

    // La ligne est TOUJOURS là : c'est la demande d'autorisation qui s'affiche, en nommant le
    // privilège requis. Rien n'a encore été supprimé.
    await expect(modale).toContainText('Autorisation requise');
    await expect(modale).toContainText('Suppression de produit');
    await expect(lignes.first()).toContainText(produit);
  });

  await etape(2, async () => {
    // La clé du superviseur — numérique, et distincte de son mot de passe : le back-office
    // compare son SHA-256 à `app_user.action_authority_key`. Le commentaire est facultatif,
    // mais c'est lui qui rendra l'opération compréhensible dans le journal des autorisations.
    await modale.locator('#securityKey input').fill('1234');
    await modale.locator('#comment').fill('Erreur de saisie signalée par le client');
    await modale.getByRole('button', { name: 'Autoriser' }).click();

    await expect(modale).toBeHidden();
    // La suppression n'a lieu qu'après l'autorisation : le panier redevient vide.
    await expect(page.locator('#main-content')).toContainText(/Aucun produit dans la vente|Panier vide/i);
  });
});
