/**
 * Connexion unique, rejouée ensuite par tous les parcours via `storageState`.
 *
 * Le jeton JWT est rangé dans `localStorage` (app/core/auth/jwt-token.service.ts), que
 * `storageState` sait enregistrer et restaurer — d'où l'économie : une seule traversée de
 * l'écran de connexion pour toute la campagne, au lieu d'une par parcours.
 *
 * La connexion passe délibérément par l'interface plutôt que par un appel direct à l'API :
 * c'est le seul écran que tout le monde voit, et une régression dessus doit arrêter la
 * campagne tout de suite plutôt que produire soixante captures d'écrans vides.
 */
import { expect, test as setup } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { FICHIER_SESSION, IDENTIFIANTS } from '../src/config';

setup("ouvrir une session et l'enregistrer", async ({ page }) => {
  await page.goto('/login');

  // `data-cy` : les seuls points d'ancrage stables existant déjà dans l'application (écrans
  // hérités du générateur). Les écrans métier n'en ont pas — cf. §5.1 du plan.
  await page.locator('[data-cy="username"]').fill(IDENTIFIANTS.utilisateur);
  // Le mot de passe passe par le composant maison `app-password` : l'attribut porte sur
  // l'hôte, la saisie se fait dans l'input qu'il encapsule.
  await page.locator('[data-cy="password"] input').fill(IDENTIFIANTS.motDePasse);
  await page.locator('[data-cy="submit"]').click();

  // Message d'erreur explicite plutôt qu'un délai d'attente dépassé sur l'assertion suivante :
  // des identifiants faux sont la cause la plus fréquente, et la moins lisible.
  const erreur = page.locator('[data-cy="loginError"]');
  await expect(
    erreur,
    `Connexion refusée pour « ${IDENTIFIANTS.utilisateur} ». ` +
      'Renseigner E2E_USER / E2E_PASSWORD pour le jeu de démonstration.',
  ).toBeHidden();

  // La session est établie quand l'application a quitté l'écran de connexion.
  await expect(page).not.toHaveURL(/\/login/);

  mkdirSync(dirname(FICHIER_SESSION), { recursive: true });
  await page.context().storageState({ path: FICHIER_SESSION });
});
