import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un inventaire ne dit pas seulement ce qui manque : il dit ce que vaut le stock. La
 * valorisation globale encadre l'écart de ses deux bornes — la valeur AVANT régularisation,
 * celle d'APRÈS — et donne la différence, au prix d'achat comme au prix de vente.
 *
 * C'est le chiffre qui part en comptabilité : la démarque de l'exercice, celle qu'un
 * pharmacien devra expliquer à son expert-comptable. D'où l'importance de l'avoir qualifiée
 * ligne à ligne (STK-13) plutôt que de la subir en bloc.
 *
 * La ventilation par famille, rayon ou emplacement — le même total sous trois angles — fait
 * l'objet de son propre écran (STK-16).
 *
 * Parcours en LECTURE.
 */
scenario('STK-15', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const valorisation = page.locator('app-inventory-valuation');

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Clôturés/);
    await expect(lignes.first()).toContainText(/clôturé/i);
    await page.getByRole('button', { name: 'Déplier le détail' }).first().click();
  });

  await etape(2, async () => {
    // Les deux bornes et leur différence, au prix d'achat comme au prix de vente.
    await expect(valorisation).toContainText(/Valeur avant \(achat\)/i);
    await expect(valorisation).toContainText(/Valeur après \(achat\)/i);
    await expect(valorisation).toContainText(/Écart coût/i);
  });
});
