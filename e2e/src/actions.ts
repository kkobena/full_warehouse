/**
 * Gestes récurrents des parcours, factorisés ici plutôt que recopiés.
 *
 * Chacun encapsule une particularité d'un composant du Design System qui, sans cela, se
 * paierait dans chaque parcours — et se redécouvrirait à chaque fois par un échec.
 */
import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Saisit une date dans un `pharma-date-picker`, au format attendu par l'application.
 *
 * Le composant posait le MÊME `id` sur son hôte et sur son `<input>` interne — deux éléments
 * pour un identifiant, invalide en HTML, et Playwright refusait d'agir (« strict mode
 * violation »). Corrigé dans `pharma-date-picker`, qui retire l'attribut de son hôte. Le
 * sélecteur reste précisé à la balise : c'est le champ de saisie qu'on veut, sans ambiguïté.
 */
export async function saisirDate(page: Page, inputId: string, date: Date): Promise<void> {
  const jj = String(date.getDate()).padStart(2, '0');
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  await page.locator(`input#${inputId}`).fill(`${jj}/${mm}/${date.getFullYear()}`);
}

/**
 * Ouvre un onglet d'un écran à navigation verticale (facturation, comptabilité, différés…).
 *
 * ng-bootstrap donne bien `role="tab"` aux liens, mais leur texte se termine par un chevron
 * « › » et porte parfois un compteur (« Factures 37 »). D'où l'expression régulière plutôt
 * qu'une correspondance exacte, qui échouerait sur les deux.
 */
export async function ouvrirOnglet(page: Page, libelle: RegExp | string): Promise<void> {
  await page.getByRole('tab', { name: libelle }).first().click();
}

/**
 * Choisit une option dans un `app-select` (ng-select) par son LIBELLÉ visible.
 *
 * L'ouverture se fait au CLAVIER, pour deux raisons :
 *
 *  1. `inputId` pose bien l'identifiant sur un `<input>`, mais ng-select le rend
 *     **invisible** : cliquer dessus échoue sur « element is not visible » ;
 *  2. c'est le chemin d'un utilisateur qui navigue sans souris, donc celui qu'il vaut mieux
 *     éprouver.
 *
 * Le clic fonctionne aussi désormais : il ne le faisait pas quand une valeur était déjà
 * sélectionnée — ng-select refuse d'ouvrir si `searchable` est faux et que le clic tombe sur
 * la valeur, pour laisser copier le libellé. Corrigé dans `app-select`.
 */
/**
 * Choisir dans un `app-select-search` — la variante qui interroge le SERVEUR.
 *
 * Différence avec `choisirDansSelect`, et elle est décisive : ce composant ne détient aucune
 * option tant qu'on n'a pas tapé. Ouvrir la liste à la flèche du bas n'affiche donc rien, et
 * l'attente d'une option expire sur un composant pourtant sain. Il faut FRAPPER le terme, au
 * moins aussi long que `minSearchLength`, puis attendre la réponse.
 */
export async function chercherDansSelect(page: Page, inputId: string, terme: string, libelle = terme): Promise<void> {
  const select = page.locator('ng-select', { has: page.locator(`#${inputId}`) });
  const champ = page.locator(`#${inputId}`);

  await expect(select).toBeVisible();
  await champ.click();

  // `fill` plutôt que la frappe touche à touche : elle pose la valeur et émet l'évènement
  // d'entrée en une fois, sans dépendre du focus. Le clic qui ouvre la liste ne le donne pas
  // toujours au champ de recherche, et exiger le focus faisait échouer un parcours sur dix.
  await champ.fill(terme);
  const option = page.locator('.ng-option', { hasText: libelle }).first();
  const vue = await option
    .waitFor({ state: 'visible', timeout: 4000 })
    .then(() => true)
    .catch(() => false);
  if (!vue) {
    // Repli : certains champs n'écoutent que la frappe réelle (recherche serveur déclenchée
    // au `keyup`). On retape alors touche par touche.
    await champ.fill('');
    await champ.pressSequentially(terme, { delay: 60 });
  }
  await expect(option).toBeVisible();
  await option.click();
  await expect(select).toContainText(libelle);
}

export async function choisirDansSelect(page: Page, inputId: string, libelle: string): Promise<void> {
  const select = page.locator('ng-select', { has: page.locator(`#${inputId}`) });
  const champ = page.locator(`#${inputId}`);

  await expect(select).toBeVisible();
  await champ.focus();
  await champ.press('ArrowDown');

  // Les options ne portent pas `role="option"` : on les cible par leur classe.
  await page.locator('.ng-option', { hasText: libelle }).first().click();

  // La valeur retenue s'affiche dans le select : on attend qu'elle y soit avant de rendre
  // la main, sinon une recherche lancée dans la foulée partirait sur l'ancien filtre.
  await expect(select).toContainText(libelle);
}

