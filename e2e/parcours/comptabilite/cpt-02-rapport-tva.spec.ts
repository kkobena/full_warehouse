import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Scénario CORRIGÉ dans le modèle en écrivant ce parcours. Il annonçait un onglet
 * « Déclaration TVA » calculant la TVA collectée, la TVA déductible et la TVA nette :
 * l'onglet s'appelle « Rapport TVA » et ne couvre que la collecte, c'est-à-dire les ventes.
 * Rien, dans l'écran, ne touche aux achats. Un manuel qui aurait recopié le modèle aurait
 * promis une déclaration complète là où l'officine n'obtient qu'une moitié.
 */
scenario('CPT-02', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);
  const lignes = page.locator('tbody tr').filter({ visible: true });

  /** Somme d'une colonne de montants, séparateurs de milliers ôtés. */
  const somme = async (colonne: number): Promise<number> => {
    const cellules = await lignes.locator(`td:nth-child(${colonne})`).allInnerTexts();
    return cellules.reduce((total, texte) => total + Number(texte.replace(/\D/g, '') || 0), 0);
  };

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Rapport TVA/);
    // Le titre « Rapport TVA » apparaît deux fois — sur l'onglet et sur la barre d'outils.
    // On s'ancre plutôt sur le regroupement par taux, propre à cet écran.
    await expect(page.getByLabel('Période')).toBeVisible();
  });

  await etape(2, async () => {
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
  });

  await etape(3, async () => {
    await rechercher(page);
    // Le tableau n'existe pas tant que le calcul n'a pas répondu : sa première ligne est
    // donc à la fois la preuve du calcul et l'attente qui protège la capture.
    await expect(page.getByRole('columnheader', { name: 'Code tva' })).toBeVisible();
    await expect(lignes.first()).toBeVisible();
  });

  await etape(4, async () => {
    // Le résultat attendu du modèle, vérifié plutôt qu'affirmé : la ligne de total (dans le
    // `tfoot`, hors du décompte des lignes) est la somme exacte des taux affichés.
    const totalTva = Number(
      (await page.locator('tfoot td').nth(2).innerText()).replace(/\D/g, ''),
    );
    expect(totalTva).toBe(await somme(3));
  });
});
