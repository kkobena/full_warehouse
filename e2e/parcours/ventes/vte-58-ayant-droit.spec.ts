import { expect } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'assuré paie la cotisation, mais c'est souvent un autre qui vient chercher les
 * médicaments : conjoint, enfant, parent. La vente doit donc distinguer DEUX personnes — celle
 * qui ouvre les droits et celle qui est soignée — sans jamais les confondre : c'est l'assuré
 * qui porte les organismes, l'ayant droit qui reçoit les produits.
 *
 * L'écran les tient côte à côte, chacun sur sa carte, et le bénéficiaire retenu est enregistré
 * sur la vente.
 *
 * Parcours en LECTURE : il n'ajoute aucun produit et abandonne la vente commencée.
 */
scenario('VTE-58', async ({ etape, page }) => {
  const matricule = 'ASA01-000003';
  const ayantDroit = 'IBRAHIM';
  const modale = page.locator('.modal-content');
  const bandeau = page.locator('app-insurance-data-bar');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await page.getByRole('tab', { name: /Assurance/ }).click();
  const recherche = page.getByPlaceholder('Rechercher un client assuré');
  await recherche.fill('TRAORE');
  await recherche.press('Enter');
  await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
  await expect(bandeau).toContainText(matricule);

  await etape(1, async () => {
    // Tant qu'aucun bénéficiaire n'est choisi, la carte « Ayant droit » n'offre qu'une action :
    // en désigner un. La liste est celle des ayants droit DE CET ASSURÉ, personne d'autre.
    await page.getByRole('button', { name: 'Ajouter un ayant droit' }).click();
    await expect(modale).toContainText('LISTE DES AYANTS DROITS DU CLIENT');
    await expect(modale).toContainText('AWA TRAORE');
  });

  await etape(2, async () => {
    // La liste propose aussi d'en créer un : un nouveau-né, un conjoint récemment déclaré —
    // cela n'attend pas la prochaine visite au fichier client.
    await expect(modale.getByRole('button', { name: 'Ajouter un ayant droit' })).toBeVisible();
    await modale.locator('tbody tr').filter({ hasText: ayantDroit }).first().dblclick();
    await expect(modale).toBeHidden();
  });

  await etape(3, async () => {
    // Les deux identités cohabitent : l'assuré et son matricule d'un côté, le bénéficiaire de
    // l'autre. C'est cette lecture côte à côte qui évite de facturer pour la mauvaise personne.
    await expect(bandeau).toContainText('AWA TRAORE');
    await expect(bandeau).toContainText(matricule);
    await expect(bandeau).toContainText(ayantDroit);
  });

  await etape(4, async () => {
    // La vente se poursuit normalement : les organismes sont ceux de l'ASSURÉ, et les bons
    // aussi. L'ayant droit ne change rien à la couverture.
    await expect(bandeau).toContainText('ASACI');
    await expect(bandeau).toContainText('SUNU');
    await expect(page.locator('#produitbox')).toBeVisible();
  });
});
