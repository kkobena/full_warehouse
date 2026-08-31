import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Une commande peut partir plusieurs fois : un premier envoi en erreur réseau, un renvoi après
 * correction, un reliquat transmis à son tour. L'historique garde la trace de chacun — sa
 * date, son statut, le nombre de lignes acceptées et celui des ruptures.
 *
 * C'est ce qu'on ouvre quand le grossiste affirme n'avoir rien reçu : la date et le statut de
 * l'envoi répondent à sa place.
 *
 * Parcours en LECTURE.
 */
scenario('ACH-31', async ({ etape, page }) => {
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
    await ligneTransmise().dblclick();
    await expect(panneau).toBeVisible();
  });

  await etape(2, async () => {
    // Une ligne par envoi : quand, dans quel état, combien de lignes acceptées, combien en
    // rupture. C'est le journal de la relation avec ce grossiste, commande par commande.
    await expect(panneau).toContainText(/Historique des envois/i);
    await expect(panneau).toContainText('Date');
    await expect(panneau).toContainText(/Acc\./i);
    await expect(panneau).toContainText(/Rupt\./i);
  });
});
