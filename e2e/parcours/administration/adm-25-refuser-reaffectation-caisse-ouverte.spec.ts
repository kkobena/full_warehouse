import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Un compte se réaffecte : quelqu'un passe de l'officine à la parapharmacie. Mais pas avec une
 * caisse ouverte — sinon la recette resterait à compter dans un magasin où l'intéressé ne
 * travaille plus, et personne ne saurait plus à qui en demander le billetage.
 *
 * Le refus vient du SERVEUR, pas du formulaire : l'écran propose le changement, c'est
 * l'enregistrement qui le rejette, avec un message qui dit quoi faire. C'est le bon partage —
 * l'écran n'a pas à connaître l'état des caisses de tous les magasins pour griser un champ.
 *
 * ── Ce que ce parcours a fait corriger ──────────────────────────────────────────────────────
 * Le refus arrivait en HTTP 500 « Une erreur interne est survenue » : la règle de gestion était
 * bien tenue — rien n'était écrit — mais l'utilisateur ne l'apprenait pas, et le serveur
 * journalisait son propre refus comme un défaut du logiciel. Les quatre refus d'affectation
 * lèvent désormais l'exception métier, qui arrive en 400 avec son message à l'écran.
 *
 * Parcours en LECTURE : le refus est précisément ce qui garantit qu'il n'écrit rien. Il le
 * vérifie quand même en base — si le refus tombait, le caissier changerait de magasin et sa
 * caisse ouverte deviendrait orpheline.
 */
scenario('ADM-25', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const alertes = page.locator('.alerts');

  // Le sujet est pris en base : le caissier dont la caisse est OUVERTE. Figer un login
  // rendrait le parcours dépendant du compte qui a servi la dernière journée du jeu.
  const sujet = lire(`SELECT u.login FROM cash_register cr
                        JOIN app_user u ON u.id = cr.user_id
                       WHERE cr.statut = 'OPEN' ORDER BY cr.id LIMIT 1`);
  expect(sujet, 'la démo doit porter une caisse ouverte').not.toBe('');

  const unite = lire(`SELECT name FROM magasin WHERE type_magasin = 'UNITE_GESTION' ORDER BY id LIMIT 1`);
  expect(unite, 'la démo doit porter une unité de gestion où tenter la réaffectation').not.toBe('');

  /** L'affectation et les droits du sujet : ce que le refus doit laisser intact. */
  const etatDuSujet = (): string =>
    lire(`SELECT m.name || ' | ' || COALESCE((SELECT string_agg(ua.authority_name, ',' ORDER BY ua.authority_name)
                                                FROM user_authority ua WHERE ua.user_id = u.id), 'aucun')
            FROM app_user u JOIN magasin m ON m.id = u.magasin_id WHERE u.login = '${sujet}'`);
  const avant = etatDuSujet();
  expect(avant, `état illisible pour « ${sujet} »`).not.toBe('');

  await etape(1, async () => {
    await page.goto('/admin/user-management');
    await page.getByPlaceholder('Rechercher…').fill(sujet);
    await expect(lignes.filter({ hasText: sujet }).first()).toBeVisible();
    // `app-button` rend un <button> même avec un `routerLink` : c'est un bouton qu'on cherche.
    await lignes.filter({ hasText: sujet }).first().getByRole('button', { name: 'Modifier' }).click();
    await expect(page.locator('#field_login')).toHaveValue(sujet);
  });

  await etape(2, async () => {
    // Le champ s'offre en modification comme à la création : un compte se réaffecte.
    await choisirDansSelect(page, 'field_magasin', unite);
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    // Le message, et non le seul échec : un enregistrement qui ne fait rien sans rien dire
    // laisse l'utilisateur recommencer indéfiniment.
    await expect(alertes, 'le refus doit dire qu’il faut faire le billetage').toContainText(/billetage/i);
    // On reste sur la fiche : l'écran ne revient à la liste qu'après un enregistrement réussi.
    await expect(page).not.toHaveURL(/user-management$/);
  });

  // Rien n'a bougé : ni le magasin, ni les droits.
  expect(etatDuSujet(), `« ${sujet} » ne devait pas changer de magasin avec une caisse ouverte`).toBe(avant);
});
