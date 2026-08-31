import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * PharmaML est le canal par lequel la commande part CHEZ LE GROSSISTE, sans ressaisie ni
 * téléphone : l'officine émet, le répartiteur répond — disponibilités, ruptures,
 * substitutions. C'est l'automatisation qui distingue une officine équipée d'une autre.
 *
 * Une fois soumise, la commande se VERROUILLE : ses lignes ne se modifient plus, car ce qui a
 * été transmis ne peut plus être réécrit unilatéralement. Le bandeau le dit à l'écran.
 *
 * Parcours ÉCRIVANT — et qui SORT de l'application : l'envoi part réellement chez le
 * grossiste. La commande de démonstration est donc tenue au strict minimum, une ligne d'un
 * produit à petit prix, sous le plafond convenu de 5 000 F.
 */
scenario('ACH-28', async ({ etape, page }) => {
  // On commande à une AGENCE, comme en officine : c'est elle qui livre. La configuration
  // PharmaML, elle, vit sur le grossiste PRINCIPAL — l'application remonte au parent pour la
  // lire, ce qui évite de la ressaisir sur chaque agence.
  const fournisseur = 'LABOREX ABIDJAN COCODY';
  // Un produit dont le CIP est RÉELLEMENT connu du grossiste — le message PharmaML transmet
  // le code, pas le libellé, et un code inventé ne donnerait ni disponibilité, ni rupture, ni
  // substitution en retour (`98_pharmaml_local.sql`). Prix d'achat 274 F, quantité 1 : la
  // commande d'essai reste très en dessous du plafond convenu de 5 000 F.
  const produit = 'ARNICA MONTANA 5CH';
  const grille = page.locator('app-commande-requested');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    await page.getByRole('button', { name: 'Nouvelle commande' }).click();
    await chercherDansSelect(page, 'fournisseur-select', fournisseur, fournisseur);
    await chercherDansSelect(page, 'produitbox', produit, produit);
    await page.locator('input[placeholder="Qté"]').fill('1');
    await page.locator('input[placeholder="Qté"]').press('Enter');
    // La commande existe dès sa première ligne : rien d'autre à valider avant l'envoi.
    await expect(grille).toContainText(produit);
    await expect(grille).toContainText('1 ligne(s)');
  });

  await etape(2, async () => {
    // L'envoi se déclenche aussi depuis le menu de la LIGNE, dans la liste des commandes —
    // c'est ce qui permet de transmettre ses commandes du matin l'une après l'autre sans les
    // ouvrir. Ici, on montre le chemin depuis la commande ouverte.
    //
    // Le canal n'offre plus qu'une action sur une commande en préparation : l'envoi. Le
    // bouton à choix se réduit alors à un bouton simple — la comparaison de disponibilité
    // qui l'accompagnait est masquée, le répartiteur ne sachant pas y répondre
    // (COMPARAISON_DISPONIBILITE_ACTIVE).
    const chevron = page.locator('app-split-button').filter({ hasText: /Actions PharmaML/ });
    if (await chevron.count()) {
      await chevron.getByRole('button').last().click();
    }
    await page.getByRole('button', { name: 'Envoyer via PharmaML' }).first().click();
    const modale = page.locator('.modal-content');
    await expect(modale).toContainText('Envoi commande PharmaML');
    await expect(modale).toContainText('Date de livraison souhaitée');
  });

});