/**
 * Clique le bouton « Rechercher » d'une barre d'outils.
 *
 * A nécessité un temps un ancrage de repli : dans une barre étroite, le libellé visible est
 * réduit à une largeur nulle et sortait du nom accessible, rendant le bouton introuvable par
 * `getByRole` — et anonyme pour un lecteur d'écran. Le défaut a été corrigé dans
 * `app-button`, qui pose désormais systématiquement l'`aria-label` ; le repli n'a plus lieu
 * d'être.
 */
export async function rechercher(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Rechercher' }).click();
}

/**
 * Coche une option dans un `app-multi-select` (ng-select en mode multiple), par son LIBELLÉ.
 *
 * Deux différences avec `choisirDansSelect`, qui justifient une fonction distincte :
 *
 *  1. la liste **reste ouverte** après un clic — c'est le propre d'un choix multiple. Sans le
 *     `Escape`, la capture montrerait le menu déroulant déplié par-dessus le tableau qu'elle
 *     est censée illustrer ;
 *  2. la valeur retenue s'affiche en pastille dans le champ, pas en texte simple — l'attente
 *     porte donc sur le contenu du select une fois refermé.
 */
export async function cocherDansMultiSelect(page: Page, inputId: string, libelle: string): Promise<void> {
  const select = page.locator('ng-select', { has: page.locator(`#${inputId}`) });
  const champ = page.locator(`#${inputId}`);

  await expect(select).toBeVisible();
  await champ.focus();
  await champ.press('ArrowDown');
  await page.locator('.ng-option', { hasText: libelle }).first().click();
  await champ.press('Escape');

  await expect(select).toContainText(libelle);
}

/**
 * Renvoie la CARTE (`.card`) dont le titre est `titre`, et elle seule.
 *
 * `page.locator('.card').filter({ hasText: titre })` ne convient pas : les écrans de
 * tableau de bord imbriquent leurs cartes, et le filtre retourne alors aussi les cartes
 * ENGLOBANTES — dont celle qui contient toute la page. Une assertion ainsi mal cadrée passe
 * au vert en trouvant son texte n'importe où ailleurs dans l'écran ; c'est le même piège que
 * l'onglet masqué, en plus silencieux.
 *
 * On part donc du titre et on remonte à la première carte qui l'englobe.
 */
export function carte(page: Page, titre: RegExp | string): Locator {
  return page
    .getByRole('heading', { name: titre })
    .locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " card ")][1]');
}

/**
 * Garantit que la caisse de l'utilisateur est OUVERTE, en l'ouvrant si besoin.
 *
 * Les parcours doivent rester indépendants de leur ordre d'exécution, or l'écran de vente
 * refuse d'encaisser sans caisse ouverte, tandis que d'autres parcours la referment. Chacun
 * pose donc l'état dont il a besoin, hors de ses étapes — la mise en scène ne se photographie
 * pas.
 */
export async function assurerCaisseOuverte(page: Page, fonds = '50000'): Promise<void> {
  await page.goto('/my-cash-register');
  const formulaire = page.getByRole('button', { name: 'Ouvrir la caisse' });
  const enCours = page.getByText(/Ma caisse en cours/i);
  await expect(formulaire.or(enCours).first()).toBeVisible();
  if (await enCours.isVisible()) {
    return;
  }
  const montant = page.locator('#cashFundAmount');
  // Le champ prend le focus ~100 ms après l'affichage, en y écrivant le fonds proposé :
  // saisir avant, c'est se faire réécrire — voir `saisirQuantite` pour le même piège.
  //
  // On n'ATTEND plus ce focus : l'écran ne le donne pas toujours (il ne le fait pas quand la
  // page arrive déjà chargée), et l'attente échouait alors sur un formulaire parfaitement
  // utilisable. On écrit, puis on VÉRIFIE la valeur — ce qui couvre aussi le cas où
  // l'application la réécrit après coup.
  await expect(montant).toBeVisible();
  await montant.fill(fonds);
  if ((await montant.inputValue()) !== fonds) {
    await montant.fill(fonds);
  }
  await expect(montant).toHaveValue(fonds);
  await expect(formulaire).toBeEnabled();
  await formulaire.click();
  await expect(enCours).toBeVisible();
}

/**
 * Garantit que la caisse de l'utilisateur est FERMÉE, en la clôturant si besoin.
 *
 * La clôture passe obligatoirement par le billetage : c'est le comptage qui ferme la caisse,
 * il n'existe pas de fermeture sèche.
 */
