import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'inventaire clôturé quitte l'application dès qu'il faut le montrer : à l'expert-comptable,
 * au pharmacien titulaire, ou simplement à celui qui recomptera le rayon litigieux avec la
 * feuille en main.
 *
 * L'export reprend, produit par produit, la quantité théorique, la quantité comptée et
 * l'écart — figés à la clôture, et non recalculés : c'est la photographie du comptage, pas
 * l'état du stock d'aujourd'hui.
 *
 * Parcours en LECTURE des données — il n'écrit rien — mais il produit bien le document : un
 * export qui échoue rend un fichier vide d'apparence normale, et le service a longtemps ravalé
 * ses erreurs pour renvoyer une liste vide. Vérifier que le bouton existe ne disait rien de ça.
 */
scenario('STK-17', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/inventaire');
    await ouvrirOnglet(page, /Clôturés/);
    await expect(lignes.first()).toContainText(/clôturé/i);
    await lignes.first().getByRole('button', { name: 'Exporter PDF' }).click();
    await expect(modale).toBeVisible();
  });

  await etape(2, async () => {
    // Avant de produire le document, l'écran demande ce qu'il doit contenir.
    await expect(modale).toContainText('Grouper par');
    await expect(modale).toContainText('Filtre lignes');
    await expect(modale.getByRole('button', { name: 'Exporter PDF' })).toBeVisible();

    // Le document est réellement produit : c'est la seule assertion qui traverse la requête
    // d'export jusqu'au rendu PDF.
    const telechargement = page.waitForEvent('download');
    await modale.getByRole('button', { name: 'Exporter PDF' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
