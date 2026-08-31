import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le carton contenait un produit abîmé, un autre périmé, un troisième qu'on n'avait pas
 * commandé. Ces trois-là repartent chez le grossiste, mais pas la livraison entière : c'est
 * un retour LIGNE À LIGNE, et l'avoir qui en découle ne porte que ce qui repart.
 *
 * L'écran part du bon reçu et de ses lignes : pour chacune, la quantité à retourner est
 * PLAFONNÉE à la quantité reçue — on ne renvoie pas ce qu'on n'a pas eu — et le motif devient
 * obligatoire dès qu'une quantité est saisie. C'est ce motif qui justifiera l'avoir auprès du
 * grossiste, et qui fera la différence entre une casse, une erreur de préparation et un
 * produit trop proche de sa péremption.
 *
 * Parcours en LECTURE : il prépare l'avoir et montre les contrôles, sans l'émettre — un avoir
 * engage la relation avec le grossiste. La dernière étape s'arrête donc au bouton.
 */
scenario('ACH-79', async ({ etape, page }) => {
  const liste = page.locator('app-list-bons');
  // Le retour par ligne n'est pas une boîte de dialogue mais un ESPACE DE TRAVAIL : le
  // bon y reste sous les yeux pendant qu'on décide, ligne par ligne, de ce qui repart.
  const espace = page.locator('app-retour-workspace');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    const clos = liste.locator('tbody tr').filter({ hasText: 'Clôturé' }).first();
    await clos.getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Retour par ligne' }).click();
    await expect(espace).toBeVisible();
    await expect(espace).toContainText(/Qté à retourner/);
  });

  await etape(2, async () => {
    // On COCHE d'abord la ligne : tant qu'elle ne l'est pas, ni la quantité ni le motif ne
    // s'ouvrent — c'est ce qui empêche de retourner un article par inadvertance en tapant
    // dans la mauvaise cellule.
    const ligne = espace.locator('tbody tr').first();
    await ligne.locator('input[type="checkbox"]').first().check();
    const champ = ligne.locator('input[type="number"], input:not([type="checkbox"])').first();
    await champ.click();
    await champ.fill('');
    await champ.pressSequentially('2', { delay: 40 });
    await expect(champ).toHaveValue('2');
  });

  await etape(3, async () => {
    // Le motif est obligatoire dès qu'une quantité est saisie : c'est lui qui justifiera
    // l'avoir auprès du grossiste — casse, erreur de préparation, péremption trop proche.
    const ligne = espace.locator('tbody tr').first();
    await ligne.locator('.ng-select').first().click();
    await page.locator('.ng-option').first().click();
    await expect(espace).toContainText(/Montant/);
  });

  await etape(4, async () => {
    // Le total sélectionné est calculé au prix d'achat des lignes retenues : c'est le
    // montant qu'on opposera au grossiste, et il se contrôle AVANT d'émettre l'avoir.
    await expect(espace).toContainText(/Sélectionné\s*:/);
  });

  await etape(5, async () => {
    // Le commentaire porte le numéro d'avoir du grossiste ou la référence de l'échange.
    // Le parcours s'arrête ici : émettre l'avoir engage la relation avec le fournisseur.
    const commentaire = espace.locator('textarea').first();
    if (await commentaire.isVisible().catch(() => false)) {
      await commentaire.fill('Casse constatée à l’ouverture du carton — parcours de démonstration');
    }
    await expect(espace.getByRole('button', { name: /Créer l'avoir/ })).toBeVisible();
  });
});
