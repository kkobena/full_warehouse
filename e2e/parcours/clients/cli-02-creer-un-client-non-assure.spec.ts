import { expect } from '@playwright/test';
import { ajouterAuPanier, assurerPanierVide, chercherProduit } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un client de passage n'a pas de couverture à saisir : lui demander un organisme, un numéro
 * d'affiliation et un taux serait absurde, et le formulaire complet (CLI-01) l'exigerait.
 *
 * La fiche allégée ne demande donc que l'essentiel — nom, prénom, téléphone — et c'est ce qui
 * la rend utilisable AU COMPTOIR, la vente déjà commencée : on ne quitte pas l'écran, on ne
 * fait pas attendre.
 *
 * C'est aussi pourquoi elle vit là et pas dans la gestion des clients : elle répond à un
 * besoin de caisse, celui de rattacher une vente à quelqu'un sans instruire un dossier.
 *
 * Parcours ÉCRIVANT le panier, qu'il annule à la fin ; la fiche client, elle, n'est pas
 * enregistrée.
 */
scenario('CLI-02', async ({ etape, page }) => {
  const modale = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await assurerPanierVide(page);
    await page.goto('/sales-home');
    // Le panneau client n'apparaît qu'une fois la vente commencée : il n'y a rien à
    // rattacher à un panier vide.
    await chercherProduit(page, 'DOLIPRANE 500MG');
    await ajouterAuPanier(page, '1');
    // Le panneau s'ouvre en popover, et son intitulé dépend de l'état de la vente :
    // « Choisir un client » si aucun n'est rattaché, « Changer le client » sinon.
    await page
      .getByRole('button', { name: /Choisir un client|Changer le client/ })
      .first()
      .click();
    await page.getByRole('button', { name: 'Nouveau client' }).click();
    await expect(modale).toContainText("FORMULAIRE D'AJOUT DE NOUVEAU CLIENT");
  });

  await etape(2, async () => {
    // Trois champs, et aucun lié à un tiers payant : c'est toute la différence avec la
    // fiche complète.
    await modale.locator('#field_firstName').fill('KOFFI');
    await modale.locator('#field_lastName').fill('YAO');
    await modale.locator('#field_phone').fill('0102030405');
    await expect(modale).not.toContainText(/Numéro de sécurité|Taux \(%\)/);
  });

  await etape(3, async () => {
    await expect(modale.getByRole('button', { name: 'Enregistrer' })).toBeVisible();
  });

  // ── Remise en état, hors étapes : la vente en cours est abandonnée. ───────────────────
  await modale.getByRole('button', { name: 'Annuler' }).click();
  await page.getByRole('button', { name: 'Annuler' }).last().click();
});
