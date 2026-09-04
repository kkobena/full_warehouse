import { expect } from '@playwright/test';
import { saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un retour a une date de péremption, lui aussi : passé le délai autorisé — trente jours par
 * défaut, réglable —, l'officine n'est plus tenue de reprendre la marchandise.
 *
 * L'application SIGNALE sans BLOQUER, et c'est le bon arbitrage : le pharmacien connaît son
 * client et son commerce. Ce qu'il ne peut pas deviner, c'est l'ancienneté exacte de la
 * vente ; l'écran la lui donne en jours, avec un avertissement.
 *
 * Parcours en LECTURE : il ouvre la fenêtre de retour sur une vente ancienne du jeu de
 * démonstration et la referme sans rien valider.
 */
scenario('VTE-24', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  // Deux mois en arrière : bien au-delà des trente jours, sans sortir de la période couverte
  // par le jeu de démonstration.
  //
  // Le recul est ajusté si la date tombe un LUNDI : l'officine est fermée ce jour-là et le
  // jeu de démonstration n'y pose aucune vente (`09_ventes.sql`, exclusion du jour 1). Sans
  // cet ajustement, le parcours échoue un jour de campagne sur sept — sur une journée vide,
  // pas sur un défaut de l'application.
  const veille = new Date();
  veille.setDate(veille.getDate() - 60);
  if (veille.getDay() === 1) {
    veille.setDate(veille.getDate() - 1);
  }

  // ── Retrouver une vente ancienne : le journal s'ouvre sur la journée, c'est le filtre de
  //    dates qui donne accès au passé. Le scénario ne décrit qu'une étape — la tentative de
  //    retour —, la recherche n'est donc pas photographiée. ─────────────────────────────────
  await page.goto('/sales-home/gestion');
  await saisirDate(page, 'fromDate', veille);
  await saisirDate(page, 'toDate', veille);
  await page.getByRole('button', { name: 'Rechercher' }).click();
  await expect(lignes.first()).toBeVisible();

  await etape(1, async () => {
    await lignes.first().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Retour client' }).click();

    // L'ancienneté en jours, et l'avertissement qui va avec : la reprise reste possible, mais
    // elle devient une décision, pas une formalité.
    await expect(modale).toContainText('Hors délai');
    await expect(modale).toContainText(/\d+ jours/);
    await expect(modale.getByRole('button', { name: 'Valider le retour' })).toBeVisible();
  });

  // ── Remise en état : rien n'est validé. ─────────────────────────────────────────────────
  await modale.getByRole('button', { name: 'Annuler' }).click();
});
