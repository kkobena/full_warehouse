import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Retrouver un avoir, c'est presque toujours répondre à un organisme qui conteste : « quel
 * avoir avez-vous passé, quand, et pour quel motif ». La recherche croise donc le tiers
 * payant, la période, le statut et le numéro d'avoir.
 *
 * La barre d'indicateurs répond en amont à une autre question, plus courante encore : combien
 * d'avoirs traînent en brouillon, et pour quel montant. Un brouillon oublié est de l'argent
 * qu'on ne rend pas — et une facture qu'on relance à tort.
 */
scenario('FAC-23', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Avoirs/);
    await rechercher(page);
    await expect(contenu).toContainText('Brouillons');
    await expect(contenu).toContainText('Imputés');
    await expect(contenu).toContainText('N° Avoir');
  });

  await etape(2, async () => {
    // Le statut est le critère le plus utilisé : il isole d'un geste les avoirs encore à
    // traiter de ceux dont l'histoire est close.
    await choisirDansSelect(page, 'av-statut', 'Émis');
    await rechercher(page);
    await expect(contenu).toContainText(/Émis/);
  });

  await etape(3, async () => {
    // L'export est un bouton SCINDÉ : l'action principale sort l'Excel, le chevron — dont le
    // nom accessible est « Autres actions », faute de libellé visible — ouvre le choix entre
    // les deux formats. PDF pour justifier auprès de l'organisme, Excel pour la comptabilité.
    await page.getByRole('button', { name: 'Autres actions' }).first().click();
    const menu = page.locator('.dropdown-menu.show');
    await expect(menu.getByRole('button', { name: 'Excel' })).toBeVisible();
    await expect(menu.getByRole('button', { name: 'PDF' })).toBeVisible();
  });
});
