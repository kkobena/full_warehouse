import { expect } from '@playwright/test';
import { ouvrirOnglet, rechercher } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un organisme rejette trois lignes d'une facture déjà émise. On ne réécrit pas la facture —
 * elle est partie, elle est numérotée — on lui adosse un avoir, qui dit ce qu'on rend et
 * pourquoi.
 *
 * Le brouillon existe précisément pour ce moment-là : le rejet est connu, son montant exact
 * ne l'est pas encore. L'avoir se prépare, se relit, se corrige, et ne pèse sur aucun solde
 * tant qu'il n'est pas émis puis imputé.
 *
 * La création se lance ici depuis la barre de l'écran. C'était impossible jusqu'à peu : le
 * seul point d'entrée était le « + » d'une ligne existante, si bien qu'une liste vide — le
 * cas du tout premier avoir — ne menait nulle part.
 */
scenario('FAC-19', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const modal = page.locator('.modal-content:visible');

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Avoirs/);
    await rechercher(page);
    await expect(contenu).toContainText('Brouillons');
    await expect(contenu).toContainText('N° Avoir');
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Nouvel avoir' }).click();
    await expect(modal).toBeVisible();

    // La facture d'origine se cherche au serveur : le composant ne détient aucune option
    // tant qu'on n'a pas frappé. Seules les factures déjà réglées, au moins en partie, sont
    // proposées — on ne rend pas ce qui n'a pas été versé.
    // Le numéro suit le format « ANNEE_0001 », mais la liste n'affiche que ce qui suit le
    // souligné : chercher « 20 » — l'année — ne ramenait rien, et le parcours cliquait alors
    // la ligne « Aucun résultat », qui ne sélectionne évidemment aucune facture.
    const champFacture = modal.locator('ng-select input[type="text"]').first();
    await champFacture.fill('00');
    // Le panneau d'options est rendu SUR LE CORPS de la page (`appendTo="body"`), donc en
    // dehors de la modal : le chercher dans `.modal-content` ne trouve jamais rien.
    const option = page.locator('.ng-option').first();
    await expect(option).toBeVisible({ timeout: 15000 });
    await expect(option, 'Aucune facture réglée à créditer.').not.toContainText('Aucun résultat');
    await option.click();

    // Le montant de l'avoir ne peut EXCÉDER ce que l'organisme a déjà réglé : on ne rembourse
    // pas ce qu'on n'a pas reçu. Mille francs restent sous le règlement de n'importe quelle
    // facture de la démonstration, réglée ou partiellement réglée.
    //
    // `app-input-number` pose l'identifiant SUR son `<input>` — pas sur l'hôte — et
    // concatène si l'on ne vide pas : d'où `fill` plutôt que `type`.
    await modal.locator('input#av-montant').fill('1000');
    await modal.locator('textarea').first().fill('Rejet partiel de l\'organisme : trois lignes non prises en charge');

    await expect(modal.getByRole('button', { name: "Créer l'avoir" })).toBeEnabled();
  });
});