export async function assurerCaisseFermee(page: Page): Promise<void> {
  await page.goto('/my-cash-register');
  const ouverture = page.getByRole('button', { name: 'Ouvrir la caisse' });
  const enCours = page.getByText(/Ma caisse en cours/i);
  await expect(ouverture.or(enCours).first()).toBeVisible();
  if (await ouverture.isVisible()) {
    return;
  }
  await page.getByRole('button', { name: /Fermer la caisse/i }).click();
  await page.locator('#numberOf10Thousand').fill('5');
  await page.getByRole('button', { name: /Valider le billetage/i }).click();
  const confirmation = page.locator('.modal-content');
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Oui' }).click();
  await expect(ouverture).toBeVisible();
}

/**
 * Saisit la quantité d'un produit sur l'écran de vente, une fois le produit choisi.
 *
 * L'écran donne le focus à ce champ ET y écrit `1` environ 200 ms APRÈS la sélection du
 * produit. Saisir avant, c'est se faire écraser sans le voir : la ligne part alors avec une
 * quantité de 1, et le total de la capture ne correspond plus à ce que la légende annonce.
 * Attendre le focus, c'est attendre que ce préremplissage ait eu lieu.
 */
export async function saisirQuantite(page: Page, quantite: string): Promise<void> {
  const champ = page.locator('#quantiteSaisie');
  // Deux attentes, et elles ne font pas double emploi : l'écran DONNE LE FOCUS au champ puis
  // y POSE la quantité par défaut. Saisir entre les deux, c'est écrire une valeur que la
  // seconde opération effacera — la ligne part alors avec la quantité par défaut, et le
  // total de la capture ne correspond plus à sa légende.
  await expect(champ).toBeFocused();
  await expect(champ).not.toHaveValue('');
  await champ.fill(quantite);
  await expect(champ).toHaveValue(quantite);
}

/**
 * Saisit la quantité PUIS ajoute la ligne au panier.
 *
 * Le bouton de validation plutôt que la touche `Entrée` : la frappe part parfois avant que le
 * champ ne soit prêt et se perd sans rien dire, alors qu'un bouton attend d'être actif. Le
 * geste illustré reste le même — c'est celui d'un comptoir à la souris.
 */
export async function ajouterAuPanier(page: Page, quantite: string): Promise<void> {
  const valider = page.getByRole('button', { name: 'Ajouter au panier' });
  await saisirQuantite(page, quantite);
  await expect(valider).toBeEnabled();
  await valider.click();
}

/**
 * Garantit que l'écran de vente s'ouvre sur un panier VIDE.
 *
 * La vente en cours vit côté serveur : dès la première ligne, elle existe et l'écran la
 * rouvre au chargement suivant. Un parcours qui laisserait la sienne — parce qu'il a échoué,
 * ou parce qu'il l'a mise en attente — ferait donc démarrer le suivant avec le panier d'un
 * autre, et son assertion porterait sur la mauvaise ligne. Chacun repart d'un panier vide.
 */
export async function assurerPanierVide(page: Page): Promise<void> {
  await page.goto('/sales-home');
  // Attendre que l'écran soit prêt AVANT de conclure quoi que ce soit : la vente en cours
  // est rechargée depuis le serveur, et un panier peut donc apparaître après coup. Conclure
  // « panier vide » sur le premier rendu laisse passer la vente d'un autre parcours.
  await expect(page.locator('#produitbox')).toBeVisible();

  // Boucle, et non un simple `if` : les ventes en cours s'empilent — en abandonner une fait
  // remonter la suivante. Cinq tours suffisent largement ; au-delà, c'est que quelque chose
  // d'autre ne va pas, et il vaut mieux échouer que tourner.
  const annuler = page.getByRole('button', { name: 'Annuler' });
  for (let essai = 0; essai < 5; essai++) {
    const panierRepris = await annuler
      .waitFor({ state: 'visible', timeout: 3000 })
      .then(() => true)
      .catch(() => false);
    if (!panierRepris) {
      break;
    }
    await annuler.click();
    const confirmation = page.locator('.modal-content');
    await expect(confirmation).toBeVisible();
    await confirmation.getByRole('button', { name: 'Oui' }).click();
    await expect(confirmation).toBeHidden();
  }

  await abandonnerVentesEnAttente(page);
  await expect(page.getByText(/Panier vide|Ajoutez des produits|Sélectionnez un client assuré/i).first()).toBeVisible();
}

/**
 * Met la vente en cours DE CÔTÉ (bouton « En attente » de la zone d'encaissement).
 *
 * Deux boutons portent ce nom à l'écran : celui-ci, qui gare la vente, et celui de la barre
 * d'outils, qui ouvre la liste des ventes garées. Les distinguer par leur position est un
 * piège — le second n'affiche son compteur que s'il y a quelque chose à compter, et son nom
 * change donc avec l'état de la journée. On vise donc la zone d'actions de la vente.
 */
export async function mettreEnAttente(page: Page): Promise<void> {
  await page.locator('app-sale-actions').getByRole('button', { name: 'En attente' }).click();
}

