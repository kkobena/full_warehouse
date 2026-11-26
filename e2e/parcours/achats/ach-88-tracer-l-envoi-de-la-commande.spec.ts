import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le statut REQUESTED recouvrait deux situations opposées : un bon en cours de saisie, et une
 * commande partie chez le grossiste dont on attend la livraison. Rien ne les distinguait — ni
 * dans la liste, ni dans le calcul de réassort, qui comptait comme déjà commandé un bon resté
 * sur le bureau et sous-commandait d'autant.
 *
 * Dater l'envoi tranche les deux. Ce parcours vérifie que le geste existe et qu'il se voit :
 * une commande non transmise porte BROUILLON, la même porte ENVOYÉE une fois datée, et le
 * marquage se défait — parce qu'on exporte un bon aussi bien pour l'envoyer que pour le relire.
 *
 * Parcours ÉCRIVANT dans la base : il date puis dé-date l'envoi d'une commande en attente, et
 * la rend à son état initial.
 */
scenario('ACH-88', async ({ etape, page }) => {
  const liste = page.locator('app-commande-requested-home');
  const premiereLigne = liste.locator('tbody tr').first();
  const menu = () => premiereLigne.locator('app-commande-requested-actions button').first();

  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);
  await page.getByRole('tab', { name: /Commandes fournisseurs/ }).click();
  await expect(premiereLigne).toBeVisible();

  // Mise en scène : la commande doit partir d'un état connu. Si elle est déjà datée, on la
  // ramène au brouillon avant de commencer.
  if (
    await premiereLigne
      .getByText('ENVOYÉE')
      .isVisible()
      .catch(() => false)
  ) {
    await menu().click();
    await page.getByRole('button', { name: /Annuler l'envoi/ }).click();
    await page
      .locator('.modal-content')
      .getByRole('button', { name: /Oui|Confirmer/ })
      .click();
  }

  await etape(1, async () => {
    // Tant que rien n'est daté, la commande est un brouillon : elle n'engage rien, et le
    // réassort ne la compte pas.
    await expect(premiereLigne.getByText('BROUILLON')).toBeVisible();
  });

  await etape(2, async () => {
    // Le canal est demandé, pas deviné : c'est lui qui dira où retrouver la trace de l'envoi
    // si le grossiste prétend n'avoir rien reçu.
    await menu().click();
    await page.getByRole('button', { name: /Marquer comme envoyée/ }).click();
    const modale = page.locator('app-marquer-envoi-modal');
    await expect(modale).toBeVisible();
    await modale.getByRole('button', { name: /^Téléphone$/ }).click();
    await modale.getByRole('button', { name: /Marquer comme envoyée/ }).click();

    await expect(premiereLigne.getByText('ENVOYÉE')).toBeVisible();
    await expect(premiereLigne.getByText('BROUILLON')).toHaveCount(0);
  });

  await etape(3, async () => {
    // Ce que le grossiste a reçu ne doit plus bouger chez nous, sans quoi le bon imprimé et la
    // commande en base cessent de dire la même chose. Le verrou ne valait que pour l'envoi EDI,
    // seul envoi que l'application constatait ; il vaut maintenant pour tous les canaux.
    await premiereLigne.dblclick();
    const saisie = page.locator('app-commande-requested');
    await expect(saisie).toBeVisible();
    await expect(saisie.locator('.cr-banner--locked')).toContainText(/modifications désactivées/i);
    // La barre d'ajout de produit est neutralisée : c'est elle qui ferait entrer une ligne que
    // le grossiste n'a jamais reçue.
    await expect(saisie.locator('.add-product-bar')).toHaveClass(/section-locked/);

    // La réception, elle, reste ouverte : une commande envoyée est précisément celle qu'on
    // réceptionne.
    await expect(saisie.getByRole('button', { name: /Créer le bon/ })).toBeVisible();

    // PAS `page.goBack()` : l'onglet actif ne vit pas dans l'URL, et le retour navigateur
    // rend l'écran sur son onglet par défaut — la liste n'est alors plus montée du tout.
    // Le bouton de l'application est le seul chemin qui ramène à la liste où l'on était.
    await page.getByRole('button', { name: 'Retour à la liste' }).click();
    await expect(premiereLigne).toBeVisible();
  });

  await etape(4, async () => {
    // Un marquage se défait : on exporte un bon aussi bien pour l'envoyer que pour le relire,
    // et une commande datée par erreur sortirait à tort de la file des brouillons. C'est aussi
    // ce qui empêche le verrou d'enfermer qui que ce soit.
    await menu().click();
    await page.getByRole('button', { name: /Annuler l'envoi/ }).click();
    await page
      .locator('.modal-content')
      .getByRole('button', { name: /Oui|Confirmer/ })
      .click();

    await expect(premiereLigne.getByText('BROUILLON')).toBeVisible();
  });
});
