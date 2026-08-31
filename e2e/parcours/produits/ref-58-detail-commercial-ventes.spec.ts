import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le même onglet que REF-10, lu autrement : non plus la courbe, mais la LIGNE À LIGNE. Chaque
 * vente du produit y figure avec son prix unitaire pratiqué, sa remise, son montant TTC, sa
 * TVA, son net et l'opérateur qui l'a encaissée.
 *
 * C'est ce qui permet de répondre à « pourquoi la marge de ce produit a-t-elle baissé ce
 * mois-ci ? » : le total ne le dit pas, la colonne des remises si.
 *
 * Parcours en LECTURE : il produit un PDF, il ne modifie rien.
 */
scenario('REF-58', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-ventes-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Ventes');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    await onglet.getByRole('button', { name: '1 an' }).click();
    await onglet.getByRole('button', { name: 'Actualiser' }).click();
    await expect(onglet.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    // Les colonnes commerciales : c'est leur présence qui distingue ce relevé d'un simple
    // historique de quantités.
    await expect(onglet).toContainText('Prix unit.');
    await expect(onglet).toContainText('Remise');
    await expect(onglet).toContainText('Montant TTC');
    await expect(onglet).toContainText('TVA');
    await expect(onglet).toContainText('Opérateur');
    // Et la ligne de totaux, qui recoupe ce que la synthèse annonce.
    await expect(onglet).toContainText(/Totaux/i);
  });

  await etape(4, async () => {
    const telechargement = page.waitForEvent('download');
    await onglet.getByRole('button', { name: 'PDF' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