/** Ouvre le panneau des ventes garées (bouton de la barre d'outils). */
export async function ouvrirVentesEnAttente(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Voir les ventes en attente' }).click();
}

/**
 * Abandonne les ventes laissées EN ATTENTE par les parcours précédents.
 *
 * Vider le panier courant ne suffit pas : une vente mise en attente est garée côté serveur,
 * et l'écran de vente rouvre sur la liste de ces ventes garées. Le parcours suivant ouvre
 * alors un comptoir qui n'est pas neutre — panneau ouvert, parfois panier repris — et ses
 * captures montrent l'ardoise d'un autre. Elles s'empilent d'un parcours à l'autre : VTE-14
 * en gare une par construction, c'est ce qu'elle illustre.
 *
 * On les reprend donc une à une pour les annuler, ce que ferait le caissier qui trouve le
 * comptoir encombré au matin. Chaque tour repart d'un écran de vente neuf : reprendre une
 * vente laisse le panneau dans un état dont on ne veut rien présumer.
 */
export async function abandonnerVentesEnAttente(page: Page): Promise<void> {
  const liste = page.locator('app-pending-sales-list');

  for (let essai = 0; essai < 8; essai++) {
    await page.goto('/sales-home');
    await expect(page.locator('#produitbox')).toBeVisible();

    const bouton = page.getByRole('button', { name: 'Voir les ventes en attente' });
    if (!(await bouton.isVisible().catch(() => false))) {
      return;
    }
    await bouton.click();
    const ouverte = await liste
      .waitFor({ state: 'visible', timeout: 3000 })
      .then(() => true)
      .catch(() => false);
    if (!ouverte) {
      return;
    }

    // Viser le BOUTON plutôt que la ligne : le tableau imbrique le détail dépliable d'une
    // vente dans une ligne à lui, et la « première ligne » n'est donc pas toujours celle qui
    // porte les actions.
    const reprendre = liste.getByRole('button', { name: 'Reprendre la vente' }).first();
    const garee = await reprendre
      .waitFor({ state: 'visible', timeout: 3000 })
      .then(() => true)
      .catch(() => false);
    if (!garee) {
      // Plus rien à reprendre : on repart d'un écran neuf, le panneau ne survit pas au
      // rechargement.
      await page.goto('/sales-home');
      return;
    }

    await reprendre.click();
    const annuler = page.getByRole('button', { name: 'Annuler' });
    await expect(annuler).toBeVisible();
    await annuler.click();

    // Annuler une vente est un geste SOUMIS À PRIVILÈGE : joué sous un compte de caissier —
    // ce que font VTE-46 et VTE-47 — il ouvre une demande d'autorisation superviseur au lieu
    // de la confirmation habituelle. On renonce alors au ménage plutôt que de le forcer : ce
    // qui a été garé sous un autre compte se nettoiera sous ce compte-là.
    const confirmation = page.locator('.modal-content');
    const confirme = await confirmation
      .waitFor({ state: 'visible', timeout: 3000 })
      .then(() => true)
      .catch(() => false);
    const oui = confirmation.getByRole('button', { name: 'Oui' });
    if (!confirme || (await oui.count()) === 0) {
      await page.goto('/sales-home');
      return;
    }
    await oui.click();
    await expect(confirmation).toBeHidden();
  }
}


/**
 * Crée un produit jetable par le FORMULAIRE de l'application, et rend son libellé.
 *
 * Les parcours qui suppriment ou modifient un produit ont besoin d'une fiche dont personne
 * d'autre ne dépend : toucher à un produit du catalogue de démonstration déplacerait les
 * montants d'autres parcours, et supprimer un produit qui a des mouvements est de toute façon
 * refusé. On passe par l'écran plutôt que par la base : ce que le parcours crée, un
 * utilisateur pourrait le créer.
 *
 * Les huit champs renseignés sont exactement ceux que le formulaire exige (REF-01).
 */
export async function creerProduitJetable(page: Page, libelle: string, codeCip: string): Promise<void> {
  await page.goto('/produits/new');
  await expect(page.locator('#f_libelle')).toBeVisible();
  await page.locator('#f_codeCip').fill(codeCip);
  await chercherDansSelect(page, 'f_fournisseur', 'LABOREX', 'LABOREX');
  await page.locator('#f_libelle').fill(libelle);
  await page.locator('#f_costAmount').fill('1500');
  await page.locator('#f_regularUnitPrice').fill('2000');
  await choisirDansSelect(page, 'f_tva', '18');
  await chercherDansSelect(page, 'f_famille', 'HOMEO', 'HOMEOPATHIE');
  await chercherDansSelect(page, 'f_rayon', 'HOMEO', 'HOMEOPATHIE');
  await page.getByRole('button', { name: 'Enregistrer' }).click();
  await expect(page.getByRole('heading', { name: 'Catalogue produits' })).toBeVisible();
}

