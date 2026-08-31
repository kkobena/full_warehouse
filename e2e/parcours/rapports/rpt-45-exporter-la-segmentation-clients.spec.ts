import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';
import { ouvrirRapport } from './_sections';

/**
 * Une segmentation ne sert que si elle sort de l'écran : on cible une action commerciale à
 * plusieurs, et la liste des clients concernés doit pouvoir circuler.
 *
 * L'export reprend le périmètre FILTRÉ — les champions seuls, ou les clients à risque — et non
 * le fichier entier. C'est ce qui distingue un export d'un vidage de base.
 */
scenario('RPT-45', async ({ etape, page }) => {
  await etape(1, async () => {
    await ouvrirRapport(page, 'partners', 'Segmentation Clients');
    await expect(page.locator('#main-content')).toContainText(/CHAMPIONS/i, { timeout: 20000 });
  });

  await etape(2, async () => {
    // Le bouton avait été commenté dans le gabarit alors que l'export existait de bout en
    // bout, service et endpoint compris : la segmentation n'avait aucune sortie. Et le
    // gabarit Thymeleaf du document manquait par ailleurs — d'où un export réellement
    // déclenché ici, et pas seulement constaté.
    const reponse = page.waitForResponse(
      r => r.url().includes('/api/customers/segmentation/export') && r.request().method() === 'GET',
    );
    await page.getByRole('button', { name: 'Exporter PDF' }).click();
    const pdf = await reponse;
    expect(pdf.status(), "L'export PDF de la segmentation a échoué.").toBe(200);
    expect((await pdf.body()).subarray(0, 4).toString('latin1')).toBe('%PDF');
  });
});
