import { expect } from '@playwright/test';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le client repart avec son bon d'avoir : c'est sa preuve, et le seul document qu'il pourra
 * présenter à sa prochaine visite si le solde ne s'affiche pas comme il l'attend.
 *
 * Le PDF reprend les trois informations qui font foi — le montant, le motif du retour, et la
 * DATE D'EXPIRATION. Cette dernière est la plus importante à écrire : un avoir périmé ne se
 * conteste pas si le client savait quand l'utiliser.
 *
 * L'impression part de la ligne de l'avoir, dans la fiche du client : c'est là qu'on le
 * retrouve des semaines plus tard.
 *
 * ── Pourquoi le client est DÉSIGNÉ par la base ──────────────────────────────────────────────
 * Le parcours ouvrait le PREMIER client de la liste et tolérait « aucun avoir ». Il ne prouvait
 * donc rien la plupart du temps, et il est tombé le jour où ce client s'est trouvé porter un
 * avoir EXPIRÉ : ni le bouton attendu, ni le message d'absence. Un avoir est rattaché à une
 * ligne de vente, donc à qui a acheté — espérer le trouver sur la première fiche venue est un
 * pari que la régénération du jeu de démo finit toujours par faire perdre.
 *
 * On demande donc à la base un client qui en détient un, et on vérifie le geste du scénario.
 *
 * Parcours en LECTURE SEULE : il n'écrit rien.
 */
scenario('CLI-10', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });

  // `fullName` est bâti côté serveur en « prénom espace nom » (CustomerDTO), c'est ce que la
  // liste affiche et ce que la recherche doit retrouver.
  const client = lire(
    `SELECT c.first_name || ' ' || c.last_name
       FROM avoir_client a JOIN customer c ON c.id = a.customer_id
      WHERE a.statut = 'OUVERT' ORDER BY a.id LIMIT 1`,
  );
  expect(client, 'la démo doit porter au moins un avoir ouvert, sinon le scénario est sans objet').not.toBe('');

  await etape(1, async () => {
    await page.goto('/customer');
    await expect(lignes.first()).toBeVisible();
    await page.getByPlaceholder('Taper pour rechercher').fill(client);
    await page.getByPlaceholder('Taper pour rechercher').press('Enter');
    await expect(lignes.first()).toContainText(client);
    // Le bouton d'ouverture de fiche n'a pas de nom accessible : on le désigne par son infobulle.
    await lignes.first().locator('app-button[ngbtooltip="Voir la fiche"] button').first().click();
    await page
      .getByRole('tab', { name: /Avoirs/ })
      .first()
      .click();
    // Un avoir ouvert laisse un solde : c'est ce que le client vient réclamer.
    await expect(contenu).toContainText('Solde disponible');
  });

  await etape(2, async () => {
    // Le bon PDF vit sur la ligne de l'avoir, et il doit être atteignable PAR SON NOM — c'est
    // la même exigence pour un lecteur d'écran que pour ce parcours.
    await expect(page.getByRole('button', { name: "Bon d'avoir PDF" }).first()).toBeVisible();
  });
});
