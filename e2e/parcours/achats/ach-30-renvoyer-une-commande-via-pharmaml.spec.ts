import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une commande transmise électroniquement peut avoir à repartir : le grossiste n'a rien reçu,
 * la liaison a coupé au mauvais moment, ou l'on a corrigé des quantités après un premier envoi.
 *
 * Le renvoi n'est pas une nouvelle commande — c'est le MÊME bon qui repart, avec sa référence.
 * Créer un doublon ferait livrer deux fois.
 *
 * L'écran s'en souvient : une commande déjà transmise n'offre plus « Envoyer via PharmaML »
 * mais « Voir réponse », et c'est depuis cette réponse qu'on constate ce que le grossiste a
 * réellement enregistré avant de décider de renvoyer.
 *
 * Parcours en LECTURE, et c'est une contrainte ferme : chaque envoi part chez un vrai
 * grossiste. Le parcours ouvre le menu sans transmettre.
 */
scenario('ACH-30', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Commandes fournisseurs/ }).click();
    await expect(lignes.first()).toBeVisible();
    // Le double-clic ouvre le bon : c'est depuis lui que l'échange électronique se pilote.
    await lignes.first().dblclick();
    await expect(page.locator('#main-content')).toContainText('Retour à la liste');
  });

  await etape(2, async () => {
    // Le bon porte les deux sens de l'échange : ce qu'on transmet, et ce que le grossiste a
    // répondu. Une commande déjà partie propose la réponse plutôt qu'un second envoi.
    await expect(
      page.getByRole('button', { name: /Envoyer via PharmaML|Voir réponse|Importer réponse/ }).first(),
    ).toBeVisible();
  });
});
