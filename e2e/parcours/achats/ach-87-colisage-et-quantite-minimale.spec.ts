import { expect } from '@playwright/test';
import { ajouterLigneCommande, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le grossiste ne livre pas à l'unité tout ce qu'il vend : certains produits partent par colis
 * entier, d'autres ont une quantité plancher sous laquelle la commande est refusée. Tant que
 * l'application laisse saisir n'importe quelle quantité, l'écart se découvre à la réception —
 * et il faut alors le justifier au rapprochement de facture, où il se mêle aux écarts qui, eux,
 * comptent vraiment.
 *
 * Le choix retenu est d'AJUSTER ET D'INFORMER plutôt que de refuser : un blocage obligerait à
 * ressaisir, une correction muette donnerait l'impression d'un logiciel qui change les chiffres
 * tout seul. La quantité monte donc au multiple supérieur du colis — ou au minimum du
 * fournisseur — et un message dit laquelle des deux contraintes a décidé.
 *
 * Les deux produits sont choisis pour leur fiche fournisseur, pas pour leur nature : l'un est
 * livré par colis de 10, l'autre exige 5 unités au minimum. Les parcours ACH-01 et ACH-03
 * travaillent sur des produits sans contrainte, et ne voient donc rien de ce mécanisme.
 *
 * Parcours ÉCRIVANT dans la base : il ajoute deux lignes à une commande en attente, et les
 * retire.
 */
scenario('ACH-87', async ({ etape, page }) => {
  // Les deux produits viennent du MÊME catalogue, et c'est une contrainte, pas un détail : une
  // commande n'a qu'un fournisseur, et la recherche de produit est bornée à son catalogue. Une
  // agence n'a pas de catalogue propre — c'est celui de son grossiste qui sert, ici DPCI.
  // Colis de 10 unités, aucun minimum.
  const produitColise = 'ADARTREL 0,50 mg';
  // Minimum de 5 unités, pas de colisage.
  const produitMinimum = 'SAVON LIQUIDE MAINS 100ML';
  // L'agence dont le grossiste porte les deux contraintes.
  const agence = 'DPCI ABIDJAN MARCORY';
  const grille = page.locator('app-commande-requested');

  const ligne = (produit: string) => page.locator('.ag-row').filter({ hasText: produit }).first();
  const quantite = (produit: string) => ligne(produit).locator('[col-id="quantityRequested"]');
  const message = (motif: RegExp) => page.locator('[role="alert"], .toast, ngb-toast').filter({ hasText: motif }).first();

  // Mise en scène : on se place dans une commande en attente déjà existante.
  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);
  await page.getByRole('tab', { name: /Commandes fournisseurs/ }).click();
  const liste = page.locator('app-commande-requested-home');
  await expect(liste.locator('tbody tr').first()).toBeVisible();
  // PAS la première ligne, et on CHERCHE le bon : la liste est paginée, et la ligne qu'il nous
  // faut est celle du fournisseur dont le catalogue porte les deux contraintes.
  await page.getByPlaceholder(/Rechercher \(fournisseur/).fill(agence);
  const bon = liste.locator('tbody tr').filter({ hasText: agence }).first();
  await expect(bon).toBeVisible();

  // Mise en scène, comme ACH-88 : un bon daté est VERROUILLÉ — sa barre d'ajout est neutralisée,
  // et aucune ligne n'y entre. On le ramène au brouillon plutôt que de miser sur l'état où le
  // jeu de démonstration l'a laissé : le tirage change à chaque rechargement.
  if (
    await bon
      .getByText('ENVOYÉE')
      .isVisible()
      .catch(() => false)
  ) {
    await bon.locator('app-commande-requested-actions button').first().click();
    await page.getByRole('button', { name: /Annuler l'envoi/ }).click();
    await page
      .locator('.modal-content')
      .getByRole('button', { name: /Oui|Confirmer/ })
      .click();
    await expect(bon.getByText('BROUILLON')).toBeVisible();
  }

  await bon.dblclick();
  await expect(grille).toBeVisible();

  await etape(1, async () => {
    // Sept unités demandées d'un produit livré par colis de dix : la ligne ne peut pas
    // exister à 7, elle part à 10.
    await ajouterLigneCommande(page, produitColise, '7');
    await expect(grille).toContainText(produitColise);
    await expect(message(/colis de 10/i)).toBeVisible();
    await expect(quantite(produitColise)).toContainText('10');
  });

  await etape(2, async () => {
    // Quatorze unités tombent entre deux colis : deux colis partent, soit vingt unités. Le
    // message nomme le nombre de colis, qui est ce que le pharmacien recevra réellement.
    await quantite(produitColise).click();
    await quantite(produitColise).locator('input').fill('14');
    await page.keyboard.press('Enter');
    await expect(message(/2 colis de 10/i)).toBeVisible();
    await expect(quantite(produitColise)).toContainText('20');
  });

  await etape(3, async () => {
    // Autre contrainte, autre message : ce produit n'a pas de colisage mais un plancher de
    // cinq unités. Deux unités demandées deviennent cinq.
    await ajouterLigneCommande(page, produitMinimum, '2');
    await expect(grille).toContainText(produitMinimum);
    await expect(message(/minimum fournisseur de 5/i)).toBeVisible();
    await expect(quantite(produitMinimum)).toContainText('5');
  });

  // ── Remise en état : les deux lignes ajoutées sont retirées. ────────────────────────────
  for (const produit of [produitColise, produitMinimum]) {
    if (
      await ligne(produit)
        .isVisible()
        .catch(() => false)
    ) {
      await ligne(produit).locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
      const supprimer = page.getByRole('button', { name: /Supprimer \(/ });
      if (await supprimer.isVisible().catch(() => false)) {
        await supprimer.click();
        const confirmation = page.locator('.modal-content');
        if (await confirmation.isVisible().catch(() => false)) {
          await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
        }
      }
    }
  }
});
