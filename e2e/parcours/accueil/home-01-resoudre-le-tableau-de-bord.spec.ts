import { expect } from '@playwright/test';
import { IDENTIFIANTS } from '../../src/config';
import { scenario } from '../../src/scenario';

/**
 * Le premier écran d'une journée n'est pas le même pour tout le monde : le titulaire veut ses
 * indicateurs, le caissier sa caisse, le responsable achats ses commandes. PharmaSmart ne le
 * demande pas — il le DÉDUIT, à la connexion, dans un ordre fixe :
 *
 *   1. le tableau de bord personnel de l'utilisateur, s'il en a marqué un ;
 *   2. sinon celui marqué par défaut pour l'un de ses rôles ;
 *   3. sinon l'accueil générique, qui ne laisse jamais l'écran vide.
 *
 * Le résultat est mis en cache pour la session : la déduction coûte un appel, pas un par
 * navigation.
 *
 * Ici, l'administrateur n'a pas de tableau de bord personnel : c'est le niveau 2 qui tranche,
 * et le tableau de bord Pharmacien attaché à son rôle qui s'affiche.
 */
scenario('HOME-01', async ({ etape, page }) => {
  await etape(1, async () => {
    // Une connexion pour de bon : la résolution n'a lieu qu'à l'arrivée sur l'accueil, et
    // c'est précisément ce moment que le parcours doit montrer.
    await page.goto('/login');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/login');
    await page.locator('[data-cy="username"]').fill(IDENTIFIANTS.utilisateur);
    await page.locator('[data-cy="password"] input').fill(IDENTIFIANTS.motDePasse);
    await page.locator('[data-cy="submit"]').click();
    await expect(page).not.toHaveURL(/\/login/);
  });

  await etape(2, async () => {
    // Le tableau de bord Pharmacien, reconnaissable à sa bande d'indicateurs : personne ne
    // l'a demandé, il découle du rôle.
    await expect(page.locator('.kpi-strip-item').filter({ hasText: 'CA Net' })).toBeVisible({ timeout: 20000 });
  });
});