/**
 * Filtre le catalogue produits sur un terme, et ATTEND que la liste ait suivi.
 *
 * La recherche part sur `Entrée`, mais la liste charge aussi d'elle-même à l'ouverture de
 * l'écran : une recherche lancée pendant ce premier chargement est écrasée par lui, et le
 * parcours travaille alors sur le catalogue entier en croyant l'avoir filtré — c'est ainsi
 * qu'on finit par éditer le premier produit venu. On relance donc jusqu'à ce que la première
 * ligne réponde au terme cherché.
 *
 * `attendu` sert quand on cherche par CODE : la ligne trouvée porte alors un libellé, pas le
 * code tapé.
 */
export async function chercherAuCatalogue(page: Page, terme: string, attendu = terme): Promise<void> {
  const champ = page.getByPlaceholder(/Rechercher \(CIP/);
  const premiere = page.locator('tbody tr').filter({ visible: true }).first();

  for (let essai = 0; essai < 4; essai++) {
    await champ.fill('');
    await champ.fill(terme);
    await champ.press('Enter');
    const trouve = await premiere
      .filter({ hasText: attendu })
      .waitFor({ state: 'visible', timeout: 3000 })
      .then(() => true)
      .catch(() => false);
    if (!trouve) {
      continue;
    }
    // Vérifier que le résultat TIENT : le chargement initial de la liste, parti avant la
    // recherche, revient parfois après elle et réaffiche le catalogue entier. Un parcours qui
    // enchaîne aussitôt travaille alors sur le premier produit venu, en croyant tenir le sien.
    await page.waitForTimeout(400);
    if (await premiere.filter({ hasText: attendu }).isVisible().catch(() => false)) {
      return;
    }
  }
  await expect(premiere).toContainText(attendu);
}

/**
 * Saisit un MONTANT dans un `app-input-number`, en remplaçant ce qu'il contenait.
 *
 * Le composant reformate à chaque frappe — espaces des milliers compris — et `fill` seul ne
 * remplace pas toujours la valeur : elle s'ajoute à celle déjà présente, et un champ
 * pré-rempli par un solde devient un montant à douze chiffres. On vide donc d'abord, on
 * vérifie que c'est vide, puis on frappe.
 */
export async function saisirMontant(page: Page, selecteur: string, montant: string): Promise<void> {
  const champ = page.locator(selecteur);
  await expect(champ).toBeVisible();
  await champ.click();
  await champ.fill('');
  await expect(champ).toHaveValue('');
  await champ.pressSequentially(montant, { delay: 40 });
}

/**
 * Cherche un produit sur l'écran de vente et retient la première suggestion.
 *
 * Deux précautions, chacune payée par un échec :
 *  1. **cliquer avant de saisir.** Le champ est un ng-select ; saisir sans l'avoir ouvert
 *     laisse la liste fermée et aucune suggestion n'apparaît jamais.
 *  2. **attendre la suggestion, pas un délai.** La recherche interroge le serveur ; cliquer
 *     sur `.ng-option` sans l'avoir vue arrive une fois sur trois sur une liste vide.
 */
export async function chercherProduit(page: Page, libelle: string): Promise<void> {
  const champ = page.locator('#produitbox');
  // Le critère est l'OUVERTURE de la liste, pas le focus. L'écran de vente donne lui-même le
  // focus au champ après la saisie d'un numéro de bon, mais sans ouvrir le ng-select : y
  // écrire alors ne déclenche aucune recherche, et la liste affiche « Aucun résultat » —
  // symptôme trompeur, qui fait croire à un catalogue vide. À l'inverse, cliquer une liste
  // déjà ouverte la referme. On regarde donc l'état réel du composant.
  const selecteur = page.locator('ng-select').filter({ has: champ }).first();
  const ouvrir = async () => {
    if (!(await selecteur.evaluate(element => element.classList.contains('ng-select-opened')))) {
      await champ.click();
    }
  };
  await ouvrir();
  await champ.fill(libelle);
  const suggestion = page.locator('.ng-option').first();
  // Une seconde chance : si la frappe n'a rien déclenché, on rouvre et on ressaisit plutôt
  // que d'échouer sur un composant qui n'a simplement pas reçu l'événement.
  try {
    await expect(suggestion).toContainText(libelle, { timeout: 6000 });
  } catch {
    await champ.fill('');
    await ouvrir();
    await champ.fill(libelle);
    await expect(suggestion).toContainText(libelle);
  }
  await suggestion.click();

  // Attendre le PANNEAU DE DÉTAIL du produit (prix, rayon, stock) : c'est son arrivée qui
  // réinitialise le champ quantité. Enchaîner sans l'attendre, c'est écrire une quantité que
  // l'écran effacera un instant plus tard — sans rien signaler.
  await expect(page.locator('#main-content')).toContainText(/Prix\s*:\s*[\d\s]*\d/);
}

/**
 * Ouvrir la session d'un AUTRE utilisateur que celui du `storageState`.
 *
 * Presque tous les parcours jouent avec le compte administrateur, chargé une fois pour toute
 * la campagne. Quelques-uns doivent pourtant montrer ce que voit — ou ce que ne peut pas
 * faire — un utilisateur ordinaire : c'est le cas des autorisations par clé de sécurité, qui
 * ne se déclenchent QUE pour un compte dépourvu du privilège. Les jouer en administrateur ne
 * montrerait rien du tout.
 *
 * Le jeton vit dans `localStorage` : le vider suffit à déconnecter, sans passer par le menu.
 * Le contexte du navigateur est propre à chaque test, le fichier de session partagé n'est
 * donc pas altéré pour les parcours suivants.
 */
export async function seConnecterEnTantQue(page: Page, login: string, motDePasse: string): Promise<void> {
  await page.goto('/login');
  await page.evaluate(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
  });
  await page.goto('/login');
  // Le formulaire est renvoyé une seconde fois si l'on est TOUJOURS sur la page de connexion
  // sans message d'erreur : le serveur de développement recompile parfois entre deux
  // parcours, la première soumission part alors dans le vide et la page se recharge, vide.
  // Un identifiant réellement refusé, lui, affiche son message et échoue tout de suite.
  for (let essai = 0; essai < 2; essai++) {
    await page.locator('[data-cy="username"]').fill(login);
    await page.locator('[data-cy="password"] input').fill(motDePasse);
    await page.locator('[data-cy="submit"]').click();
    await expect(page.locator('[data-cy="loginError"]'), `Connexion refusée pour « ${login} ».`).toBeHidden();
    const entre = await page
      .waitForURL(url => !/\/login/.test(url.toString()), { timeout: 15000 })
      .then(() => true)
      .catch(() => false);
    if (entre) {
      return;
    }
  }
  await expect(page, `Connexion sans effet pour « ${login} ».`).not.toHaveURL(/\/login/);
}

