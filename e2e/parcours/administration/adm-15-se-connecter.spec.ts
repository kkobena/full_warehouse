import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { IDENTIFIANTS } from '../../src/config';

/**
 * Scénario AJOUTÉ au cahier de recette : la connexion n'y figurait pas, alors qu'elle est le
 * seul écran que TOUS les utilisateurs voient, et le premier du manuel.
 *
 * Le parcours commence par se déconnecter — la campagne rejoue une session enregistrée, il
 * faut donc la défaire pour photographier l'écran d'accueil tel que le voit un pharmacien qui
 * arrive le matin.
 */
scenario('ADM-15', async ({ etape, page }) => {
  await etape(1, async () => {
    await page.goto('/login');
    // La session rejouée vit dans le stockage local (jeton JWT) : l'effacer, puis recharger,
    // ramène l'application à l'état « personne n'est connecté ».
    await page.evaluate(() => localStorage.clear());
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Bienvenue' })).toBeVisible();

    await page.locator('[data-cy="username"]').fill(IDENTIFIANTS.utilisateur);
    // Le mot de passe passe par `app-password` : l'attribut est sur l'hôte, la saisie dans
    // l'`<input>` qu'il encapsule.
    await page.locator('[data-cy="password"] input').fill(IDENTIFIANTS.motDePasse);
    // Les deux champs sont renseignés — c'est ce que l'image doit montrer, le mot de passe
    // restant masqué.
    await expect(page.locator('[data-cy="username"]')).toHaveValue(IDENTIFIANTS.utilisateur);
  });

  await etape(2, async () => {
    await page.locator('[data-cy="submit"]').click();
    // Quitter l'écran de connexion est la seule preuve que le serveur a accepté : le message
    // d'erreur, lui, reste sur place.
    await expect(page).not.toHaveURL(/\/login/);
  });

  await etape(3, async () => {
    // Le tableau de bord du profil, et non une page vide : un indicateur chiffré le prouve.
    await expect(page.locator('.kpi-strip-item').filter({ hasText: 'CA Net' })).toContainText(/\d/);
    // Et l'identité de la session dans la barre de navigation — ce qui distingue une session
    // ouverte d'un écran public.
    await expect(page.getByRole('button', { name: /Administrator/i })).toBeVisible();
  });
});
