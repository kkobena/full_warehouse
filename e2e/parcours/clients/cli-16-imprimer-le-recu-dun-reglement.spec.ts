import { expect } from '@playwright/test';
import { saisirMontant } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le client qui paye son carnet repart avec un papier : c'est sa seule preuve tant que le
 * solde n'est pas soldé, et la première chose qu'il ressortira s'il conteste plus tard.
 *
 * L'impression n'est pas automatique — on ne gâche pas un ticket quand le client n'en veut
 * pas — mais elle est PROPOSÉE aussitôt le règlement enregistré, au moment où le client est
 * encore devant le comptoir. Passé ce moment, le reçu reste réimprimable depuis l'historique
 * (CLI-17).
 *
 * Le circuit d'impression dépend du poste : ticket ESC/POS sur une caisse en mode desktop,
 * imprimante classique ailleurs. Le parcours s'arrête à la proposition, le poste d'exécution
 * n'ayant pas d'imprimante.
 *
 * Parcours ÉCRIVANT : il enregistre un versement PARTIEL et volontairement modeste, pour ne
 * pas solder le compte dont vivent les autres parcours différés.
 */
scenario('CLI-16', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const panneau = page.locator('.detail-column');
  const formulaire = page.locator('app-reglement-differe-form');

  await etape(1, async () => {
    await page.goto('/differes');
    await lignes.first().click();
    await panneau.getByRole('tab', { name: /Régler/ }).click();
    await expect(formulaire).toBeVisible();

    // Un versement d'appoint : le compte reste débiteur après coup. Le champ est pré-rempli
    // avec le solde entier et `app-input-number` CONCATÈNE si l'on ne le vide pas — d'où le
    // passage par `saisirMontant`, qui vide, vérifie, puis frappe.
    await saisirMontant(page, 'app-reglement-differe-form app-input-number input', '500');
    await page.getByRole('button', { name: 'Valider' }).click();

    // Le règlement enregistré, l'écran propose le ticket sans l'imposer.
    const question = page.locator('.modal-content:visible');
    await expect(question).toContainText('Ticket règlement');
    await expect(question).toContainText(/imprimer le ticket/i);
  });
});
