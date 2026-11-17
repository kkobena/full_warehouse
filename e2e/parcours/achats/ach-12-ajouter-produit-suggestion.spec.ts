import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le calcul ne sait pas tout : une rupture constatée au comptoir, une promotion à venir, une
 * commande d'un client — autant de raisons d'ajouter un produit que la VMM n'a pas proposé.
 * La proposition reste donc ouverte à la main, avec le même geste que la commande : chercher
 * le produit, donner la quantité.
 *
 * ── Pourquoi la ligne est cherchée dans la GRILLE, et non lue dans le panneau ───────────────
 * Le tableau des lignes est une grille AG Grid : elle ne rend que les lignes visibles. Chercher
 * le libellé dans le texte du panneau échouait donc sur un produit pourtant ajouté — celui-ci
 * se trie sous « F », loin derrière les quelques dizaines de lignes effectivement peintes. Le
 * nettoyage de fin butait sur le même mur, et laissait la ligne derrière lui : la proposition
 * gardait un produit ajouté par un parcours, et le parcours suivant l'incrémentait.
 *
 * On passe donc par le filtre du tableau, qui est un quick filter AG Grid : il réduit la grille
 * à la ligne cherchée, laquelle est alors rendue — et sélectionnable pour le nettoyage.
 *
 * ── Ce que ce parcours n'affirme PAS ────────────────────────────────────────────────────────
 * Il n'affirme pas que la ligne est marquée « saisie manuellement » et protégée du prochain
 * recalcul : `addSuggestionLine` ne pose pas `quantiteModifieeManuel`, le drapeau reste à faux.
 * Verrouiller ou non une ligne ajoutée à la main est une règle de gestion — à trancher avant
 * d'en faire une assertion.
 *
 * Parcours ÉCRIVANT dans la base : il retire la ligne qu'il a ajoutée.
 */
scenario('ACH-12', async ({ etape, page }) => {
  const produit = 'FORMICA RUFA COMPOSE BOIRON';
  const panneau = page.locator('app-suggestion-produit-panel');
  const filtre = panneau.getByPlaceholder('Rechercher (libellé, CIP)...');
  const ligne = page.locator('.ag-row').filter({ hasText: produit });
  const QUANTITE = 6;

  /** Le nombre de lignes annoncé par le panneau. */
  const compte = async (): Promise<number> => {
    const texte = (await panneau.innerText()).replace(/\s+/g, ' ');
    const trouve = texte.match(/(\d+)\s*ligne\(s\)/);
    return trouve ? Number(trouve[1]) : -1;
  };

  /** Ce que la base dit de ce produit dans cette proposition. */
  const enBase = (suggestionId: string): string =>
    lire(`SELECT COALESCE(SUM(sl.quantity), 0)
            FROM suggestion_line sl
            JOIN fournisseur_produit fp ON fp.id = sl.fournisseur_produit_id
            JOIN produit p ON p.id = fp.produit_id
           WHERE sl.suggestion_id = ${suggestionId} AND p.libelle LIKE '${produit}%'`);

  let lignesAvant = 0;
  let suggestionId = '';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('tab', { name: /Propositions d'achat/ }).click();
    const liste = page.locator('app-suggestion-fournisseur-list');
    await expect(liste.locator('tbody tr').first()).toBeVisible();
    await liste.locator('tbody tr').first().click();
    await expect(panneau).toBeVisible();
    // Le compteur du panneau est le témoin de l'ajout : on le relève avant d'y toucher.
    await expect.poll(compte, { timeout: 20_000 }).toBeGreaterThan(0);
    lignesAvant = await compte();
  });

  await etape(2, async () => {
    await chercherDansSelect(page, 'produitbox', produit, produit);
    await panneau.locator('input[placeholder="Qté"]').fill(String(QUANTITE));
    // L'identifiant de la proposition est pris À LA SOURCE, dans l'URL de l'appel d'ajout :
    // le deviner en base supposerait de savoir quelle ligne de la liste a été ouverte, ce qui
    // dépend de l'ordre d'affichage — exactement le genre de pari qui périme un parcours.
    const appel = page.waitForRequest(r => /\/api\/suggestions\/add-item\/\d+$/.test(r.url()), { timeout: 20_000 });
    await panneau.locator('input[placeholder="Qté"]').press('Enter');
    suggestionId = new URL((await appel).url()).pathname.split('/').pop() ?? '';
    expect(suggestionId, "l'appel d'ajout doit nommer la proposition").toMatch(/^\d+$/);

    // Le filtre amène la ligne sous les yeux — sans lui, elle existe sans être rendue.
    await expect.poll(compte, { timeout: 20_000 }).toBe(lignesAvant + 1);
    await filtre.fill(produit);
    await expect(ligne.first()).toBeVisible();
    await expect(ligne.first()).toContainText(String(QUANTITE));
    // Et la quantité demandée est bien celle qui est partie en base.
    expect(enBase(suggestionId), 'la quantité saisie doit être enregistrée').toBe(String(QUANTITE));
  });

  // ── Remise en état : la ligne ajoutée est retirée de la proposition. ────────────────────
  await filtre.fill(produit);
  if (await ligne.first().isVisible().catch(() => false)) {
    await ligne.first().locator('.ag-selection-checkbox, input[type="checkbox"]').first().click();
    const supprimer = panneau.getByRole('button', { name: 'Supprimer' });
    if (await supprimer.isEnabled().catch(() => false)) {
      await supprimer.click();
      const confirmation = page.locator('.modal-content');
      if (await confirmation.isVisible().catch(() => false)) {
        await confirmation.getByRole('button', { name: /Oui|Confirmer/ }).click();
      }
    }
  }
  // La base est rendue telle qu'on l'a trouvée : sans ce contrôle, le résidu passe inaperçu
  // et c'est le parcours suivant qui trébuche.
  await expect
    .poll(() => enBase(suggestionId), { timeout: 20_000 })
    .toBe('0');
});
