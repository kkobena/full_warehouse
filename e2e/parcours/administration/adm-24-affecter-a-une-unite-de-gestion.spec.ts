import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Une officine peut tenir, au même endroit, une unité qui a sa propre caisse et son propre
 * stock : une parapharmacie. Encore faut-il que quelqu'un puisse y travailler — et c'est ce
 * champ, « Magasin d'affectation », qui le permet.
 *
 * Il n'apparaît QUE s'il y a plus d'un magasin où affecter. Dans l'immense majorité des
 * installations — une officine seule — il n'y a rien à choisir, et un champ à une seule
 * valeur serait du bruit. Ce parcours ne peut donc se jouer que si le jeu de démonstration
 * porte une unité de gestion : il le vérifie en base AVANT d'ouvrir l'écran, plutôt que de
 * laisser le champ manquant passer pour une régression.
 *
 * Deux magasins sont proposés, jamais trois : le DÉPÔT est écarté. On n'y saisit rien, il n'a
 * pas de caisse, et un compte affecté là y ouvrirait des sessions dans un établissement qui
 * n'en tient pas. Le refus est aussi tenu côté serveur — l'écran ne le propose pas, et
 * l'enregistrement le refuserait de toute façon.
 *
 * Parcours ÉCRIVANT dans la base : il supprime le compte qu'il crée.
 */
scenario('ADM-24', async ({ etape, page }) => {
  // Le login porte l'horodatage : il est UNIQUE par base, et le formulaire refuse un doublon.
  const login = 'akone' + Date.now().toString().slice(-6);
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  const unite = lire(`SELECT name FROM magasin WHERE type_magasin = 'UNITE_GESTION' ORDER BY id LIMIT 1`);
  expect(unite, 'la démo doit porter une unité de gestion rattachée à l’officine').not.toBe('');
  const depot = lire(`SELECT COALESCE(name, '') FROM magasin WHERE type_magasin = 'DEPOT' ORDER BY id LIMIT 1`);

  /** Le magasin du compte, lu en base : l'écran ne réaffiche pas l'affectation dans la liste. */
  const magasinDuCompte = (): string =>
    lire(`SELECT COALESCE(m.name, '(aucun)') FROM app_user u
            LEFT JOIN magasin m ON m.id = u.magasin_id WHERE u.login = '${login}'`);

  try {
    await etape(1, async () => {
      // On passe par la liste plutôt que d'ouvrir le formulaire directement : « Enregistrer »
      // revient à l'écran PRÉCÉDENT, et une arrivée en lien direct ferait sortir de
      // l'application.
      await page.goto('/admin/user-management');
      await page.getByRole('button', { name: 'Nouvel utilisateur' }).click();
      await expect(page.locator('#field_login')).toBeVisible();
    });

    await etape(2, async () => {
      await page.locator('#field_login').fill(login);
      await page.locator('#field_lastName').fill('KONE');
      await page.locator('#field_firstName').fill('AMINATA');
      await expect(page.locator('#field_login')).toHaveValue(login);
    });

    await etape(3, async () => {
      await choisirDansSelect(page, 'field_authority', 'Vendeur');

      // Le champ n'existe que parce qu'il y a plus d'un magasin : son absence serait un
      // défaut, pas une variante d'affichage.
      const selecteur = page.locator('ng-select', { has: page.locator('#field_magasin') });
      await expect(selecteur, 'le choix du magasin doit s’offrir dès qu’une unité est rattachée').toBeVisible();

      // Ce que la liste propose, et ce qu'elle ne propose pas : on la déplie et on lit.
      await page.locator('#field_magasin').focus();
      await page.locator('#field_magasin').press('ArrowDown');
      const options = page.locator('.ng-option');
      await expect(options.first()).toBeVisible();
      const proposes = (await options.allInnerTexts()).map(t => t.trim());
      expect(proposes, 'l’unité de gestion doit être proposée').toContain(unite);
      if (depot) {
        expect(proposes, 'un dépôt ne s’affecte à personne : il n’a ni caisse ni saisie').not.toContain(depot);
      }
      await options.filter({ hasText: unite }).first().click();
      await expect(selecteur).toContainText(unite);
    });

    await etape(4, async () => {
      await page.getByRole('button', { name: 'Enregistrer' }).click();
      await expect(page).toHaveURL(/user-management$/);
      // La liste est paginée : on retrouve le compte par la recherche, comme n'importe qui.
      await page.getByPlaceholder('Rechercher…').fill(login);
      const nouveau = lignes.filter({ hasText: login });
      await expect(nouveau).toContainText(/vendeur/i);
      await expect(nouveau).toContainText(/actif/i);
      // La liste n'affiche pas le magasin : sans ce contrôle, le parcours ne dirait rien de
      // ce qu'il prétend montrer — un compte affecté ailleurs qu'à l'officine.
      expect(magasinDuCompte(), 'le compte doit être rattaché à l’unité de gestion').toBe(unite);
    });
  } finally {
    // ── Remise en état : le compte créé ne doit pas survivre au parcours. `app_user` est
    //    préservée par la remise à zéro du jeu de démonstration — un compte oublié ici
    //    resterait indéfiniment.
    await page.goto('/admin/user-management').catch((): void => undefined);
    await page
      .getByPlaceholder('Rechercher…')
      .fill(login)
      .catch((): void => undefined);
    const aSupprimer = lignes.filter({ hasText: login }).first();
    const present = await aSupprimer
      .waitFor({ state: 'visible', timeout: 10_000 })
      .then(() => true)
      .catch(() => false);
    if (present) {
      await aSupprimer.getByRole('button', { name: 'Supprimer' }).click({ timeout: 5_000 });
      await expect(modale).toBeVisible({ timeout: 5_000 });
      await modale
        .getByRole('button', { name: /Supprimer|Oui/ })
        .first()
        .click({ timeout: 5_000 });
      await expect(modale).toBeHidden({ timeout: 10_000 });
    }
    const reste = lire(`SELECT count(*) FROM app_user WHERE login = '${login}'`);
    if (reste !== '0') {
      throw new Error(
        `Remise en état INCOMPLÈTE : le compte « ${login} » existe toujours. Le supprimer depuis ` +
          'la liste des utilisateurs avant de relancer.',
      );
    }
  }
});
