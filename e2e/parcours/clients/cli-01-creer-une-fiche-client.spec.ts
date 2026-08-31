import { expect } from '@playwright/test';
import { chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La fiche client n'est pas un carnet d'adresses : c'est elle qui porte la COUVERTURE. Un
 * assuré sans tiers payant rattaché paiera tout au comptoir, et l'erreur se découvre au pire
 * moment — devant le patient, la vente commencée.
 *
 * D'où l'ordre du formulaire : l'identité d'abord, puis l'organisme, le numéro d'affiliation
 * et le TAUX de prise en charge. Ce taux est propre au lien entre CE client et CET organisme —
 * la fiche du tiers payant ne le porte pas (FAC-36) — parce qu'un même assureur ne couvre pas
 * tous ses assurés au même niveau.
 *
 * Trois types de clients coexistent, et ils n'ouvrent pas le même formulaire : assuré, carnet,
 * dépôt. Le bouton crée un assuré, le cas courant ; le menu donne les deux autres.
 *
 * Parcours en LECTURE : il remplit sans enregistrer, un client de test s'invitant ensuite dans
 * toutes les recherches du comptoir.
 */
scenario('CLI-01', async ({ etape, page }) => {
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/customer');
    await page.getByRole('button', { name: 'Nouveau client' }).click();
    await expect(modale).toContainText('FORMULAIRE DE CREATION DE CLIENT');

    await modale.locator('#field_firstName').fill('AMINATA');
    await modale.locator('#field_lastName').fill('TRAORE');
    await modale.locator('#field_phone').fill('0708091011');
  });

  await etape(2, async () => {
    // La couverture : l'organisme, le numéro d'affiliation, et le taux propre à cet assuré.
    //
    // La recherche porte sur le NOM LONG, celui que la liste affiche : taper le sigle ne
    // ramène rien.
    await chercherDansSelect(page, 'tiersPayantId', 'CAISSE NATIONALE D');
    // Le champ filtre les caractères à la frappe (`appKeyFilter="alphanum"`) : un tiret y
    // est rejeté sans le moindre message, et le formulaire reste invalide sans qu'on
    // comprenne pourquoi.
    await modale.locator('#field_num').fill('CNAM01000999');
    await modale.locator('#field_taux').fill('80');
    await expect(modale.getByRole('button', { name: /Enregistrer|Suivant/ }).first()).toBeVisible();
  });
});
