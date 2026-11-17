import { expect } from '@playwright/test';
import { chercherProduit, seConnecterEnTantQue } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Chaque magasin tient SON stock. Le même article peut donc afficher trente-six unités à la
 * parapharmacie et quatre à l'officine — et le vendeur de la parapharmacie n'a pas à se
 * demander lequel des deux il consulte : le comptoir lui montre celui du magasin où il est
 * connecté, puisque c'est celui-là qu'il va engager.
 *
 * Ce parcours se joue donc AVEC LE COMPTE DE L'UNITÉ, et non celui de l'administrateur : joué
 * en administrateur, il montrerait le stock de l'officine et ne démontrerait rien. Le compte
 * est pris en base par son magasin, pas par son nom — un login figé serait faux dès que le jeu
 * de démonstration nommerait autrement son vendeur.
 *
 * Parcours en LECTURE : il sélectionne un article sans l'ajouter au panier, donc sans ouvrir de
 * vente. Il le vérifie à la fin — une vente en cours laissée derrière lui gênerait les
 * parcours suivants, qui travaillent sur le même compte au comptoir.
 */
scenario('VTE-77', async ({ etape, page }) => {
  const vendeur = lire(`SELECT u.login FROM app_user u JOIN magasin m ON m.id = u.magasin_id
                         WHERE m.type_magasin = 'UNITE_GESTION' AND u.activated ORDER BY u.id LIMIT 1`);
  expect(vendeur, 'la démo doit porter un compte affecté à l’unité de gestion').not.toBe('');

  // Un article tenu DANS LES DEUX magasins, en quantités différentes : c'est l'écart qui fait
  // la démonstration. Sans écart, l'écran pourrait montrer l'un ou l'autre sans qu'on le voie.
  // Les libellés à virgule ou à tiret sont écartés : la recherche du comptoir ne les tolère pas.
  const sujet = lire(`
    SELECT p.libelle || '|' || (para.qty_stock + para.qty_ug) || '|' || (offi.qty_stock + offi.qty_ug)
      FROM magasin u
      JOIN storage su ON su.magasin_id = u.id AND su.storage_type = 'PRINCIPAL'
      JOIN stock_produit para ON para.storage_id = su.id
      JOIN produit p ON p.id = para.produit_id
      JOIN magasin o ON o.type_magasin = 'OFFICINE'
      JOIN storage so ON so.magasin_id = o.id AND so.storage_type = 'PRINCIPAL'
      JOIN stock_produit offi ON offi.produit_id = para.produit_id AND offi.storage_id = so.id
     WHERE u.type_magasin = 'UNITE_GESTION'
       AND p.status = 'ENABLE'
       AND p.libelle NOT LIKE '%,%' AND p.libelle NOT LIKE '%-%'
       AND para.qty_stock > 5
       AND (para.qty_stock + para.qty_ug) <> (offi.qty_stock + offi.qty_ug)
     ORDER BY abs((para.qty_stock + para.qty_ug) - (offi.qty_stock + offi.qty_ug)) DESC, p.id
     LIMIT 1`);
  expect(sujet, 'la démo doit porter un article tenu dans les deux magasins, en quantités différentes').toContain('|');
  const [article, stockUnite, stockOfficine] = sujet.split('|');

  const ventesDuVendeur = (): string =>
    lire(`SELECT count(*) FROM sales s JOIN app_user u ON u.id = s.user_id
           WHERE u.login = '${vendeur}' AND s.statut <> 'CLOSED'`);
  const avant = ventesDuVendeur();

  await etape(1, async () => {
    await seConnecterEnTantQue(page, vendeur, 'admin');
  });

  await etape(2, async () => {
    await page.goto('/sales-home');
    await expect(page.locator('#produitbox')).toBeVisible();
  });

  await etape(3, async () => {
    await chercherProduit(page, article);
    // « Rayon » désigne le stock de vente du magasin connecté. C'est le nombre que le vendeur
    // lit avant d'annoncer une disponibilité.
    const panneau = page.locator('.product-meta');
    // Le panneau concatène ses éléments sans séparateur — « Rayon: 36Prix: 7 655 FCFA » — donc
    // pas de limite de mot après le nombre : on exige seulement qu'aucun chiffre ne le suive,
    // sans quoi « 3 » passerait pour « 36 ».
    await expect(panneau).toContainText(new RegExp(`Rayon:\\s*${stockUnite}(?!\\d)`));
    // Et surtout : ce n'est PAS celui de l'officine. L'assertion précédente passerait par
    // hasard si les deux chiffres se ressemblaient — la requête les a choisis différents.
    expect(stockUnite, 'les deux stocks doivent différer pour que la démonstration vaille').not.toBe(stockOfficine);
  });

  // Aucune vente ouverte : sélectionner un article n'en crée pas, seul l'ajout au panier le fait.
  expect(ventesDuVendeur(), `« ${vendeur} » ne doit pas laisser de vente en cours`).toBe(avant);
});
