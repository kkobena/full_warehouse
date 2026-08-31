import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Devant un contrôle, la question n'est pas « la certification est-elle activée » mais
 * « cette facture-là a-t-elle été transmise, et quand ». L'historique répond aux deux, et
 * surtout aux échecs : un envoi refusé par la DGI y figure avec son message, ce qui est la
 * seule façon de savoir qu'il faut corriger les coordonnées d'un organisme et relancer.
 *
 * Sans cette trace, une certification échouée resterait invisible jusqu'au contrôle.
 */
scenario('FAC-35', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    await page.getByRole('tab').filter({ hasText: /Certification FNE/ }).first().click();
    await expect(contenu).toContainText(/Historique des certifications/);
  });

  await etape(2, async () => {
    // Début, fin, statut et message : de quoi distinguer un passage sans rien à faire d'un
    // passage qui a échoué.
    await expect(contenu).toContainText(/Début/);
    await expect(contenu).toContainText(/Statut/);
    await expect(contenu).toContainText(/Message|Aucune exécution/);
  });
});
