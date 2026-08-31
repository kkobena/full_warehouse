import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une compagnie d'assurance couvre souvent plusieurs contrats — santé, entreprise, famille —
 * qui sont autant de tiers payants distincts mais partagent un interlocuteur, un délai de
 * règlement et un bordereau.
 *
 * Le groupe porte ces règles par défaut une seule fois. Il ouvre surtout la facturation
 * GROUPÉE : une facture unique pour l'ensemble, plutôt qu'une par contrat, ce que la
 * compagnie demande presque toujours.
 *
 * Parcours en LECTURE : il remplit sans enregistrer, un groupe de test s'invitant ensuite
 * dans toutes les éditions de factures.
 */
scenario('FAC-38', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    // Les groupes ne sont pas un écran à part : ils partagent la page des tiers payants,
    // en second onglet.
    await page.goto('/tiers-payant');
    await ouvrirOnglet(page, /Groupe de tiers payants/);
    await expect(page.getByRole('button', { name: 'Nouveau' })).toBeVisible();
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(page.locator('#field_name')).toBeVisible();
  });

  await etape(2, async () => {
    await page.locator('#field_name').fill('GROUPE TEST ASSURANCES');
    await page.locator('#field_telephone').fill('0102030405');
    // Le délai de règlement et la périodicité descendent sur chaque membre du groupe : c'est
    // le sens de « règles par défaut ».
    await page.locator('#delaiReglement').fill('60');
    await expect(contenu).toContainText('Ordre de tris');
  });

  await etape(3, async () => {
    await expect(page.getByRole('button', { name: 'Enregistrer' }).first()).toBeEnabled();
  });
});
