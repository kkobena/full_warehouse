import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La valorisation d'un inventaire clôturé se lit sur trois colonnes — avant, après, écart —
 * et sous trois angles : emplacement, famille, rayon. Le même écart total s'y répartit
 * différemment, et c'est ce qui permet de dire OÙ il s'est produit. Rien de tout cela
 * n'apparaît tant que l'inventaire n'est pas déplié : la ligne du tableau ne montre que sa
 * date de clôture.
 */
scenario('STK-16', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const valorisation = page.locator('app-inventory-valuation');

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Clôturés/);
    await expect(lignes.first()).toContainText(/clôturé/i);
    // Le détail s'ouvre par le chevron de la ligne — dont le nom accessible a été posé en
    // écrivant ce parcours : les quatre actions de la ligne étaient des boutons sans
    // intitulé, muets pour un lecteur d'écran comme pour Playwright.
    await page.getByRole('button', { name: 'Déplier le détail' }).first().click();
    // La carte de valorisation encadre l'écart de ses deux bornes ; elle n'existe pas tant
    // que l'inventaire n'est pas déplié.
    await expect(valorisation).toContainText(/Valeur avant \(achat\)/i);
    await expect(valorisation).toContainText(/Écart coût/i);
  });

  await etape(2, async () => {
    // Le regroupement par défaut est l'emplacement — un seul groupe ici, donc peu parlant.
    // Le rayon est l'angle qui décompose réellement l'écart.
    await choisirDansSelect(page, 'invVentilation', 'Par rayon');
    // Plusieurs groupes, c'est ce que la ventilation promet : une ligne par rayon
    // inventorié, là où l'emplacement n'en donnait qu'une.
    await expect(page.getByRole('columnheader', { name: 'Groupe' })).toBeVisible();
    // Plusieurs groupes : c'est ce que la ventilation par rayon apporte sur l'emplacement,
    // qui n'en donne qu'un seul.
    await expect(valorisation.locator('tbody tr')).not.toHaveCount(1);
  });
});