/**
 * Ouvre le JOURNAL DES VENTES sur la journée du jour.
 *
 * Le filtre de dates du journal est porté par un service partagé : un parcours qui remonte
 * dans le passé — pour montrer une vente ancienne — le laisse dans cet état pour tous les
 * suivants. Ceux qui viennent d'enregistrer une vente et la cherchent en tête de liste ne la
 * trouvent alors pas, et échouent sur un journal pourtant correct.
 *
 * On repose donc explicitement la journée courante avant de lire la première ligne.
 */
export async function ouvrirJournalDuJour(page: Page): Promise<void> {
  await page.goto('/sales-home/gestion');
  const aujourdhui = new Date();
  await saisirDate(page, 'fromDate', aujourdhui);
  await saisirDate(page, 'toDate', aujourdhui);
  await page.getByRole('button', { name: 'Rechercher' }).click();
  await expect(page.locator('tbody tr').filter({ visible: true }).first()).toBeVisible();
}

/**
 * Change le nombre de lignes par page d'un tableau.
 *
 * Le sélecteur du pied de tableau n'a pas d'identifiant — et ne peut pas en avoir : plusieurs
 * tableaux cohabitent sur certains écrans, et un même `id` répété rendrait le document
 * invalide. Il porte en revanche un nom accessible, qui suffit à le désigner.
 *
 * Utile aux parcours qui cherchent une ligne dans une liste longue : ouvrir la page en grand
 * vaut mieux que la parcourir page à page, où la ligne visée dépend du jeu de données.
 */
export async function choisirTaillePage(page: Page, taille: string): Promise<void> {
  // Le nom accessible est porté par le `ng-select` LUI-MÊME, pas par un descendant : un
  // filtre `has:` ne le trouve donc pas.
  const select = page.locator('ng-select[aria-label="Nombre de lignes par page"]').first();
  await expect(select).toBeVisible();
  await select.locator('input').first().focus();
  await select.locator('input').first().press('ArrowDown');
  await page.locator('.ng-option', { hasText: taille }).first().click();
  await expect(select).toContainText(taille);
}

/**
 * Saisit le montant remis en espèces, et VÉRIFIE qu'il est bien arrivé.
 *
 * Le champ est formaté au fil de la frappe : `fill()` sur un champ déjà renseigné concatène
 * la nouvelle valeur à l'ancienne, et un clic qui manque sa cible laisse le champ vide. Dans
 * les deux cas la vente part de travers — montant absurde d'un côté, proposition de vente
 * différée de l'autre, puisqu'un montant nul est inférieur au dû. Rien n'échoue franchement :
 * le parcours attend un écran vide qui ne vient jamais.
 */
