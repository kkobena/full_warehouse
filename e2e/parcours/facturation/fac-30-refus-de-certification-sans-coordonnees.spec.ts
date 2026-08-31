import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La DGI rejette une facture dont le client n'a ni téléphone ni adresse électronique. Le
 * logiciel s'en aperçoit AVANT d'envoyer quoi que ce soit : il refuse la certification et dit
 * ce qui manque, plutôt que de laisser l'administration répondre une erreur opaque.
 *
 * Le cas est fréquent, et c'est ce qui le rend important : l'email d'un organisme ne se
 * demande jamais au comptoir, personne ne le saisit, et le manque ne se découvre qu'au moment
 * de certifier. Le jeu de démonstration garde donc délibérément un organisme sans email —
 * ASACI — pour que ce garde-fou soit visible.
 *
 * Rien n'est transmis ici : le refus intervient à la construction du message, avant tout
 * appel au service fiscal.
 */
scenario('FAC-30', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    // On cible l'organisme incomplet : c'est lui qui porte le cas. Le filtre est un choix
    // multiple ET interrogé au SERVEUR : ouvrir la liste à la flèche n'affiche rien tant
    // qu'on n'a pas frappé au moins deux caractères.
    // La recherche porte sur le NOM LONG, celui que la liste affiche — pas sur le sigle :
    // taper « ASACI » ne ramène rien.
    const champTp = page.locator('#fhTp');
    await champTp.fill('ASSURANCES SANTE');
    await expect(page.locator('.ng-option').first()).toBeVisible({ timeout: 15000 });
    await page.locator('.ng-option').first().click();
    await champTp.press('Escape');
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();

    const certifiable = lignes.filter({ has: page.locator('button:has(.pi-shield)') }).first();
    await certifiable.locator('button:has(.pi-shield)').first().click();
    await page.locator('.modal-content:visible').getByRole('button', { name: 'Oui' }).click();

    // Le refus nomme ce qui manque, sans quoi on chercherait longtemps. Il arrive par une
    // notification, hors du flux de la page : c'est elle qu'il faut regarder.
    await expect(
      page.locator('[role="alert"], .toast').filter({ hasText: /téléphone|email/i }).first(),
    ).toBeVisible({ timeout: 15000 });
  });
});
