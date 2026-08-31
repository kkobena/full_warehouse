import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * En rupture sur une référence, le grossiste propose souvent un équivalent. Le prendre ou non
 * n'est pas anodin : le substitut a son propre prix, sa propre présentation, et le patient
 * attend parfois précisément la marque prescrite.
 *
 * Le protocole distingue trois cas, et l'écran les affiche tels quels :
 *
 *   • EP — Équivalent Proposé : le grossiste demande l'accord. C'est le seul cas où l'on
 *     décide, et le produit n'entre dans la commande qu'après acceptation ;
 *   • EL — Équivalent Livré, et RL — Remplaçant Livré : la substitution est déjà faite, le
 *     carton est parti. Il n'y a plus qu'à en prendre acte.
 *
 * Refuser une EP retire simplement la ligne : rien ne sera livré, et l'on commande ailleurs.
 *
 * Parcours en LECTURE : il montre l'arbitrage sans l'exercer, une acceptation modifiant la
 * commande transmise.
 */
scenario('ACH-33', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Commandes fournisseurs/ }).click();
    const lignes = page.locator('tbody tr').filter({ visible: true });
    await expect(lignes.first()).toBeVisible();

    // Seule une commande TRANSMISE peut porter des substitutions : elles viennent de la
    // réponse du grossiste. La liste la signale par sa marque « PHARMAML ».
    // Le repère est la PASTILLE « PHARMAML », pas le texte de la ligne : `hasText` cherche
    // dans tout le contenu et retenait la première ligne venue.
    await lignes
      .filter({ has: page.locator('span.pharma-badge', { hasText: 'PHARMAML' }) })
      .first()
      .dblclick();
    await expect(page.locator('#main-content')).toContainText('Retour à la liste');
    // Le compteur de substitutions en attente s'affiche à côté du bon, sans qu'on ait à
    // chercher : c'est une décision qui bloque la livraison.
    await expect(page.getByRole('button', { name: /subst\./ })).toBeVisible({ timeout: 15000 });
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: /subst\./ }).click();
    const modale = page.locator('.modal-content:visible');
    await expect(modale).toContainText('Substitutions proposées par le grossiste');
    // Ce que l'on compare pour décider : le produit demandé et celui qu'on propose à la
    // place, chacun avec son code.
    await expect(modale).toContainText('Produit original');
    await expect(modale).toContainText('Substitut proposé');
    // Et la raison invoquée par le répartiteur, qui n'est pas toujours la rupture.
    await expect(modale).toContainText('Raison répartiteur');
  });
});
