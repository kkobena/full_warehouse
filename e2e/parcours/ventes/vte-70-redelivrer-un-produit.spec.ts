import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerCaisseOuverte, assurerPanierVide } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un client revient pour « la même chose que la dernière fois ». Dans la fiche, « Re-délivrer »
 * présélectionne le produit dans la recherche — avec son prix et son stock du jour — et la vente
 * suit son circuit ordinaire : les contrôles de stock et de prix s'appliquent comme d'habitude.
 *
 * Parcours en LECTURE : la vente commencée est abandonnée.
 */
scenario('VTE-70', async ({ etape, page }) => {
  const matricule = 'CNAM01-000098';
  const produit = 'SAVON DE MARSEILLE 100ML';
  // Le bon doit être unique par client : l'application refuse un bon déjà employé.
  const numeroBon = 'BON' + Date.now().toString().slice(-9);
  const modale = page.locator('.modal-content');
  const panneau = page.locator('app-fiche-client-panel');
  const contenu = page.locator('#main-content');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);
  await page.getByRole('tab', { name: /Assurance/ }).click();
  const recherche = page.getByPlaceholder('Rechercher un client assuré');
  await recherche.fill('TRAORE');
  await recherche.press('Enter');
  await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
  await expect(contenu).toContainText(matricule);
  const bon = page.getByPlaceholder('Numéro de bon');
  await bon.click();
  await bon.pressSequentially(numeroBon, { delay: 25 });
  await bon.press('Enter');

  await etape(1, async () => {
    await page.getByRole('button', { name: 'Voir la fiche client' }).click();
    await expect(panneau).toContainText('Dernières délivrances');
    await expect(panneau).toContainText(produit);
  });

  await etape(2, async () => {
    await panneau.locator('tr').filter({ hasText: produit }).getByRole('button', { name: 'Re-délivrer ce produit' }).click();
    await expect(panneau).toBeHidden();
    // Le produit est présélectionné, avec son prix du jour.
    await expect(contenu).toContainText(produit);
    await expect(contenu).toContainText(/Prix\s*:\s*[\d\s]*\d/);
  });

  await etape(3, async () => {
    await ajouterAuPanier(page, '1');
    await expect(page.locator('tbody tr').filter({ visible: true }).first()).toContainText(produit);
  });

  await assurerPanierVide(page);
});
