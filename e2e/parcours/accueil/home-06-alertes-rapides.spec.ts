import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le premier écran que voit un pharmacien : `/` résout le tableau de bord de son rôle. Sa
 * particularité est qu'il ne montre QUE les alertes non nulles — un compteur à zéro n'a pas
 * de pastille du tout. C'est ce qui rend la bande lisible d'un coup d'œil, et c'est aussi ce
 * qui surprend : l'absence d'une pastille est une bonne nouvelle, pas un écran incomplet.
 */
scenario('HOME-06', async ({ etape, page }) => {
  // Les pastilles sont des `<button>` sans rôle particulier ; on les cible par leur classe,
  // faute d'un libellé accessible distinct du compteur qu'elles portent.
  const pastille = (libelle: string) => page.locator('.quick-alert-card').filter({ hasText: libelle });

  await etape(1, async () => {
    await page.goto('/');
    await expect(page.locator('.kpi-strip-item').filter({ hasText: 'CA Net' })).toContainText(/\d/);
  });

  await etape(2, async () => {
    await expect(pastille('Péremptions')).toContainText(/\d/);
    await expect(pastille('Ruptures')).toContainText(/\d/);
    await expect(pastille('Ajustements')).toContainText(/\d/);
    // Le résultat attendu du modèle, mot pour mot : aucune pastille à zéro n'encombre
    // l'accueil. C'est vérifiable, et c'est la seule chose que la légende promet ici.
    await expect(page.locator('.quick-alert-badge').filter({ hasText: /^0$/ })).toHaveCount(0);
  });

  await etape(3, async () => {
    await pastille('Péremptions').click();
    // Le raccourci n'ouvre pas une liste filtrée « maison » : il envoie sur l'écran de
    // traitement des péremptions, celui-là même que couvre STK-18.
    await expect(page).toHaveURL(/gestion-peremption/);
    await expect(page.getByRole('tab', { name: /Produits périmés/ })).toBeVisible();
  });
});
