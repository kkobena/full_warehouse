import { expect } from '@playwright/test';
import { ouvrirEtiquettesProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La réglette ne va pas sur la boîte mais sur l'ÉTAGÈRE, et elle ne s'adresse pas à la même
 * personne : le client y lit le prix, le vendeur le libellé. D'où une mise en page à
 * l'inverse de l'étiquette de boîte — prix en gros, code en petit — et aucun numéro de lot,
 * qui n'aurait rien à dire sur une gondole où les boîtes se remplacent.
 *
 * C'est le type d'étiquette qui le décide, pas le format : la réglette s'imprime sur le même
 * catalogue de planches et de rouleaux que les autres (REF-66), les formats les plus larges
 * lui allant simplement mieux.
 *
 * Parcours en LECTURE : il produit un PDF, il ne modifie rien.
 */
scenario('REF-67', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 20DH WELEDA';
  let modale = page.locator('.modal-content');

  await etape(1, async () => {
    modale = await ouvrirEtiquettesProduit(page, produit);
    await expect(modale).toContainText(produit);
    await expect(modale).toContainText("Nombre d'étiquettes");
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Réglette' }).click();
    // Le texte sous le sélecteur dit ce que la réglette porte, et ce qu'elle ne porte pas :
    // la différence ne se verrait qu'une fois la planche imprimée.
    await expect(modale).toContainText(/le prix en gros, le code en petit, pas de lot/i);
    // Aucune donnée supplémentaire à fournir, contrairement au type « Lot » : la réglette se
    // lit entièrement dans la fiche.
    await expect(modale.locator('#etiquetteLot')).toHaveCount(0);
  });

  await etape(3, async () => {
    // Le PDF s'ouvre dans un onglet du navigateur, donc sous le nom de son URL de blob : le
    // nom calculé par la modale (`reglettes-<code>`) ne sert que sur le poste, sous Tauri.
    const telechargement = page.waitForEvent('download');
    await modale.getByRole('button', { name: 'Imprimer' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
