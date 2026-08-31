import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * La même liste que REF-53, lue par l'autre bout. Le pharmacien qui prépare une négociation
 * annuelle ne se demande pas « quels organismes tarifent CE produit ? » mais « sur quoi cet
 * organisme applique-t-il un tarif, et lequel ? ». La liste vue depuis l'organisme donne les
 * produits, leur CIP, l'option de prix retenue et le taux.
 *
 * Cette lecture existait dans le composant depuis le début — il sait se charger par produit ou
 * par tiers payant — mais SEUL le sens « par produit » avait un point d'entrée. Le bouton
 * « Tarifs produits négociés » de la liste des tiers payants a été ajouté en écrivant ce
 * parcours.
 *
 * Parcours en LECTURE.
 */
scenario('REF-54', async ({ etape, page }) => {
  const organisme = 'CAISSE NATIONALE';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/tiers-payant');
    await expect(lignes.first()).toBeVisible();
    await expect(page.locator('#main-content')).toContainText(/Tiers/i);
  });

  await etape(2, async () => {
    const ligne = lignes.filter({ hasText: organisme }).first();
    await expect(ligne).toBeVisible();
    await ligne.getByRole('button', { name: 'Tarifs produits' }).click();
    // L'en-tête nomme l'organisme : on sait de quel payeur on lit les tarifs.
    await expect(modale).toContainText(organisme);
    // Et les colonnes changent de sens : c'est le PRODUIT qui est nommé, avec son CIP.
    await expect(modale).toContainText('Produit');
    await expect(modale).toContainText('Cip');
    await expect(modale).toContainText('Option de prix');
  });
});
