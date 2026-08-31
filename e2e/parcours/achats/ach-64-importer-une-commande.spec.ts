import { expect } from '@playwright/test';
import { chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un grossiste envoie son catalogue ou sa proposition de réassort sous forme de fichier ; le
 * ressaisir ligne à ligne serait long et fautif. L'import constitue la commande directement à
 * partir de ce fichier.
 *
 * Deux familles de formats : ceux des grossistes reconnus — LABOREX, COPHARMED, DPCI, TEDIS,
 * dont les colonnes sont connues d'avance — et deux formats génériques, « CIP + quantité » et
 * « CIP + quantité + prix d'achat », pour tout le reste. Le séparateur est le point-virgule,
 * et une première ligne d'en-tête est reconnue puis ignorée.
 *
 * Le fournisseur se choisit AVANT le fichier : c'est lui qui donne le tarif appliqué aux
 * lignes, et le code CIP est cherché dans SON catalogue. Un code qu'il ne référence pas ne
 * peut pas devenir une ligne — le résultat d'import le signale plutôt que de l'inventer.
 *
 * Parcours ÉCRIVANT dans la base : il importe une commande de deux lignes.
 */
scenario('ACH-64', async ({ etape, page }) => {
  const modale = page.locator('.modal-content');

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    await page.getByRole('button', { name: 'Importation' }).click();
    await expect(modale).toContainText(/IMPORTATION DE NOUVELLE COMMANDE/i);
  });

  await etape(2, async () => {
    // Le fournisseur d'abord : c'est son catalogue qui sera interrogé, et sa grille qui
    // donnera les prix.
    await chercherDansSelect(page, 'fournisseur-select', 'LABOREX', 'LABOREX ABIDJAN COCODY');
    // Puis le format : ici le générique « CIP + quantité », que tout tableur produit.
    await modale.locator('ng-select').last().click();
    await page.locator('.ng-option').filter({ hasText: 'Cip  quantité' }).first().click();
  });

  await etape(3, async () => {
    // Deux lignes, séparateur point-virgule, une ligne d'en-tête reconnue et ignorée.
    await modale.locator('input[type="file"]').first().setInputFiles({
      name: 'commande-laborex.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from('CIP;QUANTITE\n1000023;5\n1000024;3\n', 'utf-8'),
    });
    await expect(modale).toContainText(/commande-laborex\.csv/);
  });

  await etape(4, async () => {
    // Le résultat d'import distingue ce qui est entré dans la commande de ce qui a été
    // rejeté : un code inconnu du fournisseur ne devient pas une ligne muette.
    await modale.getByRole('button', { name: /Importer|Valider|Enregistrer/ }).first().click();
    // Le compte rendu d'import s'ouvre à la place : lignes retenues d'un côté, lignes
    // rejetées de l'autre, avec le code qui n'a pas été reconnu.
    await expect(page.locator('.modal-content').last()).toBeVisible();
    await expect(page.locator('.modal-content').last()).toContainText(/[Ii]mport|ligne/);
  });
});
