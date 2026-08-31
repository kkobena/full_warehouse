import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Entre le retrait du stock et la destruction physique, les lots attendent — parfois des
 * mois, le temps de réunir un volume qui justifie le passage du prestataire. Cet écran est
 * cette salle d'attente : ce qui est sorti du stock vendable et n'a pas encore été détruit.
 *
 * Il est chiffré, et c'est ce qui en fait autre chose qu'une liste : nombre de produits,
 * quantités, valeur d'achat et valeur de vente. La première est la perte réelle, la seconde
 * le manque à gagner — les deux se présentent différemment devant un assureur ou un
 * comptable.
 *
 * Les lots déjà détruits restent visibles avec leur date : le procès-verbal (STK-45) se
 * reconstitue à tout moment.
 *
 * Parcours en LECTURE.
 */
scenario('STK-42', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/gestion-peremption');
    await ouvrirOnglet(page, /Lots à détruire/);
    await expect(page.locator('tbody tr').first()).toBeVisible();
  });

  await etape(2, async () => {
    // Le résumé chiffré : ce que la destruction coûte, des deux points de vue.
    await expect(contenu).toContainText(/valeur/i);
    await expect(page.locator('app-kpi-item, .kpi-item').first()).toBeVisible();
  });

  await etape(3, async () => {
    // Le statut de péremption colore la liste et se lit en clair sur chaque ligne.
    await expect(contenu).toContainText('Statut');
  });
});
