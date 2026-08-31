import { expect } from '@playwright/test';
import { chercherAuCatalogue, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le pendant de l'onglet Ventes, côté fournisseur : quand ce produit est-il entré, en quelle
 * quantité, et à quel prix d'achat ? La question se pose à chaque négociation — et à chaque
 * fois qu'un prix d'achat monte sans qu'on s'en aperçoive, la marge s'érode en silence.
 *
 * La colonne « Prix achat » est celle qu'on suit dans le temps : deux réceptions du même
 * produit au même fournisseur n'ont aucune raison d'afficher deux prix différents.
 *
 * Parcours en LECTURE : il produit un PDF, il ne modifie rien.
 */
scenario('REF-59', async ({ etape, page }) => {
  const produit = 'DOLIPRANE 500MG';
  const onglet = page.locator('app-produit-achats-tab');

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await ouvrirOnglet(page, 'Achats');
    await expect(onglet).toBeVisible();
  });

  await etape(2, async () => {
    await onglet.getByRole('button', { name: '1 an' }).click();
    await onglet.getByRole('button', { name: 'Actualiser' }).click();
    await expect(onglet.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    await expect(onglet).toContainText('Prix achat');
    await expect(onglet).toContainText('Montant achat');
    await expect(onglet).toContainText(/Totaux/i);
    await onglet.getByRole('button', { name: 'Graphique' }).click();
    await expect(onglet.locator('canvas')).toBeVisible();
  });

  await etape(4, async () => {
    const telechargement = page.waitForEvent('download');
    await onglet.getByRole('button', { name: 'PDF' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
