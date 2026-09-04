import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Deux façons de réceptionner, pour deux moments du travail. Le mode SÉQUENTIEL sert au
 * carton ouvert : une ligne à l'écran, les mains sur les boîtes. La vue TABLEAU sert à la
 * relecture : on voit le bon entier, on compare les lignes entre elles, et le panneau de
 * concordance — absent du mode séquentiel — chiffre les écarts de quantité, de prix et de
 * colisage.
 *
 * La grille est aussi la seule à donner accès à la SAISIE DES LOTS sans quitter le bon : la
 * colonne « Lots », épinglée à droite, déplie sous chaque ligne un éditeur — numéro,
 * péremption, quantité, unités gratuites — dont les champs se valident par Entrée et
 * s'abandonnent par Échap. Le mode séquentiel, lui, renvoie vers un écran séparé.
 *
 * Le mode est MÉMORISÉ sur le poste (`reception-view-mode` dans le stockage local) : ce n'est
 * pas un réglage de la réception en cours mais une habitude de travail, qui survit à la
 * fermeture du navigateur. D'où la dernière étape, qui rouvre un bon pour le montrer.
 *
 * Parcours en LECTURE : il ne modifie aucune ligne et ne finalise rien. Il rend le poste au
 * mode séquentiel en partant, celui-ci étant le mode nominal.
 */
scenario('ACH-84', async ({ etape, page }) => {
  const liste = page.locator('app-list-bons');
  const sequentiel = page.locator('app-reception-sequential');
  const concordance = page.locator('app-reception-concordance');
  const editeurLots = page.locator('app-lot-inline-editor');

  /** Ouvre un bon EN ATTENTE DE SAISIE depuis la liste : un bon clôturé n'a plus de mode. */
  const ouvrirUnBon = async (): Promise<void> => {
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').filter({ hasNotText: 'Clôturé' }).first().dblclick();
  };

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: /Réceptions/ }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();

    // Le poste est remis à son état de sortie d'usine AVANT d'ouvrir le bon : le mode est lu
    // à la création de l'écran de réception, et une campagne précédente aurait pu laisser la
    // grille. Sans cela, la première capture montrerait déjà le tableau — soit l'inverse de
    // ce que la légende annonce.
    await page.evaluate(() => localStorage.removeItem('reception-view-mode'));

    await ouvrirUnBon();
    // Le mode nominal : une seule ligne à l'écran, le curseur dans la quantité reçue.
    await expect(sequentiel).toBeVisible();
    await expect(concordance).toHaveCount(0);
  });

  await etape(2, async () => {
    // Le sélecteur est un segment à deux positions ; « Grille » est la seconde. Ctrl+G fait
    // la même bascule sans la souris, mais c'est le bouton qu'il faut montrer : le raccourci
    // ne se photographie pas.
    await page.getByRole('button', { name: 'Grille' }).click();

    // Ce que la vue tableau apporte, et que le mode séquentiel n'a pas : le bon entier, et le
    // panneau de concordance qui compte les anomalies par nature.
    await expect(sequentiel).toHaveCount(0);
    await expect(concordance).toBeVisible();
    await expect(concordance).toContainText('Écarts qté');
    // Le panneau se replie — c'est le sens du bouton, qui n'apparaît qu'ici.
    await expect(page.getByRole('button', { name: /Fermer récap|Récap BL/ })).toBeVisible();
  });

  await etape(3, async () => {
    // Le chevron de la colonne « Lots » ouvre l'éditeur SOUS la ligne, sans fenêtre ni
    // changement d'écran : c'est ce qui permet de reprendre les lots au fil de la relecture
    // du bon. La touche Espace sur la cellule fait la même chose, mais ne se photographie pas.
    const chevron = page.locator('app-lot-expand-cell .lec-chevron').first();
    await expect(chevron).toBeVisible();
    await chevron.click();
    await expect(editeurLots).toBeVisible();

    // Le formulaire d'AJOUT n'apparaît que sur une ligne dont la quantité reçue n'est pas
    // encore couverte — sur un bon complet, il n'y en a aucune. C'est donc la modification
    // d'un lot posé qui montre les champs, et ce sont les mêmes : numéro, péremption,
    // quantité, UG. Le crayon n'a qu'une infobulle, que `getByRole` ne lit pas.
    await editeurLots.locator('app-button[ngbtooltip="Modifier ce lot"] button').first().click();
    const ligneEnEdition = editeurLots.locator('.lie-row-editing');
    await expect(ligneEnEdition.getByPlaceholder('N° lot')).toBeVisible();
    // La quantité et les UG n'ont pas de texte d'invite en modification — la valeur du lot y
    // est déjà —, on les désigne donc par leur classe.
    await expect(ligneEnEdition.locator('.lie-field--qty')).toBeVisible();

    // Rien n'est modifié : le parcours montre l'ACCÈS à la saisie, pas une écriture de lot,
    // qui changerait le bon. L'édition reste ouverte jusqu'à la capture — la refermer ici
    // donnerait une image qui ne montre pas ce que la légende annonce — et est abandonnée
    // sans enregistrement au retour à la liste, à l'étape suivante.
  });

  await etape(4, async () => {
    // Retour à la liste puis réouverture : le mode n'a pas été redemandé, et pourtant le bon
    // s'ouvre en tableau. C'est la mémorisation sur le poste, seule chose que cette étape
    // prouve — d'où le passage par la liste plutôt qu'une bascule directe.
    await page.getByRole('button', { name: 'Retour à la liste' }).click();
    await ouvrirUnBon();
    await expect(concordance).toBeVisible();
    await expect(sequentiel).toHaveCount(0);
  });

  // ── Remise en état : le poste repart en séquentiel, mode nominal de la réception. ───────
  await page.getByRole('button', { name: 'Séquentiel' }).click();
  await expect(sequentiel).toBeVisible();
});
