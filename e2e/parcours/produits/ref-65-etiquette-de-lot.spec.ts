import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirEtiquettesProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le retournement de ce lot de travail : plutôt que d'espérer que la boîte porte un code que
 * l'application sache lire, on imprime SA PROPRE étiquette. Elle réencode le code du
 * catalogue, le numéro de lot et la péremption dans un GS1 DataMatrix — un symbole dont
 * l'application maîtrise la trame, donc qu'elle relira au comptoir et à l'inventaire.
 *
 * Ce n'est pas une modification de l'étiquette existante : c'est un SECOND TYPE, et le choix
 * se fait à la génération. L'étiquette standard sort exactement comme avant (REF-07) —
 * décision prise pour ne pas changer, en silence, ce que l'officine imprime déjà.
 *
 * Le type « Lot » ne se contente pas de changer la mise en page, il demande une donnée de
 * plus : QUEL lot. La liste n'offre que les lots en stock, avec leur péremption et leur
 * quantité, et l'impression reste refusée tant qu'aucun n'est choisi.
 *
 * Parcours en LECTURE : il produit un PDF, il ne modifie rien.
 */
scenario('REF-65', async ({ etape, page }) => {
  // L'article nommé par le jeu de démonstration comme cible de l'étiquette interne : suivi
  // par lot, et doté de lots en stock à la péremption future (27_codes_alias_datamatrix.sql).
  const produit = 'METFORMINE VIATRIS 1000 mg';
  let modale = page.locator('.modal-content');

  await etape(1, async () => {
    modale = await ouvrirEtiquettesProduit(page, produit);
    await expect(modale).toContainText(produit);
    // À l'ouverture, le type standard : celui qui ne porte que le code article.
    await expect(modale).toContainText(/Code-barres du code article, sans lot ni péremption/i);
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Lot (DataMatrix)' }).click();
    await expect(modale).toContainText(/GS1 DataMatrix portant le lot et la péremption/i);
    // Le lot ne peut pas être deviné : sans lui la trame n'aurait rien à porter, et
    // l'impression est refusée jusqu'à ce qu'un lot soit désigné.
    await expect(modale.getByRole('button', { name: 'Imprimer' })).toBeDisabled();
    // Les lots en stock, avec ce qu'il faut pour choisir : numéro, péremption, quantité.
    await choisirDansSelect(page, 'etiquetteLot', 'en stock');
    await expect(modale.getByRole('button', { name: 'Imprimer' })).toBeEnabled();
  });

  await etape(3, async () => {
    // Dans un NAVIGATEUR, un PDF s'ouvre dans un onglet (`BlobDownloadService.openInNewTab`) :
    // il porte donc le nom de son URL de blob, et non le `etiquettes-lot-<code>` calculé par
    // la modale — ce nom-là ne sert que sur le poste, où Tauri ouvre un « Enregistrer sous ».
    // On vérifie donc que le document SORT, pas comment le navigateur le baptise.
    const telechargement = page.waitForEvent('download');
    await modale.getByRole('button', { name: 'Imprimer' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
