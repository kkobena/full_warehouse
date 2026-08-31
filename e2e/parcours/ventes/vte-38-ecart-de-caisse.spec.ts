import { expect } from '@playwright/test';
import { assurerCaisseOuverte } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le moment de vérité de la journée : ce que la caisse DEVRAIT contenir — fonds d'ouverture
 * plus encaissements — face à ce qu'on y a réellement compté. L'écart n'est pas une faute en
 * soi : c'est un fait, qu'il faut constater et expliquer. Ce qui serait fautif, c'est de le
 * découvrir le lendemain.
 *
 * La clôture ne se valide donc pas à l'aveugle : le comptage est confronté au théorique avant
 * confirmation.
 *
 * Parcours ÉCRIVANT dans la base : il clôture la caisse de l'utilisateur.
 */
scenario('VTE-38', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await page.goto('/my-cash-register');
    await page.getByRole('button', { name: /Fermer la caisse/i }).click();
    await expect(page.getByText('Billetage de caisse')).toBeVisible();

    // Un comptage volontairement PARTIEL : deux billets de 10 000 là où la journée a
    // encaissé davantage. C'est ce décalage qui fait apparaître l'écart.
    await page.locator('#numberOf10Thousand').fill('2');
    await expect(contenu).toContainText(/20\s?000/);
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: /Valider le billetage/i }).click();
    // La confirmation est le dernier point d'arrêt : après elle, la caisse est close et le
    // comptage figé.
    await expect(modale).toBeVisible();
    await modale.getByRole('button', { name: 'Oui' }).click();
    // Caisse fermée : l'écran ne propose plus que l'ouverture de la suivante.
    await expect(page.getByRole('button', { name: 'Ouvrir la caisse' })).toBeVisible();
  });
});
