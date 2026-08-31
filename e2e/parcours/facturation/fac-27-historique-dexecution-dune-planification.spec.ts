import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une automatisation qu'on ne peut pas relire n'inspire pas confiance — et à raison : la
 * question n'est pas « la planification est-elle active », mais « la période de mars a-t-elle
 * bien été facturée, et par quelle exécution ».
 *
 * L'historique répond aux deux : chaque déclenchement y figure avec sa période traitée, son
 * issue et, en cas d'échec, le message qui l'explique. C'est là qu'on voit qu'un mois a été
 * sauté, ou traité deux fois.
 */
scenario('FAC-27', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Automatisation/);
    await lignes.first().locator('button:has(.pi-eye)').first().click();
    await expect(contenu).toContainText('Exécutions');
  });

  await etape(2, async () => {
    await page.getByRole('tab', { name: /Exécutions/ }).first().click();
    // Le tableau reste lisible même sans exécution passée : c'est le cas d'une planification
    // qu'on vient d'activer, et l'écran doit le dire plutôt que rester blanc.
    await expect(contenu).toContainText(/Période|Aucune exécution|Statut/);
  });
});
