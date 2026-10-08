import { expect, type Locator } from '@playwright/test';
import { ouvrirOnglet, rechercher, saisirDate } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tableau du pharmacien est le document que l'officine sort pour son expert-comptable :
 * une ligne par jour (ou par mois), les ventes d'un côté, les achats par grossiste de
 * l'autre, et les deux ratios qui les relient. C'est l'écran le plus large de
 * l'application — d'où l'intérêt d'une image plutôt que d'un paragraphe.
 *
 * Deux identités se lisent à même l'écran, et aucune n'était vérifiée :
 *
 *  1. le pied de table totalise les lignes, sans en oublier ni en inventer ;
 *  2. basculer du journalier au mensuel regroupe les mêmes ventes — les totaux ne bougent pas.
 *
 * La seconde a du mordant : le mensuel passe par une seconde fonction stockée
 * (`tableau_pharmacien_month_report`), distincte de la journalière et libre de diverger d'elle
 * à la première évolution. Rien d'autre ne les confronte.
 *
 * Une troisième identité — le Montant Net se décompose en Comptant plus Crédit — n'est PAS
 * vérifiée ici, et c'est délibéré. Elle ne tient qu'en mode « chiffre d'affaires réel ». En mode
 * « déclaré », que l'officine peut retenir par réglage (`ModeCaService.modeComptabilite`), le
 * TTC et les règlements sont ramenés au montant déclarable tandis que la colonne Crédit garde
 * sa valeur réelle : les trois colonnes ne décomposent plus la même assiette. Sur le jeu de
 * démonstration, 143 journées sur 155 s'écartent ainsi, pour 8 669 805 F cumulés, quand le mode
 * réel en laisse 2 F sur l'année entière. L'écran ne dit pas quel mode il applique, et le
 * parcours ne peut donc pas choisir la borne à exiger.
 */
scenario('CPT-06', async ({ etape, page }) => {
  const fin = new Date();
  const debut = new Date(fin.getFullYear(), fin.getMonth() - 2, 1);

  const table = page.locator('table').filter({ has: page.getByText('Montant Net') }).first();
  const lignes = table.locator('tbody tr').filter({ visible: true });
  const total = table.locator('tfoot tr').first();

  const nombre = async (ligne: Locator, colonne: number): Promise<number> =>
    Number((await ligne.locator('td').nth(colonne).innerText()).replace(/[^\d-]/g, ''));

  // Date · Comptant · Crédit · Remise · Montant Net · N. Clients · …
  const COLONNES = [
    [1, 'comptant'],
    [2, 'crédit'],
    [3, 'remise'],
    [4, 'net'],
    [5, 'clients'],
  ] as const;

  /** Somme d'une colonne sur toutes les lignes visibles. */
  const sommeColonne = async (colonne: number): Promise<number> => {
    // Lecture en UN seul passage dans la page : le tableau se redessine après un changement de
    // regroupement, et une ligne lue après coup n'existe plus.
    const textes = await lignes.evaluateAll(
      (rangees, rang) => rangees.map(r => (r.querySelectorAll('td')[rang] as HTMLElement | undefined)?.textContent ?? ''),
      colonne,
    );
    return textes.reduce((cumul, texte) => cumul + Number(texte.replace(/[^\d-]/g, '')), 0);
  };

  const totauxDuPied = async (): Promise<number[]> =>
    Promise.all(COLONNES.map(([colonne]) => nombre(total, colonne)));

  await etape(1, async () => {
    await page.goto('/comptabilite');
    await ouvrirOnglet(page, /Tableau pharmacien/);
    await expect(page.getByText('Tableau de bord pharmacien')).toBeVisible();
  });

  let totauxJournaliers: number[] = [];

  await etape(2, async () => {
    await saisirDate(page, 'dateDebut', debut);
    await saisirDate(page, 'dateFin', fin);
    await rechercher(page);
    // Le regroupement par défaut est journalier : sur trois mois, plusieurs lignes de dates
    // distinctes. Une seule ligne signifierait que la période n'a pas été prise en compte.
    await expect(lignes.nth(1)).toBeVisible();
    await expect(lignes.first()).toContainText(/\d{2}\/\d{2}\/\d{4}/);

    // 1. Le pied de table totalise ce que les lignes portent — et rien de plus.
    for (const [colonne, libelle] of COLONNES) {
      expect(await nombre(total, colonne), `total ${libelle}`).toBe(await sommeColonne(colonne));
    }

    totauxJournaliers = await totauxDuPied();
    expect(
      totauxJournaliers[3],
      'un tableau dont tout est à zéro ne prouverait rien',
    ).toBeGreaterThan(0);
  });

  await etape(3, async () => {
    // Les deux regroupements sont des boutons radio étiquetés : `getByLabel` les atteint par
    // leur libellé visible, sans avoir à connaître leur identifiant.
    await page.getByLabel('Mensuel').check();
    await rechercher(page);
    // Le mensuel condense : la colonne DATE ne porte plus un jour mais un mois. C'est le
    // seul changement visible dans la forme.
    await expect(lignes.first()).not.toContainText(/\d{2}\/\d{2}\/\d{4}/);
    await expect(lignes.first()).toContainText(/\d{4}/);

    // 2. Condenser n'est pas recalculer : ce sont les mêmes ventes, donc les mêmes totaux. Le
    // tableau se rafraîchit à l'arrivée de la réponse : on relit jusqu'à ce qu'il soit stable.
    const ecarts = async (): Promise<string[]> => {
      const problemes: string[] = [];
      const totauxMensuels = await totauxDuPied();
      for (const [rang, [colonne, libelle]] of COLONNES.entries()) {
        if (totauxMensuels[rang] !== totauxJournaliers[rang]) {
          problemes.push(`total ${libelle} après regroupement mensuel : ${totauxMensuels[rang]} au lieu de ${totauxJournaliers[rang]}`);
        }
        // Et le pied continue de totaliser ses lignes, moins nombreuses.
        const somme = await sommeColonne(colonne);
        if (totauxMensuels[rang] !== somme) {
          problemes.push(`total mensuel ${libelle} : ${totauxMensuels[rang]} au lieu de ${somme}`);
        }
      }
      return problemes;
    };
    await expect.poll(ecarts, { timeout: 15_000, intervals: [500, 1000] }).toEqual([]);
  });
});
