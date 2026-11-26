import { expect } from '@playwright/test';
import { chercherAuCatalogue, seConnecterEnTantQue } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Un rayon est rattaché à un STOCKAGE, donc à un seul magasin : le même article porte une
 * adresse propre dans chaque établissement, et n'y est pas détenu en même quantité. Celui qui
 * cherche l'article, ou qui propose un équivalent, doit lire ce qui vaut CHEZ LUI.
 *
 * Cinq lectures du logiciel prenaient le premier rangement venu — l'ordre d'itération d'un
 * ensemble, donc rien de reproductible — et l'une d'elles ne calculait même pas la quantité :
 * la colonne « Stock » de l'onglet des génériques affichait zéro pour tout le monde,
 * l'officine comprise. Les deux défauts ont été trouvés en répondant à la question « un rayon
 * est-il propre à un magasin ? », et ce parcours est ce qui les empêche de revenir.
 *
 * Parcours en LECTURE : il ne touche ni au rangement ni au stock.
 */
scenario('REF-70', async ({ etape, page }) => {
  /*
   * Deux sujets, parce que les deux lectures ne vivent pas sur le même article.
   *
   * Le premier est rangé dans les DEUX magasins, à des rayons dont aucun nom n'est contenu dans
   * l'autre — sans quoi « n'annonce pas l'autre » ne voudrait rien dire.
   */
  const partage = lire(`
    WITH rayons AS (
      SELECT rp.produit_id,
             max(CASE WHEN m.type_magasin = 'OFFICINE' THEN r.libelle END) AS rayon_officine,
             max(CASE WHEN m.type_magasin = 'UNITE_GESTION' THEN r.libelle END) AS rayon_unite
        FROM rayon_produit rp
        JOIN rayon r ON r.id = rp.rayon_id
        JOIN storage s ON s.id = r.storage_id
        JOIN magasin m ON m.id = s.magasin_id
       GROUP BY rp.produit_id
    )
    SELECT p.libelle || '|' || ra.rayon_officine || '|' || ra.rayon_unite
      FROM rayons ra
      JOIN produit p ON p.id = ra.produit_id
     WHERE p.status = 'ENABLE'
       AND p.libelle NOT LIKE '%,%' AND p.libelle NOT LIKE '%-%'
       AND ra.rayon_officine IS NOT NULL AND ra.rayon_unite IS NOT NULL
       AND position(ra.rayon_officine in ra.rayon_unite) = 0
       AND position(ra.rayon_unite in ra.rayon_officine) = 0
     ORDER BY p.id
     LIMIT 1`);
  expect(partage, 'il faut un article rangé dans les deux magasins, à deux adresses').toContain('|');
  const [article, rayonOfficine, rayonUnite] = partage.split('|');

  /*
   * Le second porte des équivalents détenus à l'officine, et la somme de leurs stocks est le
   * chiffre que l'onglet doit annoncer — celui qui s'affichait à zéro pour tout le monde.
   *
   * Ce sont des MÉDICAMENTS : la parapharmacie n'en détient aucun, et son catalogue ne les
   * liste donc pas. C'est pourquoi la lecture des équivalents ne se refait pas de l'autre côté,
   * et pourquoi l'étape 3 vérifie à la place qu'un magasin ne voit que ce qu'il détient.
   */
  const sujet = lire(`
    SELECT p.libelle || '|' || split_part(p.libelle, ' ', 1) || '|' ||
           sum(sp.qty_stock + sp.qty_ug) || '|' || count(*)
      FROM substitut sub
      JOIN produit p ON p.id = sub.produit_id
      JOIN stock_produit sp ON sp.produit_id = sub.substitut_id
      JOIN storage s ON s.id = sp.storage_id AND s.storage_type = 'PRINCIPAL'
      JOIN magasin m ON m.id = s.magasin_id AND m.type_magasin = 'OFFICINE'
     WHERE p.status = 'ENABLE'
     GROUP BY p.id, p.libelle
    HAVING min(sp.qty_stock) > 0
     ORDER BY count(*) DESC, p.id
     LIMIT 1`);
  expect(sujet, 'il faut un article dont les équivalents sont détenus à l’officine').toContain('|');
  const [aEquivalents, premierMot, stockOfficine, nombreEquivalents] = sujet.split('|');

  // Le compte de l'unité qui ouvre le catalogue : la vendeuse n'y a pas accès, le responsable
  // de commande si. Et il ne SUPERVISE rien, donc il ne voit que son magasin.
  const compteUnite = lire(`
    SELECT u.login
      FROM app_user u
      JOIN magasin m ON m.id = u.magasin_id AND m.type_magasin = 'UNITE_GESTION'
      JOIN user_authority ua ON ua.user_id = u.id AND ua.authority_name = 'ROLE_RESPONSABLE_COMMANDE'
     WHERE u.activated
     ORDER BY u.login
     LIMIT 1`);
  expect(compteUnite, 'il faut un compte de l’unité qui ouvre le catalogue').not.toBe('');

  const ligne = page.locator('app-produit-list tbody tr').filter({ visible: true }).first();
  const equivalents = page.locator('app-produit-generiques-tab tbody tr').filter({ visible: true });

  const ouvrirLeCatalogue = async (): Promise<void> => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, article);
  };

  const ouvrirLesEquivalents = async (): Promise<void> => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, aEquivalents);
    await page.locator('tbody tr').filter({ visible: true }).first().click();
    await page.getByRole('tab', { name: 'Génériques' }).click();
    await expect(equivalents).toHaveCount(Number(nombreEquivalents));
  };

  /**
   * La somme des quantités annoncées par la colonne « Stock » de l'onglet.
   *
   * Cellule par cellule DE CHAQUE LIGNE : `equivalents.locator('td').nth(4)` désigne la
   * cinquième cellule de tout le tableau, pas la cinquième de chacune, et ne rendait donc que
   * le premier équivalent.
   */
  const sommeAffichee = async (): Promise<number> => {
    const lignes = await equivalents.all();
    let total = 0;
    for (const ligneEquivalent of lignes) {
      const texte = await ligneEquivalent.locator('td').nth(4).innerText();
      total += Number(texte.replace(/[^\d]/g, '') || 0);
    }
    return total;
  };

  await etape(1, async () => {
    await ouvrirLeCatalogue();
    await expect(ligne, 'le catalogue de l’officine annonce le rayon de l’officine').toContainText(rayonOfficine);
    await expect(ligne, 'il n’annonce pas celui d’un autre établissement').not.toContainText(rayonUnite);
  });

  await etape(2, async () => {
    await ouvrirLesEquivalents();
    // Le chiffre de la base, et non « un chiffre non nul » : c'est la quantité détenue ici qui
    // doit s'afficher, et elle ne s'affichait pas du tout avant correction.
    expect(await sommeAffichee(), 'les équivalents sont annoncés au stock de l’officine').toBe(Number(stockOfficine));
  });

  await etape(3, async () => {
    await seConnecterEnTantQue(page, compteUnite, 'admin');
    await ouvrirLeCatalogue();
    await expect(ligne, 'le catalogue de l’unité annonce SON rayon').toContainText(rayonUnite);
    await expect(ligne, 'et pas celui de l’officine').not.toContainText(rayonOfficine);

    // Et le catalogue s'arrête à ce que le magasin détient : le médicament de l'étape 2 n'y
    // figure pas. C'est la même règle, vue par son autre face.
    const champ = page.getByPlaceholder(/Rechercher \(CIP/);
    await champ.fill('');
    await champ.fill(premierMot);
    await champ.press('Enter');
    await expect(page.locator('app-produit-list'), 'une parapharmacie ne trouve pas les médicaments de l’officine').not.toContainText(
      aEquivalents,
    );
  });
});
