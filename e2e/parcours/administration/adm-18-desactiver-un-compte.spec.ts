import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Supprimer un compte et le désactiver ne répondent pas à la même situation : le premier
 * geste est définitif, le second se défait. Un collaborateur en congé, un intérimaire dont le
 * contrat s'achève, un compte dont on doute — on coupe l'accès, on garde la fiche, et
 * l'historique des ventes reste attaché à son nom.
 *
 * Deux garde-fous encadrent l'action, et l'écran les applique sans rien expliquer : on ne
 * peut pas se désactiver soi-même, et le compte de l'administrateur principal n'offre pas le
 * bouton du tout — sa désactivation fermerait l'application à tout le monde.
 *
 * Parcours ÉCRIVANT dans la base : il réactive le compte qu'il a désactivé.
 */
scenario('ADM-18', async ({ etape, page }) => {
  const login = 'kkone';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const ligne = () => lignes.filter({ hasText: login }).first();
  // Le champ conserve sa valeur quand la liste se recharge : la remplir à l'identique
  // n'émettrait aucun événement et ne filtrerait donc rien. On le vide d'abord.
  /**
   * Refait la recherche jusqu'à voir l'état attendu.
   *
   * Après un changement d'état la liste se RECHARGE : le filtre appliqué à l'écran disparaît
   * sans que le champ ne se vide, et une recherche lancée pendant ce rechargement se perd.
   * On la refait donc jusqu'à lire l'état voulu — c'est aussi ce que ferait un utilisateur.
   */
  /**
   * Amène le compte à l'état voulu, et s'en assure.
   *
   * Deux difficultés se cumulent ici, et une seule tentative ne suffit pas. La liste se
   * RECHARGE après chaque changement d'état : le filtre saisi disparaît de la liste sans que
   * le champ ne se vide, et la ligne visée est remplacée — un clic parti juste avant ce
   * remplacement se perd sans rien signaler. On refait donc la recherche, on relit l'état, et
   * on ne clique que s'il reste à changer. C'est aussi ce que ferait un utilisateur devant un
   * écran qui n'a pas réagi.
   */
  const basculerVers = async (etat: 'Actif' | 'Inactif'): Promise<void> => {
    const bouton = etat === 'Actif' ? 'Activer' : 'Désactiver';
    const champ = page.getByPlaceholder('Rechercher…');
    // Texte EXACT, et non `hasText` : la correspondance de Playwright est insensible à la
    // casse et cherche une sous-chaîne — « Actif » se retrouverait dans « Inactif », et le
    // parcours croirait le compte actif alors qu'il ne l'est pas.
    const etatAffiche = () => ligne().getByText(etat, { exact: true }).first();
    for (let essai = 0; essai < 8; essai++) {
      // Le champ garde sa valeur quand la liste se recharge : le remplir à l'identique
      // n'émettrait aucun événement et ne filtrerait donc rien. On le vide d'abord.
      await champ.fill('');
      await champ.fill(login);
      if (await etatAffiche().isVisible().catch(() => false)) {
        return;
      }
      await ligne()
        .getByRole('button', { name: bouton, exact: true })
        .click({ timeout: 3000 })
        .catch(() => undefined);
      await page.waitForTimeout(600);
    }
    await expect(etatAffiche()).toBeVisible();
  };

  await etape(1, async () => {
    await page.goto('/admin/user-management');
    // Le compte doit être ACTIF au départ : un parcours précédent a pu le laisser autrement.
    await basculerVers('Actif');
    // Le libellé du bouton dit ce qui va se passer, pas l'état courant : « Désactiver » sur
    // un compte actif, « Activer » sur un compte qui ne l'est plus.
    await expect(ligne().getByRole('button', { name: 'Désactiver', exact: true })).toBeVisible();
  });

  await etape(2, async () => {
    // La liste se RECHARGE après le changement d'état, et repart donc sans le filtre : on
    // refait la recherche pour retrouver le compte. Sans cela, on croirait l'opération sans
    // effet — la ligne a simplement quitté l'écran.
    await basculerVers('Inactif');
    // L'état a basculé, et le bouton avec lui : c'est désormais « Activer » qui est proposé.
    await expect(ligne().getByRole('button', { name: 'Activer', exact: true })).toBeVisible();
  });

  await etape(3, async () => {
    await basculerVers('Actif');
  });
});
