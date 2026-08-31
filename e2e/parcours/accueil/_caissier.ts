import { expect, Page } from '@playwright/test';
import { seConnecterEnTantQue } from '../../src/actions';
import { IDENTIFIANTS } from '../../src/config';

/**
 * Le tableau de bord Caissier n'a pas d'adresse : on n'y navigue pas, on l'obtient en ÉTANT
 * caissier. Il est le résultat de la résolution décrite en HOME-01 — d'où cette ouverture de
 * session, commune aux six parcours du profil.
 *
 * KOFFI KONÉ tient la caisse dans le jeu de démonstration ; les comptes y partagent le mot de
 * passe de l'administrateur.
 */
export async function ouvrirLaSessionCaissier(page: Page): Promise<void> {
  await seConnecterEnTantQue(page, 'kkone', IDENTIFIANTS.motDePasse);
  await expect(page.locator('.quick-actions')).toBeVisible({ timeout: 20000 });
}