export async function payerEnEspeces(page: Page, montant: string): Promise<void> {
  const champ = page.locator('#CASH');
  await expect(champ).toBeVisible();
  await champ.click();
  // Vider par `fill('')` plutôt que par `Ctrl+A` puis `Suppr` : le champ se reformate à
  // chaque frappe, ce qui défait la sélection — la nouvelle valeur venait alors s'ajouter à
  // l'ancienne, et l'écran affichait des centaines de millions.
  await champ.fill('');
  await expect(champ).toHaveValue('');
  await champ.pressSequentially(montant, { delay: 40 });
  await expect(champ).toHaveValue(new RegExp(montant.replace(/(.)(?=(\d{3})+$)/g, '$1\s?')));
}

/**
 * Traverse l'enchaînement de confirmations d'une entrée en stock.
 *
 * Finaliser une réception n'est pas un clic mais une SUITE de questions, dont la plupart ne se
 * posent que dans certains cas : écarts de prix au-delà du seuil (ACH-43), lignes jamais
 * saisies, rangement rayon → réserve à prévisualiser (ACH-46, ACH-75), reliquat des articles
 * non servis (ACH-08), impression des étiquettes (ACH-76). Un parcours qui attendrait une
 * boîte précise échouerait chez le voisin, dont le bon n'a ni écart ni manquant.
 *
 * D'où cette traversée par TITRE : on répond à ce qui se présente, dans l'ordre où ça se
 * présente, et on s'arrête quand plus rien ne s'affiche. `refus` nomme les titres auxquels
 * répondre « Non » — c'est ainsi qu'un parcours décline le reliquat quand un autre l'accepte.
 * `sarreterAvant` laisse au contraire une question OUVERTE : le parcours qui vient la
 * montrer la trouve à l'écran au lieu de la voir déjà refermée.
 */
export async function traverserConfirmations(
  page: Page,
  options: { refus?: RegExp[]; limite?: number; sarreterAvant?: RegExp } = {},
): Promise<string[]> {
  const refus = options.refus ?? [];
  const vus: string[] = [];
  // Deux formes de question se succèdent dans une finalisation : les confirmations
  // Oui / Non, et la proposition de rangement rayon → réserve, qui répond « Transférer vers
  // réserve » ou « Ignorer ». Ne reconnaître que la première laissait la traversée s'arrêter
  // devant la seconde, sans rien dire — et le reliquat qui la suit n'arrivait jamais.
  //
  // `:visible` est indispensable : une boîte refermée reste dans le DOM le temps de son
  // animation, et `.first()` la retrouvait au tour suivant — la traversée croyait alors
  // avoir déjà répondu à la question qui s'affichait.
  const boites = page.locator('.modal-content:visible').filter({
    has: page.getByRole('button', { name: /Oui|Ignorer/ }),
  });

  for (let i = 0; i < (options.limite ?? 6); i++) {
    const visible = await boites.first().waitFor({ state: 'visible', timeout: 8000 })
      .then(() => true)
      .catch(() => false);
    if (!visible) {
      break;
    }
    const titre = (await boites.first().innerText()).replace(/\s+/g, ' ').trim();
    if (vus.includes(titre)) {
      // La même question deux fois de suite : le clic n'a pas porté, insister ne servirait
      // qu'à tourner en rond.
      break;
    }
    if (options.sarreterAvant?.test(titre)) {
      // La question attendue est là : on la laisse ouverte pour le parcours, qui a quelque
      // chose à en montrer.
      break;
    }
    vus.push(titre);
    // Le libellé du bouton porte son icône devant : « Oui » y devient " Oui". La
    // correspondance par sous-chaîne (le défaut de `getByRole`) le retrouve, un `exact` non.
    const accepte = !refus.some(r => r.test(titre));
    const rangement = await boites.first().getByRole('button', { name: 'Ignorer' }).count();
    const bouton = rangement
      ? accepte ? /Transférer vers réserve/ : /Ignorer/
      : accepte ? 'Oui' : 'Non';
    await boites.first().getByRole('button', { name: bouton }).click();
    // Ne pas attendre que CETTE boîte disparaisse : la suivante s'ouvre au même endroit et
    // le sélecteur la retrouverait aussitôt. On attend que le TEXTE change — ou que plus
    // aucune boîte ne reste.
    await page
      .waitForFunction(
        (precedent: string) => {
          const boite = Array.from(document.querySelectorAll('.modal-content'))
            .find(el => (el as HTMLElement).innerText.includes('Oui'));
          if (!boite) {
            return true;
          }
          return (boite as HTMLElement).innerText.replace(/\s+/g, ' ').trim() !== precedent;
        },
        titre,
        { timeout: 20000 },
      )
      .catch(() => undefined);
  }
  return vus;
}

