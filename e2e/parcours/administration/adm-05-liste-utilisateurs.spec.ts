import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La liste des utilisateurs est aussi le tableau des responsabilités : qui a le droit de
 * faire quoi se lit dans sa colonne « Rôle ». C'est le premier écran à ouvrir quand une
 * opération a été refusée à quelqu'un.
 */
scenario('ADM-05', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/admin/user-management');
    await expect(page.getByRole('columnheader', { name: 'Rôle' })).toBeVisible();
    // Plusieurs comptes, sans quoi le filtre de l'étape suivante n'aurait rien à écarter.
    await expect(lignes.nth(1)).toBeVisible();
  });

  await etape(2, async () => {
    // Le champ de recherche est logé dans l'en-tête du tableau, à droite — pas dans une
    // barre d'outils : c'est ce que l'image doit montrer.
    await page.getByPlaceholder('Rechercher').fill('kone');
    // Le filtre promet d'ÉCARTER : « admin » ne doit plus être là. Vérifier la seule
    // présence de KONE ne prouverait rien, il était déjà dans la liste complète.
    await expect(lignes).toHaveCount(1);
    await expect(lignes.first()).toContainText(/KONE/i);
  });
});
