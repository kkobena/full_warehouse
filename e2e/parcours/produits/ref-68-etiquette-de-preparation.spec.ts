import { expect } from '@playwright/test';
import { ouvrirEtiquettesProduit, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La préparation magistrale est le seul type d'étiquette dont le contenu n'existe nulle part
 * en base : il est saisi au moment d'imprimer, et rien n'est conservé. Décision assumée —
 * l'étiquette est une sortie, pas un enregistrement ; le registre des préparations suppose
 * que le contenu réglementaire soit arrêté, et c'est un autre chantier.
 *
 * Un seul champ est EXIGÉ, et c'est celui qui fait l'étiquette : la composition. Le nom du pot
 * est prérempli avec l'article d'où l'impression part, les deux dates et la mention
 * complètent. Tant que la composition manque, l'impression reste refusée — une étiquette de
 * préparation sans composition n'en est pas une.
 *
 * Parcours en LECTURE : il produit un PDF et n'écrit rien, ni en base ni dans la fiche.
 */
scenario('REF-68', async ({ etape, page }) => {
  const produit = 'ARNICA MONTANA 20DH WELEDA';
  let modale = page.locator('.modal-content');

  await etape(1, async () => {
    modale = await ouvrirEtiquettesProduit(page, produit);
    await expect(modale).toContainText(produit);
  });

  await etape(2, async () => {
    await modale.getByRole('button', { name: 'Préparation' }).click();
    await expect(modale).toContainText(/Rien n’est conservé|Rien n'est conservé/i);
    // Le nom du pot part de l'article d'où l'on vient : c'est presque toujours le bon, et il
    // reste modifiable.
    await expect(modale.locator('#prepLibelle')).toHaveValue(new RegExp(produit));
    // La composition manque : l'impression est refusée, et elle l'est AVANT le clic.
    await expect(modale.getByRole('button', { name: 'Imprimer' })).toBeDisabled();
  });

  await etape(3, async () => {
    await modale.locator('#prepComposition').fill('Acide salicylique 5 g, vaseline qsp 100 g');
    await modale.locator('#prepMention').fill('À usage externe');
    // Les deux dates que la réglementation attend sur le pot : préparé le, à utiliser avant.
    await saisirDate(page, 'prepDate', new Date());
    const dans3Mois = new Date();
    dans3Mois.setMonth(dans3Mois.getMonth() + 3);
    await saisirDate(page, 'prepPeremption', dans3Mois);
    await expect(modale.getByRole('button', { name: 'Imprimer' })).toBeEnabled();

    // Le PDF s'ouvre dans un onglet du navigateur, donc sous le nom de son URL de blob : le
    // nom calculé par la modale ne sert que sur le poste, sous Tauri.
    const telechargement = page.waitForEvent('download');
    await modale.getByRole('button', { name: 'Imprimer' }).click();
    const fichier = await telechargement;
    expect(fichier.suggestedFilename()).toMatch(/\.pdf$/);
  });
});
