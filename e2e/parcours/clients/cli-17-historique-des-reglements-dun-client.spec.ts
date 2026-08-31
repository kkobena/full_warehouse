import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * « J'ai payé le mois dernier » — et il faut pouvoir le vérifier, versement par versement.
 *
 * L'historique répond en se resserrant sur UN client : la ligne agrège ce qu'il a versé et ce
 * qu'il doit encore, et le dépliage donne chaque versement avec son mode, la date, le montant
 * attendu, le montant réellement remis et l'opérateur qui a encaissé. Le reçu de chacun se
 * réimprime depuis cette même ligne, ce qui évite de rechercher le règlement d'origine.
 *
 * C'est la différence avec la vue globale (CLI-21) : ici, on répond à un client précis, pas
 * à la comptabilité.
 */
scenario('CLI-17', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin);
  debut.setDate(debut.getDate() - 60);

  const lignes = page.locator('tbody tr').filter({ visible: true });

  await etape(1, async () => {
    await page.goto('/differes');
    await ouvrirOnglet(page, /Historique/);
    await expect(page.getByRole('columnheader', { name: 'Total encaissé' })).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'hrDu', debut);
    await saisirDate(page, 'hrAu', fin);
    await rechercher(page);
    await expect(lignes.first()).toBeVisible();
    // Le filtre client resserre l'écran sur le dossier qu'on a en main.
    const nom = (await lignes.first().innerText()).split('\n')[1]?.trim();
    if (nom) {
      await chercherDansSelect(page, 'hrClient', nom.slice(0, 4), nom);
      await rechercher(page);
    }
    await expect(lignes.first()).toBeVisible();
  });

  await etape(3, async () => {
    await page.getByRole('button', { name: 'Déplier le détail' }).first().click();
    // Le détail complet d'un versement : de quoi répondre sans rouvrir la caisse.
    await expect(page.getByRole('columnheader', { name: 'Mode de paiement' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /Date/ })).toBeVisible();
  });

  await etape(4, async () => {
    // Chaque versement garde son justificatif réimprimable, des mois plus tard.
    await expect(page.locator('button:has(.pi-print)').first()).toBeVisible();
  });
});
