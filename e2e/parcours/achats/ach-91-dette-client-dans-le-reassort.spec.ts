import { expect } from '@playwright/test';
import { ouvrirOnglet } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le réassort comparait le stock objectif au stock PHYSIQUE.
 *
 * Or une partie de ce stock peut être déjà promise : un produit servi partiellement au comptoir
 * laisse un avoir client, et les unités correspondantes sont dans le rayon mais réservées. Les
 * compter comme disponibles surestimait la couverture d'exactement la dette, et la commande
 * partait trop tard — pour un client qui avait déjà payé.
 *
 * La dette est maintenant retranchée du stock avant la comparaison, et non ajoutée au résultat :
 * quand le stock dépasse déjà l'objectif, aucune commande n'est déclenchée, là où une addition en
 * aurait provoqué une inutile.
 *
 * ── Pourquoi le produit est DÉSIGNÉ par la base ─────────────────────────────────────────────
 * Le parcours nommait un produit et récitait son arithmétique (« stock 4, dette 1, objectif 10 →
 * 7 »). Ces trois nombres sont le résultat des ventes, des avoirs et du stock du jeu de démo :
 * ils bougent à chaque régénération. Celui-ci s'est retrouvé couvert — dette 0, rien à commander
 * — donc absent d'un écran qui ne liste que ce qu'il faut commander, et le parcours cherchait un
 * décor disparu.
 *
 * On demande donc à la base un produit qui porte une dette ET une quantité à commander, puis on
 * vérifie la RÈGLE sur ses nombres à lui :
 *
 *     quantité à commander = objectif − (stock physique − dette)
 *
 * C'est plus fort que la mémoire d'un cas : l'égalité est fausse dès que la dette cesse d'être
 * retranchée, quel que soit le produit tiré.
 *
 * Parcours en LECTURE SEULE : il n'écrit rien.
 */
scenario('ACH-91', async ({ etape, page }) => {
  // L'écran n'affiche pas la quantité de la vue mais la quantité NETTE, celle qui reste après
  // soustraction de ce qui est déjà commandé et parti (`loadPendingOrderQtyBatch`), et il écarte
  // les lignes retombées à zéro. On choisit donc un produit sans commande en cours : la vue et
  // l'écran disent alors le même nombre, et la règle se lit sans terme correctif.
  const sujet = lire(
    `WITH attente AS (
       SELECT fp.produit_id, SUM(ol.quantity_requested - COALESCE(ol.quantity_received, 0)) AS en_attente
         FROM order_line ol
         JOIN fournisseur_produit fp ON fp.id = ol.fournisseur_produit_id
         JOIN commande c ON c.id = ol.commande_id AND c.order_date = ol.commande_order_date
        WHERE c.order_status = 'REQUESTED' AND c.date_envoi IS NOT NULL
          AND ol.quantity_requested > COALESCE(ol.quantity_received, 0)
        GROUP BY fp.produit_id
       HAVING SUM(ol.quantity_requested - COALESCE(ol.quantity_received, 0)) > 0)
     SELECT v.libelle || '|' || v.stock_actuel || '|' || v.dette_client || '|' || v.stock_objectif
            || '|' || v.quantite_a_commander || '|' || m.type_magasin || '|' || m.name
       FROM v_semois_suggestion v
       JOIN magasin m ON m.id = v.magasin_id
       LEFT JOIN attente a ON a.produit_id = v.produit_id
      WHERE v.dette_client > 0 AND v.quantite_a_commander > 0 AND COALESCE(a.en_attente, 0) = 0
      ORDER BY v.stock_actuel DESC, v.produit_id LIMIT 1`,
  );
  expect(sujet, 'la démo doit porter un produit à la fois endetté et à commander').not.toBe('');
  const [produit, stock, dette, objectif, aCommander, typeMagasin, nomMagasin] = sujet.split('|');
  const attendu = Number(objectif) - (Number(stock) - Number(dette));

  // Le contrôle qui porte la règle. Il ne dépend d'aucun produit nommé : seul le fait de
  // retrancher la dette avant la comparaison rend cette égalité vraie.
  expect(Number(aCommander), `${produit} : objectif ${objectif} − (stock ${stock} − dette ${dette}) doit donner ${attendu}`).toBe(attendu);

  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Tableau de bord stock/ }).click();
    await expect(page.locator('app-semois-suggestions')).toBeVisible();
  });

  const ligneAvecDette = page.locator('app-semois-suggestions tbody tr').filter({
    has: page.locator('.semois-dette'),
  });

  /*
   * Le réassort est propre au MAGASIN COMMANDEUR.
   *
   * Depuis que l'unité de gestion commande en propre, le sujet tiré peut être le sien, et
   * l'écran s'ouvre sur le réassort de l'officine — c'est le défaut assumé côté serveur, un
   * autre magasin « demande explicitement » le sien. On le demande donc par le sélecteur, comme
   * le ferait un responsable qui change d'établissement. Sans cela le parcours cherchait sur la
   * liste de l'officine un produit suggéré à la parapharmacie, et accusait l'écran d'avoir perdu
   * la dette.
   */
  if (typeMagasin !== 'OFFICINE') {
    const selecteurMagasin = page.locator('ng-select').filter({ has: page.locator('.ng-placeholder', { hasText: 'Magasin' }) });
    await expect(selecteurMagasin, 'le sélecteur de magasin paraît dès qu’un second magasin commande').toBeVisible();
    await selecteurMagasin.click();
    await page.locator('.ng-option', { hasText: nomMagasin }).first().click();
    await expect(selecteurMagasin).toContainText(nomMagasin);
  }

  await etape(2, async () => {
    // On CHERCHE le produit au lieu d'espérer le voir : le réassort en suggère des centaines et
    // une poignée seulement portent une dette.
    //
    // La recherche est RETAPÉE jusqu'à ce que la ligne paraisse. L'écran lance de front le
    // chargement des magasins, des fournisseurs, des statistiques et des suggestions ; une
    // recherche saisie avant que tout cela ne soit retombé reste sans effet, et le tableau
    // affiche « Aucune suggestion disponible » sur un produit pourtant suggéré.
    const recherche = page.getByPlaceholder('Rechercher un produit...');
    await expect(async () => {
      await recherche.fill('');
      await recherche.fill(produit.slice(0, 24));
      await expect(ligneAvecDette.first()).toBeVisible({ timeout: 5000 });
    }).toPass({ timeout: 40_000, intervals: [500, 1000, 2000, 3000] });
    await expect(ligneAvecDette.first().locator('.semois-dette')).toHaveText(new RegExp(`−${dette} dû`));
  });

  await etape(3, async () => {
    // Le stock affiché reste le stock physique : c'est ce qu'on compte en rayon. La dette est
    // montrée à part, sinon la quantité proposée passerait pour une erreur de calcul — et c'est
    // bien la quantité calculée avec la dette retranchée qui est proposée.
    const premiere = ligneAvecDette.first();
    await expect(premiere.locator('.semois-dette')).toBeVisible();
    await expect(premiere).toContainText(String(attendu));
  });
});
