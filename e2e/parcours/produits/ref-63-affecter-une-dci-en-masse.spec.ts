import { expect } from '@playwright/test';
import { chercherAuCatalogue, chercherDansSelect } from '../../src/actions';
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
 * Parcours ÉCRIVANT, mais sans effet : il rattache des DOLIPRANE au PARACÉTAMOL, substance
 * qu'ils portent déjà dans le jeu de démonstration. L'affectation est réellement validée —
 * c'est elle que montre la troisième capture, avec son compte — et la base ressort
 * inchangée. Valider pour de bon était nécessaire : le nombre de produits rattachés est le
 * résultat même du scénario, et une capture prise avant la validation ne le montrerait pas.
 */
scenario('REF-63', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/produits');
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
    // Filtrer d'abord : la sélection se fait sur ce qu'on voit, et cocher deux lignes prises
    // au hasard dans un catalogue de plusieurs milliers de références n'illustrerait aucun
    // travail réel. Une reprise se fait molécule par molécule.
    await chercherAuCatalogue(page, 'DOLIPRANE');

    // Les cases de ligne portent le libellé du produit dans leur nom accessible — le seul
    // ancrage qui ne dépende pas du rang de la ligne dans la page.
    const cases = lignes.getByRole('checkbox', { name: /^Sélectionner / });
    await cases.nth(0).check();
    await cases.nth(1).check();
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
});
