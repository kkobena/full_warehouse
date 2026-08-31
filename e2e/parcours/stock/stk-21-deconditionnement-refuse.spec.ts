import { expect } from '@playwright/test';
import { chercherAuCatalogue } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le pendant du déconditionnement (STK-20) : ce qu'il refuse de faire. Ouvrir plus de boîtes
 * qu'on n'en détient créerait des unités sorties de nulle part — le stock du détail serait
 * juste, celui de la boîte négatif, et l'inventaire suivant s'en apercevrait trop tard.
 *
 * L'écran le dit avant d'agir : la quantité demandée est comparée au stock disponible, et le
 * refus s'affiche pendant la saisie, pas après l'envoi. C'est un contrôle de saisie, pas un
 * message d'erreur.
 *
 * Parcours en LECTURE : l'opération est refusée, rien n'est enregistré.
 */
scenario('STK-21', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');
  const produit = 'AMOXICILLINE SIROP';

  await etape(1, async () => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    const ligne = lignes.filter({ hasText: produit }).first();
    await ligne.getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Déconditionner' }).click();
    await expect(modale).toContainText('Stock actuel');
  });

  await etape(2, async () => {
    // Une quantité que le stock ne couvre pas : le refus s'affiche pendant la saisie.
    const champ = modale.locator('#qtyMvt');
    await champ.click();
    await champ.fill('');
    await champ.pressSequentially('99999', { delay: 20 });
    await expect(modale).toContainText('Stock insuffisant');
    // Et l'enregistrement reste hors de portée tant que le compte n'y est pas.
    await expect(modale.getByRole('button', { name: 'Enregistrer' })).toBeDisabled();
  });
});
