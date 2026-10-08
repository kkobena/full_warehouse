import { expect } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Avant de servir un habitué, on veut savoir ce qu'il faut savoir : allergie, grossesse, carte
 * expirée, encours, avoir à utiliser — puis ce que couvre son organisme et ce qu'il a pris ces
 * derniers mois. La fiche s'ouvre en panneau latéral, sans quitter la vente (Alt+V).
 *
 * Parcours en LECTURE : aucun produit n'est ajouté.
 */
scenario('VTE-69', async ({ etape, page }) => {
  const matricule = 'CNAM01-000098';
  const modale = page.locator('.modal-content');
  const panneau = page.locator('app-fiche-client-panel');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Assurance/ }).click();
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill('TRAORE');
    await recherche.press('Enter');
    await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
    await expect(page.locator('#main-content')).toContainText(matricule);
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Voir la fiche client' }).click();
    await expect(panneau).toContainText('MOUSSA TRAORE');
  });

  await etape(3, async () => {
    // Les alertes d'abord : celui-ci n'en a aucune. Puis l'organisme, avec ce que l'assuré a déjà
    // consommé de son plafond, puis ses dernières délivrances.
    await expect(panneau).toContainText('Aucune alerte');
    await expect(panneau).toContainText('Couverture');
    await expect(panneau).toContainText('35 000');
    await expect(panneau).toContainText('Dernières délivrances');
    await expect(panneau.getByRole('button', { name: 'Re-délivrer ce produit' }).first()).toBeVisible();
  });

  await page.keyboard.press('Escape');
  await assurerPanierVide(page);
});
