import { expect } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Au comptoir, la souris coûte des secondes à chaque vente, et les secondes font la file
 * d'attente. L'écran de vente répond donc au clavier : une touche pour la recherche produit,
 * une pour la quantité, une pour le client, une pour finaliser.
 *
 * Deux familles cohabitent, et la nuance compte : les touches F1–F10 et les Alt+lettre
 * marchent PARTOUT ; les combinaisons Ctrl+lettre sont réservées par le navigateur et ne
 * fonctionnent que dans l'application de bureau. L'aide le dit elle-même — elle affiche un
 * badge « Mode Web » ou « Mode Desktop » et n'énumère que ce qui est réellement disponible.
 *
 * Parcours ÉCRIVANT dans la base : il constitue une vente au clavier, puis l'abandonne.
 */
scenario('VTE-48', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await expect(page.locator('#produitbox')).toBeVisible();
  });

  await etape(2, async () => {
    // Une vente montée sans souris : la recherche produit a le focus dès l'ouverture, la
    // validation d'une suggestion enchaîne sur la quantité, et `Entrée` ajoute la ligne.
    await chercherProduit(page, produit);
    const quantite = page.locator('#quantiteSaisie');
    await expect(quantite).toBeFocused();
    await quantite.press('Control+a');
    await quantite.pressSequentially('2', { delay: 40 });
    await quantite.press('Enter');
    await expect(lignes.first()).toContainText(produit);
    await expect(contenu).toContainText('38 620');
  });

  await etape(3, async () => {
    // F1 ouvre l'aide, où les raccourcis sont regroupés par usage. Le badge du titre dit
    // dans quel environnement on se trouve : ici un navigateur, donc « Mode Web » — et les
    // raccourcis Ctrl+lettre, propres au bureau, sont signalés comme tels.
    await page.keyboard.press('F1');
    await expect(modale).toContainText('Raccourcis Clavier');
    await expect(modale).toContainText('Mode Web');
    await expect(modale).toContainText('F1');
    // L'aide reste OUVERTE à la fin de l'étape : c'est elle qu'il faut photographier. La
    // fermer ici rendrait l'image identique à celle de l'étape précédente.
  });

  // ── Remise en état : l'aide est refermée, la vente abandonnée. ──────────────────────────
  await modale.getByRole('button', { name: /Fermer|Close/i }).first().click();
  await expect(modale).toBeHidden();
  await page.locator('app-sale-actions').getByRole('button', { name: 'Annuler' }).click();
  await expect(modale).toBeVisible();
  await modale.getByRole('button', { name: 'Oui' }).click();
});
