import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un code-barres abîmé, un produit reconditionné, un rayon réétiqueté : l'étiquette se
 * réimprime depuis la fiche, et elle reprend EXACTEMENT ce que la fiche contient — code et
 * prix. C'est ce qui garantit que le scan au comptoir retrouvera le bon produit au bon prix.
 *
 * Le second champ vaut la peine d'être connu : les planches A4 comptent 65 étiquettes, et une
 * planche entamée se reprend à la position voulue plutôt que de partir de la première case.
 *
 * Parcours en LECTURE : il produit un PDF, il ne modifie rien.
 */
scenario('REF-07', async ({ etape, page }) => {
  const produit = 'ARNICA';

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('tbody tr').filter({ visible: true }).first()
      .getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Imprimer étiquette' }).click();
    const modale = page.locator('.modal-content');
    // La fenêtre rappelle ce qui partira sur l'étiquette : le libellé, le code et le prix
    // tels qu'ils sont AUJOURD'HUI dans la fiche.
    await expect(modale).toContainText(produit);
    await expect(modale).toContainText("Nombre d'étiquettes");
    await expect(modale).toContainText('Planche A4 de 65 étiquettes');
  });

  await etape(2, async () => {
    const modale = page.locator('.modal-content');
    await modale.locator('#etiquetteQty').fill('4');
    // Le PDF est produit par le SERVEUR et remis au navigateur, qui l'ouvre dans un onglet
    // — c'est de là qu'on imprime la planche. On attend donc le fichier lui-même, et non la
    // seule fermeture de la fenêtre : c'est la preuve que l'étiquette a bien été produite.
    const telechargement = page.waitForEvent('download');
    await modale.getByRole('button', { name: 'Imprimer' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
