import { expect, type Locator } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux ratios, et ils disent la même chose à l'envers l'un de l'autre : ce que l'officine vend
 * rapporté à ce qu'elle achète, et l'inverse.
 *
 * Leur utilité tient à leur lecture croisée sur la durée. Un ratio vente/achat qui tombe mois
 * après mois signale qu'on achète plus vite qu'on ne vend — le stock gonfle, la trésorerie se
 * tend, et cela se voit ici bien avant de se voir en banque.
 *
 * Ils figurent dans le même tableau que le chiffre d'affaires et les achats qui les
 * composent : le ratio n'est pas un chiffre à part, c'est leur rapport, et il se vérifie
 * d'un coup d'œil sur la même ligne.
 */
scenario('CPT-07', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Tableau pharmacien/);
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
    await rechercher(page);
  });

  await etape(2, async () => {
    // Les ratios sont une colonne du tableau, à côté de ce qui les compose.
    await expect(contenu).toContainText('Ratios');
    await expect(contenu).toContainText("Chiffre d'affaires");
    await expect(contenu).toContainText('Achats');

    // Les deux ratios sont le sujet même de ce scenario : ils doivent se déduire des colonnes
    // qui les entourent. V/A = Montant Net / (Achats Nets − Avoirs), A/V l'inverse, tous deux
    // tronqués au centième (RoundingMode.FLOOR côté serveur).
    //
    // Les cellules sont lues depuis la FIN : le nombre de colonnes fournisseurs varie avec le
    // paramétrage, un index depuis le début désignerait une autre colonne d'une officine à
    // l'autre.
    const table = page.locator('table').filter({ has: page.getByText('MONTANT NET') }).first();
    const lignes = table.locator('tbody tr').filter({ visible: true });
    const nombreDeJours = await lignes.count();
    expect(nombreDeJours, 'un tableau sans ligne ne prouve rien').toBeGreaterThan(0);

    const auCentieme = (valeur: number) => Math.floor(valeur * 100) / 100;

    for (let rang = 0; rang < nombreDeJours; rang++) {
      const cellules = (await lignes.nth(rang).locator('td').allInnerTexts())
        .map(t => Number(t.replace(/\s/g, '').replace(',', '.').replace(/[^\d.-]/g, '')));
      const n = cellules.length;
      const net = cellules[4];
      const avoirs = cellules[n - 4];
      const achatsNets = cellules[n - 3];
      const ratioVA = cellules[n - 2];
      const ratioAV = cellules[n - 1];
      const achatNet = achatsNets - avoirs;

      if (achatNet === 0 || net === 0) {
        continue; // le serveur laisse le ratio à zéro plutôt que de diviser par zéro
      }
      expect(ratioVA, `V/A de la ligne ${rang + 1}`).toBe(auCentieme(net / achatNet));
      expect(ratioAV, `A/V de la ligne ${rang + 1}`).toBe(auCentieme(achatNet / net));
    }
  });
});
