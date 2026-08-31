import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le grossiste a livré le bon d'une autre officine. Il n'y a rien à trier : tout repart.
 *
 * Ressaisir ligne à ligne un bon de quarante références serait long et surtout faux — une
 * ligne oubliée resterait au stock sans jamais avoir été reçue. Le retour complet reprend donc
 * automatiquement TOUTES les lignes reçues du bon.
 *
 * Un seul motif est demandé, et c'est cohérent : si le bon entier repart, c'est pour une seule
 * raison. L'écran nomme le bon et son fournisseur avant d'agir, parce qu'un retour complet
 * lancé sur la mauvaise livraison vide un stock qu'on croyait sûr.
 *
 * Parcours en LECTURE : il ouvre la confirmation sans créer le retour.
 */
scenario('ACH-53', async ({ etape, page }) => {
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    const liste = page.locator('app-list-bons');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // Le retour complet n'est offert que sur un bon CLÔTURÉ, c'est-à-dire réellement entré
    // en stock : on ne renvoie pas ce qu'on n'a pas encore saisi. Un bon « en attente de
    // saisie » propose au contraire de le saisir.
    //
    // L'action vit dans le menu de la ligne — les trois points — et non dans un bouton
    // visible, la place manquant sur une table à quinze colonnes.
    const bon = liste.locator('tbody tr').filter({ hasText: 'Clôturé' }).first();
    await bon.getByRole('button', { name: 'Actions' }).click();
  });

  await etape(2, async () => {
    await page.locator('.dropdown-menu.show').getByRole('button', { name: 'Retour complet' }).click();
    await expect(modale).toContainText('Retour complet du bon');
    // Le bon et son fournisseur sont nommés : c'est ce qui évite de vider la mauvaise
    // livraison.
    await expect(modale).toContainText('seront retournées');
  });

  await etape(3, async () => {
    // Un motif unique pour tout le bon, puisqu'une seule raison le renvoie entier.
    await expect(modale.getByRole('button', { name: 'Créer le retour complet' })).toBeVisible();
  });
});
