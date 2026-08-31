import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Les filtres fins — officine, emplacement, rayon, fournisseur, famille — sont repliés par
 * défaut derrière un bouton. Sur 198 lots à traiter, c'est pourtant le seul moyen de rendre
 * la liste actionnable : on ne traite pas les péremptions de toute l'officine d'un bloc, on
 * les traite rayon par rayon.
 */
scenario('STK-32', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await expect(lignes.first()).toBeVisible();
    await page.getByRole('button', { name: /Filtres avancés/ }).click();
    // Le bouton devient « Masquer filtres » : c'est la preuve que le panneau est déployé,
    // et elle ne dépend d'aucune classe CSS.
    await expect(page.getByRole('button', { name: /Masquer filtres/ })).toBeVisible();
  });

  await etape(2, async () => {
    // Le choix relance la recherche de lui-même : il n'y a pas de bouton « Appliquer » ici.
    await choisirDansSelect(page, 'rayons', 'ANTIBIOTIQUES');
  });

  await etape(3, async () => {
    // « Tous les critères sélectionnés » se vérifie par l'absence des autres rayons, pas par
    // la présence du bon : la liste non filtrée contient déjà des lots d'antibiotiques.
    await expect(lignes.first()).toContainText('ANTIBIOTIQUES');
    await expect(lignes.filter({ hasText: 'CARDIOLOGIE' })).toHaveCount(0);
    await expect(lignes.filter({ hasText: 'HOMEOPATHIE' })).toHaveCount(0);
  });
});
