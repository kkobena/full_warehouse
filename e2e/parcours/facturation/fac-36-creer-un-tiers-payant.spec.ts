import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un nouvel organisme payeur — une mutuelle, l'assurance d'une entreprise voisine — se déclare
 * une fois, et devient aussitôt sélectionnable au comptoir.
 *
 * La fiche ne porte PAS le taux de prise en charge : celui-ci se négocie assuré par assuré et
 * vit sur le lien entre l'organisme et le client. Ce qu'elle porte, ce sont les règles de
 * FACTURATION — délai de règlement, périodicité, plafonds, modèle de bordereau —, c'est-à-dire
 * tout ce qui décidera plus tard de la facture mensuelle.
 *
 * Parcours en LECTURE : il remplit le formulaire sans enregistrer, un organisme de test
 * polluant durablement les listes de vente et de facturation.
 */
scenario('FAC-36', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/tiers-payant');
    await expect(page.getByRole('button', { name: 'Nouveau tiers payant' })).toBeVisible();
    await page.getByRole('button', { name: 'Nouveau tiers payant' }).click();
    // Le formulaire s'ouvre en modal, par-dessus la liste.
    await expect(page.locator('.modal-content:visible')).toContainText('Nom abrégé');
  });

  await etape(2, async () => {
    const formulaire = page.locator('.modal-content:visible');
    await formulaire.locator('#field_libelle').fill('MUTUELLE TEST');
    await formulaire.locator('#field_fullName').fill('MUTUELLE DES ARTISANS DE COCODY');
    await formulaire.locator('#telephone').fill('0102030405');
    // L'adresse électronique est OBLIGATOIRE, et pas par formalisme : sans elle, la
    // certification fiscale de ses factures sera refusée (FAC-30).
    await formulaire.locator('#email').fill('facturation@mutuelle-test.example');
    // Le délai de règlement n'est pas décoratif : c'est lui qui datera l'échéance de chaque
    // facture, et donc la relance.
    await formulaire.locator('#delaiReglement').fill('45');
  });

  await etape(3, async () => {
    // L'enregistrement n'est offert qu'une fois les champs obligatoires remplis.
    await expect(page.getByRole('button', { name: /Enregistrer|Sauvegarder/ }).first()).toBeEnabled();
  });
});
