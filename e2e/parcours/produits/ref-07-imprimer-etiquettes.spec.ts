import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un code-barres abîmé, un produit reconditionné, un rayon réétiqueté : l'étiquette se
 * réimprime depuis la fiche, et elle reprend EXACTEMENT ce que la fiche contient — code et
 * prix. C'est ce qui garantit que le scan au comptoir retrouvera le bon produit au bon prix.
 *
 * Le second champ vaut la peine d'être connu : une planche entamée se reprend à la position
 * voulue plutôt que de partir de la première case. La borne n'est plus écrite en dur — elle
 * vient du format de planche choisi, 65 cases sur le format par défaut (REF-66).
 *
 * Parcours en LECTURE : il produit un PDF, il ne modifie rien.
 */
scenario('REF-07', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 20DH WELEDA';

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
    // Le format par défaut reproduit le calage en place avant le catalogue des formats :
    // ce qui s'imprimait droit hier s'imprime droit aujourd'hui.
    await expect(modale).toContainText(/65 étiquettes par planche/i);
    await expect(modale).toContainText(/Case de 1 à 65/i);
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