/**
 * Ouvre un bon de réception choisi sur son ÉTAT DE SAISIE, et rend sa référence.
 *
 * Les bons ne se valent pas, et la liste ne le dit pas : elle n'affiche que « En attente de
 * saisie » ou « Clôturé ». C'est le taux de service de l'entête, une fois le bon ouvert, qui
 * distingue les trois cas dont les parcours ont besoin :
 *
 *   - `'aucun'`   (0 %)          — rien n'a été compté : le bon sur lequel s'exercent la
 *                                  saisie ligne à ligne et « Tout valider » ;
 *   - `'partiel'` (entre 1 et 99) — compté, et le grossiste n'a pas tout servi : celui qui
 *                                  donne un reliquat à la finalisation ;
 *   - `'complet'` (100 %)         — compté et servi en entier : finalisable sans discussion.
 *
 * Les références sont relevées AVANT d'ouvrir quoi que ce soit : revenir à la liste la
 * recharge, et un rang ne désigne plus le même bon d'un passage à l'autre.
 */
export async function ouvrirBonDeReception(
  page: Page,
  attendu: 'aucun' | 'partiel' | 'complet',
): Promise<string> {
  const liste = page.locator('app-list-bons');
  const ecran = page.locator('app-commande-received');
  await expect(liste.locator('tbody tr').first()).toBeVisible();

  const ouverts = await liste.locator('tbody tr').filter({ hasNotText: 'Clôturé' }).allInnerTexts();
  const references = ouverts.map(t => t.match(/BL\d+/)?.[0]).filter((r): r is string => !!r);
  expect(references.length, 'aucun bon de réception ouvert').toBeGreaterThan(0);

  for (const reference of references) {
    await liste.locator('tbody tr').filter({ hasText: reference }).first().click();
    await expect(ecran).toBeVisible();
    const taux = Number((await ecran.innerText()).match(/Taux service\s*:\s*(\d+)/)?.[1] ?? '-1');
    const convient =
      attendu === 'aucun' ? taux === 0 : attendu === 'complet' ? taux === 100 : taux > 0 && taux < 100;
    if (convient) {
      return reference;
    }
    await page.getByRole('button', { name: 'Retour à la liste' }).click();
    await expect(liste.locator('tbody tr').first()).toBeVisible();
  }
  throw new Error(`Aucun bon de réception « ${attendu} » dans la liste : jeu de démonstration incomplet.`);
}

/**
 * Ajoute une ligne à la commande ouverte : produit, quantité, validation.
 *
 * Le geste tient en trois frappes à l'écran, mais il enchaîne deux composants qui ne
 * s'accordent pas d'eux-mêmes : la sélection du produit déclenche un déplacement du focus
 * vers le champ de quantité, quelques dizaines de millisecondes plus tard. Une quantité
 * saisie avant ce déplacement est effacée par lui, et l'entrée qui suit valide une ligne
 * vide — sans erreur, sans ligne, et sans rien qui l'explique dans le rapport.
 */
export async function ajouterLigneCommande(page: Page, produit: string, quantite: string): Promise<void> {
  await chercherDansSelect(page, 'produitbox', produit, produit);
  const qte = page.locator('input[placeholder="Qté"]');
  await expect(qte).toBeVisible();
  // Laisser le composant reprendre le focus AVANT de saisir : c'est lui qui décide quand.
  await expect(qte).toBeFocused({ timeout: 5000 });
  await qte.fill(quantite);
  await expect(qte).toHaveValue(quantite);
  await qte.press('Enter');
}

/**
 * Ouvre, dans le rapprochement, le détail d'un organisme dont au moins une facture reste à
 * régler — et rend le bouton « Régler » de cette facture.
 *
 * Les organismes ne se valent pas : celui qui vient en tête peut n'avoir que des factures
 * soldées, et le sien change au fil des campagnes comme des parcours qui règlent avant. Sans
 * cette recherche, un parcours de règlement tombait sur un organisme sans rien à régler, et
 * échouait sur l'absence d'un bouton — en laissant croire à un défaut de l'écran.
 */
export async function ouvrirFactureARegler(page: Page): Promise<Locator> {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const organismes = await lignes.count();

  for (let rang = 0; rang < Math.min(organismes, 8); rang++) {
    const detail = lignes.nth(rang).locator('button:has(.pi-eye)').first();
    if ((await detail.count()) === 0) {
      continue;
    }
    await detail.click();
    const regler = page.getByRole('button', { name: 'Régler' }).first();
    const present = await regler.waitFor({ state: 'visible', timeout: 4000 })
      .then(() => true)
      .catch(() => false);
    if (present) {
      return regler;
    }
  }
  throw new Error('Aucune facture à régler dans le rapprochement : jeu de démonstration incomplet.');
}
