import { expect } from '@playwright/test';
import { chercherAuCatalogue, chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La classe ABC est recalculée périodiquement à partir des ventes : c'est ce qu'on veut pour
 * l'essentiel du catalogue, et ce qu'on ne veut surtout pas pour certains produits. Une
 * insuline se garde en A+ même si elle se vend trois fois l'an — la rupture y est un risque
 * vital, pas un manque à gagner.
 *
 * D'où le VERROU : on choisit la classe à la main, et on empêche le calcul automatique de la
 * reprendre. Sans ce verrou, le réglage tiendrait jusqu'au prochain passage du calcul.
 *
 * Le modèle annonçait un motif à saisir : le formulaire n'en demande pas. Corrigé dans
 * cahier-recette.model.ts en écrivant ce parcours.
 *
 * Parcours ÉCRIVANT dans la base : il rétablit la classe et le verrou d'origine.
 */
scenario('REF-14', async ({ etape, page }) => {
  const produit = 'POMMADE ARNICA TM 4% BOIRON';
  const select = page.locator('ng-select', { has: page.locator('#f_criticite') });
  const verrou = () => page.getByRole('switch').filter({ visible: true }).last();
  let classeOrigine = '';

  const ouvrirLaFiche = async (): Promise<void> => {
    await page.goto('/produits');
    await chercherAuCatalogue(page, produit);
    await page.locator('app-produit-list tbody tr').first()
      .getByRole('button', { name: 'Actions' }).click();
    await page.getByRole('button', { name: 'Éditer' }).click();
    await expect(page.getByRole('heading', { name: /Modifier Produit/i })).toBeVisible();
    // Le formulaire est découpé en onglets, et la classe de criticité ne vit pas dans celui
    // qui s'appelle « Classification » — lequel porte le laboratoire, la forme et la DCI —
    // mais dans « Approvisionnement », avec les seuils qu'elle commande.
    await page.getByRole('tab', { name: 'Approvisionnement' }).click();
  };

  /**
   * Enregistre, et n'en rend la main qu'une fois l'écriture ACQUITTÉE par le serveur.
   *
   * Sans cette attente, le parcours enchaînait sur `page.goto` dès que le titre du catalogue
   * reparaissait — et la navigation ANNULE la requête encore en vol. Une fois sur deux la
   * classe forcée restait en base, le parcours passant au vert malgré tout : le catalogue
   * garde en mémoire la valeur qu'il vient d'afficher, de sorte que relire le formulaire
   * montre ce qu'on espérait, non ce qui est écrit. C'est la réponse du serveur qui fait foi.
   */
  const enregistrer = async (): Promise<void> => {
    const acquittement = page.waitForResponse(
      r => r.request().method() === 'PUT' && /\/api\/produits$/.test(r.url()) && r.status() < 400,
      { timeout: 20_000 },
    );
    await page.getByRole('button', { name: 'Enregistrer' }).click();
    await acquittement;
    await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
  };

  await etape(1, async () => {
    await ouvrirLaFiche();
    await expect(page.locator('#main-content')).toContainText('Classe de criticité produit');
    classeOrigine = (await select.innerText()).trim();
  });

  await etape(2, async () => {
    // La liste ne donne pas seulement des lettres : chaque classe est décrite par ce qu'elle
    // recouvre, ce qui évite de classer au jugé.
    await chercherDansSelect(page, 'f_criticite', 'A+', 'Produits vitaux');
    // Le verrou est l'autre moitié du geste : sans lui, le prochain calcul reprendrait la main.
    await expect(page.locator('#main-content')).toContainText('Verrouiller cette classe');
    await enregistrer();
  });

  // ── Remise en état : la classe d'origine est rendue au produit. ─────────────────────────
  await ouvrirLaFiche();
  // La lettre seule ne désigne pas une option : `hasText` est insensible à la casse, et « D »
  // se retrouve dans le « d » de « Produits vitaux ». Les deux options restaient donc
  // candidates, le helper cliquait la première — A+ — et la restauration réélisait la valeur
  // qu'elle devait défaire : le produit restait forcé en base, sans que rien ne le signale.
  // Pire, la classe d'origine lue au tour suivant étant alors « A+ », la restauration était
  // sautée et le parcours repassait au vert sur une base sale.
  // D'où la convention d'ACH-06 : la lettre ne sert qu'à filtrer la liste, c'est le libellé
  // ENTIER qui désigne l'option.
  const libelleOrigine = classeOrigine.replace(/\s+/g, ' ').replace(/^×\s*/, '').trim();
  const lettreOrigine = libelleOrigine.split('—')[0].trim();
  if (lettreOrigine && libelleOrigine.includes('—') && !lettreOrigine.startsWith('A+')) {
    await chercherDansSelect(page, 'f_criticite', lettreOrigine, libelleOrigine);
    await enregistrer();
  }
});
