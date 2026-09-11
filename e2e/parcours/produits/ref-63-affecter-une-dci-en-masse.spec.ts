import { expect } from '@playwright/test';
import { chercherAuCatalogue, chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un catalogue repris d'un autre logiciel arrive presque toujours SANS DCI. Tant que la
 * substance active manque, la substitution générique ne propose rien : deux boîtes de
 * paracétamol restent, pour l'application, deux produits sans rapport. Le rattachement est
 * donc un travail de reprise — des dizaines de fiches d'affilée —, pas une correction de
 * fiche isolée. D'où l'affectation en masse, depuis la sélection du catalogue.
 *
 * Rien n'est perdu si l'on se trompe : la DCI choisie REMPLACE celle que les produits
 * portaient, et l'opération se défait en réaffectant. C'est ce qui distingue ce geste de la
 * fusion (REF-62), irréversible, et ce qui explique qu'aucune confirmation ne le précède —
 * la fenêtre, qui rappelle les produits retenus, en tient lieu.
 *
 * Parcours ÉCRIVANT, et qui se nettoie. Il travaille sur le catalogue de démonstration —
 * de vrais DOLIPRANE, ce que le manuel doit montrer — puis REPOSE sur chaque fiche la DCI
 * qu'elle portait, ou l'efface si elle n'en portait aucune. La remise en état ne peut pas
 * passer par la fenêtre d'affectation, qui remplace mais n'efface pas : elle passe fiche par
 * fiche, dont le champ DCI est effaçable. Sans elle, le parcours laisserait une substance
 * sur des produits du jeu de démonstration et changerait ce que les parcours de substitution
 * proposent ensuite.
 */
scenario('REF-63', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');
  const selectDci = page.locator('ng-select', { has: page.locator('#f_dci') });

  /**
   * Ouvre en édition la fiche d'un produit du catalogue, par son libellé, et se place sur
   * l'onglet où vit la DCI. Le champ n'est pas seulement caché hors de cet onglet : il n'est
   * pas rendu du tout, et le lire sans l'ouvrir rendrait « aucune DCI » pour tout le monde.
   */
  const ouvrirLaFiche = async (libelle: string): Promise<void> => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, libelle);
    await lignes.filter({ hasText: libelle }).first().getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Éditer' }).click();
    await expect(page.locator('#f_libelle')).toBeVisible();
    await ouvrirOnglet(page, /Classification/);
    await expect(selectDci).toBeVisible();
  };

  /** La DCI que porte la fiche ouverte — chaîne vide si elle n'en porte aucune. */
  const dciDeLaFiche = async (): Promise<string> => {
    const etiquette = selectDci.locator('.ng-value-label');
    return (await etiquette.count()) > 0 ? (await etiquette.first().innerText()).trim() : '';
  };

  // ── Relevé préalable, hors étape : quels produits vont être touchés, et quelle DCI
  //    portent-ils aujourd'hui. C'est ce relevé qui rend la remise en état possible ; sans
  //    lui elle ne saurait pas quoi reposer. Rien de tout cela n'appartient au scénario,
  //    d'où la place en dehors des étapes — et hors des captures.
  await page.goto('/produits');
  await chercherAuCatalogue(page, 'DOLIPRANE');
  const cases = lignes.getByRole('checkbox', { name: /^Sélectionner / });
  const cibles: string[] = [];
  for (const rang of [0, 1]) {
    const nom = (await cases.nth(rang).getAttribute('aria-label')) ?? '';
    cibles.push(nom.replace(/^Sélectionner\s*/, '').trim());
  }
  expect(cibles.filter(Boolean)).toHaveLength(2);

  const dciInitiales: string[] = [];
  for (const libelle of cibles) {
    await ouvrirLaFiche(libelle);
    dciInitiales.push(await dciDeLaFiche());
  }

  await etape(1, async () => {
    await page.goto('/produits');
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    // Filtrer d'abord : la sélection se fait sur ce qu'on voit, et cocher deux lignes prises
    // au hasard dans un catalogue de plusieurs milliers de références n'illustrerait aucun
    // travail réel. Une reprise se fait molécule par molécule.
    await chercherAuCatalogue(page, 'DOLIPRANE');

    // On coche les deux fiches relevées plus haut — par leur libellé, et non par leur rang :
    // c'est le seul ancrage qui garantisse que la remise en état porte sur les mêmes.
    for (const libelle of cibles) {
      await lignes.filter({ hasText: libelle }).first().getByRole('checkbox').check();
    }
    // La barre d'actions groupées n'apparaît qu'à partir d'une sélection : c'est elle qui
    // porte « Affecter une DCI », et c'est ce qu'il faut voir sur la capture.
    await expect(page.locator('.bulk-action-bar')).toContainText('2 produit(s) sélectionné(s)');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Affecter une DCI' }).click();
    await expect(modal).toBeVisible();
    // Le titre rappelle le nombre de fiches concernées, et le corps les nomme : c'est la
    // seule protection avant une opération qui en touche plusieurs d'un coup.
    await expect(modal).toContainText('Affecter une DCI à 2 produit(s)');
    await expect(modal).toContainText('Produits concernés');

    // Le champ interroge le SERVEUR à partir de deux caractères : la liste est vide tant
    // qu'on n'a pas tapé, d'où l'aide dédiée plutôt qu'un simple clic.
    await chercherDansSelect(page, 'dci-cible', 'PARACETAMOL');
    await expect(modal.getByRole('button', { name: 'Affecter la DCI' })).toBeEnabled();
  });

  await etape(3, async () => {
    await modal.getByRole('button', { name: 'Affecter la DCI' }).click();

    // Ce que l'écran doit prouver : le nombre de fiches réellement rattachées. Un « c'est
    // fait » sans compte laisserait le doute sur les lignes que la sélection contenait.
    await expect(page.getByText(/2 produit\(s\) rattaché\(s\) à/)).toBeVisible();
    await expect(modal).toHaveCount(0);
    // Et la sélection retombe : la barre groupée disparaît, l'écran est prêt pour la
    // molécule suivante sans qu'on ait à décocher quoi que ce soit.
    await expect(page.locator('.bulk-action-bar')).toHaveCount(0);
  });

  // ── Remise en état : chaque fiche retrouve la DCI qu'elle portait, ou n'en porte plus
  //    aucune. Le catalogue de démonstration ressort exactement comme il est entré. ───────
  for (const [rang, libelle] of cibles.entries()) {
    await ouvrirLaFiche(libelle);
    const initiale = dciInitiales[rang];
    if (initiale === '') {
      await selectDci.locator('.ng-clear-wrapper').click();
      await expect(selectDci.locator('.ng-value-label')).toHaveCount(0);
    } else {
      await chercherDansSelect(page, 'f_dci', initiale);
    }
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
  }
});
