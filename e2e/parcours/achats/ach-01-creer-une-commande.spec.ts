import { expect } from '@playwright/test';
import { ajouterLigneCommande, chercherDansSelect, ouvrirOnglet } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La commande fournisseur se construit ligne à ligne, et deux choix la précèdent : le
 * GROSSISTE — dont dépendent les codes produits, les prix d'achat et les délais — puis les
 * produits eux-mêmes. Tant que le bon n'est pas créé, rien n'est engagé : c'est un brouillon
 * qu'on complète au fil de la journée.
 *
 * Le fournisseur choisi n'est pas décoratif : c'est lui qui donne à chaque ligne son code CIP
 * fournisseur et son prix d'achat, ceux-là mêmes qui serviront au contrôle de concordance à
 * la réception (ACH-74) et à la détection des écarts de prix (ACH-43).
 *
 * Parcours ÉCRIVANT dans la base : il crée un bon de commande, que la restauration de
 * l'instantané efface avant la campagne suivante.
 */
scenario('ACH-01', async ({ etape, page }) => {
  // L'AGENCE qui livrera, nommée en entier : le sélecteur est groupé, et le grossiste
  // principal n'y est qu'un en-tête de groupe — le viser reviendrait à ne rien choisir.
  const fournisseur = 'LABOREX ABIDJAN COCODY';
  const produit = 'DOLIPRANE 500MG';

  await etape(1, async () => {
    await page.goto('/commande');
    await ouvrirOnglet(page, /Commandes & Réceptions/);
    await page.getByRole('button', { name: 'Commandes fournisseurs' }).click();
    await page.getByRole('button', { name: 'Nouvelle commande' }).click();
    await expect(page.locator('app-commande-requested')).toBeVisible();
  });

  await etape(2, async () => {
    // Le grossiste d'abord : sans lui, la recherche de produit n'a pas de tarif à appliquer.
    await chercherDansSelect(page, 'fournisseur-select', 'LABOREX', fournisseur);
    await ajouterLigneCommande(page, produit, '10');
    // La ligne rejoint la grille, avec le code fournisseur, le prix d'achat et le prix de
    // vente : c'est ce triplet qui rend la commande vérifiable à la réception.
    const grille = page.locator('app-commande-requested');
    await expect(grille).toContainText(produit);
    await expect(grille).toContainText('1 ligne(s)');
  });

  await etape(3, async () => {
    // Il n'y a pas de bouton « Valider » : la commande EXISTE dès sa première ligne, en
    // attente, et se complète tant qu'elle n'est pas transmise. Ce que le modèle appelle
    // valider, c'est la retrouver dans la liste des commandes fournisseurs — d'où l'on
    // pourra l'envoyer (ACH-28) ou en créer le bon de livraison (ACH-36).
    await page.getByRole('button', { name: 'Retour à la liste' }).click();
    const liste = page.locator('app-commande-requested-home');
    await expect(liste).toBeVisible();
    await expect(liste.locator('tbody tr').first()).toContainText(fournisseur);
  });
});
