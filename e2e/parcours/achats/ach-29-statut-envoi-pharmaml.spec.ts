import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une commande transmise n'est pas une commande acceptée : le grossiste répond, et sa réponse
 * peut confirmer, réduire ou refuser des lignes. Le panneau PharmaML porte cet état à côté de
 * la commande — statut de l'envoi, nombre de lignes acceptées, nombre en rupture.
 *
 * La commande, elle, est VERROUILLÉE : le bandeau le dit — « Soumise via PharmaML,
 * modifications désactivées ». Ce qui est parti chez le grossiste ne se réécrit pas.
 *
 * Ce panneau existait avec son historique et ses disponibilités, mais n'était monté NULLE
 * PART : le suivi d'un envoi était introuvable. Il a été rattaché à la commande en écrivant
 * ce parcours.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-29', async ({ etape, page }) => {
  const panneau = page.locator('app-pharmaml-home');
  const liste = page.locator('app-commande-requested-home');
  /** La ligne d'une commande DÉJÀ transmise, désignée par sa pastille. */
  const ligneTransmise = () =>
    liste
      .locator('tbody tr')
      .filter({ has: page.locator('.pharma-badge-info', { hasText: /^PHARMAML$/ }) })
      .first();

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    // LA commande transmise, repérée par sa PASTILLE — l'élément, pas le texte de la ligne :
    // le menu d'actions rendu dans chaque ligne contient « Envoyer via PharmaML », et un
    // filtre sur le texte ferait correspondre n'importe quelle commande.
    await ligneTransmise().dblclick();
    await expect(page.locator('app-commande-requested')).toContainText(/Soumise via/i);
  });

  await etape(2, async () => {
    // Le statut de l'envoi, avec ce que le grossiste a accepté et ce qu'il a mis en rupture.
    await expect(panneau).toBeVisible();
    await expect(panneau).toContainText('PharmaML');
    await expect(panneau).toContainText(/Statut/i);
    await expect(panneau).toContainText(/Soumise|Acceptée|Erreur/i);
  });
});
