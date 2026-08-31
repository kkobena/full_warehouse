import { expect } from '@playwright/test';
import { choisirDansSelect, ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le portefeuille de factures se lit rarement en entier : on cherche « ce que la CNAM nous
 * doit depuis juin », ou « toutes les impayées de l'assurance privée ». Quatre critères
 * suffisent à cela — la période, le statut, le groupe et le tiers payant — et ils se
 * combinent.
 *
 * L'export Excel reprend exactement le résultat affiché : c'est ce qui permet d'envoyer à un
 * assureur la liste de ses propres impayés, sans retraitement.
 *
 * Parcours en LECTURE.
 */
scenario('FAC-49', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /^Factures/);
    await expect(contenu).toContainText(/Statut/);
  });

  await etape(2, async () => {
    // Le statut : réglées, partiellement réglées, non réglées. C'est le filtre du
    // recouvrement — celui qui isole ce qu'il reste à encaisser.
    await choisirDansSelect(page, 'fhStatut', 'Non réglées');
    await page.keyboard.press('Escape');
  });

  await etape(3, async () => {
    await rechercher(page);
    await expect(page.getByRole('columnheader', { name: 'N° Facture' })).toBeVisible();
  });

  await etape(4, async () => {
    // L'export reprend le résultat filtré, tel qu'il est à l'écran.
    await expect(page.getByRole('button', { name: 'Excel' })).toBeVisible();
  });
});
