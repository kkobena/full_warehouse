import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * L'imputation est la seule étape du cycle qui touche à l'argent : elle porte le montant de
 * l'avoir au crédit d'une facture, qui doit d'autant moins.
 *
 * La facture cible n'est pas forcément celle d'origine — un rejet constaté en mars se
 * régularise souvent sur la facture d'avril, encore ouverte. L'écran demande donc laquelle,
 * et refuse un avoir plus gros que ce qui reste dû : on ne crédite pas au-delà de la dette.
 *
 * Cette étape ne faisait rien d'autre que changer un statut jusqu'à peu : la facture choisie
 * était ignorée et aucun solde ne bougeait. Un avoir « imputé » laissait donc le tiers payant
 * relancé pour un montant qu'il n'avait plus à régler.
 */
scenario('FAC-21', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Avoirs/);
    await rechercher(page);
    // « Imputer » n'est proposé que sur un avoir émis : c'est le filtre le plus sûr.
    const emis = lignes.filter({ has: page.getByRole('button', { name: 'Imputer' }) }).first();
    await expect(emis).toBeVisible();
    await expect(contenu).toContainText('Émis');
  });

  await etape(2, async () => {
    const emis = lignes.filter({ has: page.getByRole('button', { name: 'Imputer' }) }).first();
    await emis.getByRole('button', { name: 'Imputer' }).click();
    await expect(modal).toBeVisible();

    // Les factures cibles se cherchent au serveur, restreintes au tiers payant de l'avoir :
    // on ne crédite pas un organisme de la créance d'un autre.
    // Le numéro suit le format « ANNEE_0001 », mais la liste n'affiche que ce qui suit le
    // souligné : chercher « 20 » — l'année — ne ramenait donc rien. On cherche sur le rang.
    await modal.locator('ng-select input[type="text"]').first().fill('00');
    // Le panneau d'options est rendu SUR LE CORPS de la page (`appendTo="body"`), donc en
    // dehors de la modal : le chercher dans `.modal-content` ne trouve jamais rien.
    const option = page.locator('.ng-option').first();
    await expect(option).toBeVisible({ timeout: 15000 });
    await expect(option, 'Aucune facture cible pour cet organisme.').not.toContainText('Aucun résultat');
    await option.click();

    // Le contrôle s'affiche avant toute validation : montant de l'avoir contre reste dû.
    await expect(modal).toContainText(/Montant avoir/);
  });
});
