import { expect } from '@playwright/test';
import { choisirDansSelect } from '../../src/actions';
import { lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Une gamme entière retirée de la vente — un laboratoire suspendu, une série rappelée — ne se
 * traite pas fiche par fiche : on en oublie une, et c'est celle-là qui se vendra. La sélection
 * multiple du catalogue ouvre une barre d'actions qui applique le même geste à tout le lot.
 *
 * Les deux actions demandent une confirmation, et celle-ci rappelle le NOMBRE de produits
 * concernés : c'est le dernier moment où l'on peut s'apercevoir qu'on en tient plus qu'on ne
 * croyait. Une fois le traitement passé, la sélection se vide d'elle-même, pour qu'un second
 * clic ne reparte pas sur un lot déjà traité.
 *
 * ── Pourquoi la remise en état est sous `finally` ────────────────────────────────────────────
 * Elle vivait à la suite des étapes, dans le corps du parcours. Une étape qui échoue interrompt
 * ce corps : la réactivation était donc sautée, et la gamme restait en veille EN BASE. Le
 * parcours suivant filtrait alors sur les produits actifs, n'en trouvait aucun de cette gamme,
 * et mourait sur un clic resté sans cible — quatre-vingt-dix secondes d'attente pour un échec
 * dont la cause était l'exécution précédente, et non le code.
 *
 * D'où deux précautions : la réactivation par l'écran est protégée par un `finally`, et un
 * filet rattrape ce que l'écran n'aurait pas pu faire — une modale restée ouverte, une session
 * perdue. Le parcours répare aussi ce qu'il trouve en arrivant, pour qu'un incident passé ne se
 * paie pas indéfiniment.
 *
 * ── Et pourquoi ce filet passe par l'APPLICATION, jamais par un UPDATE ──────────────────────
 * `Produit` est en cache de second niveau. Un `UPDATE produit SET status = 'ENABLE'` exécuté
 * en base pendant que le serveur tourne laisse le cache sur l'ancienne valeur : la mise en
 * veille suivante charge une entité qui se croit DÉJÀ en veille, Hibernate ne voit aucun champ
 * modifié, n'écrit rien — et l'application répond 200. Le parcours constatait alors que la
 * veille n'était pas allée en base et accusait l'enregistrement, quand le tort venait de la
 * réparation. Le filet emprunte donc le même chemin que l'utilisateur.
 *
 * Parcours ÉCRIVANT dans la base : il réactive les produits qu'il a mis en veille.
 */
scenario('REF-60', async ({ etape, page }) => {
  const recherche = 'ARNICA MONTANA TEINTURE';
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const barre = page.locator('.bulk-action-bar');
  const confirmation = page.locator('.modal-content');

  /** Combien de produits de la gamme sont en veille en base. */
  const enVeille = (): string =>
    lire(`SELECT count(*) FROM produit WHERE libelle LIKE '${recherche}%' AND status <> 'ENABLE'`);

  const gamme = Number(lire(`SELECT count(*) FROM produit WHERE libelle LIKE '${recherche}%'`));
  expect(gamme, `la démo doit porter au moins deux produits « ${recherche} »`).toBeGreaterThanOrEqual(2);

  // Le jeton est relevé une fois, et sert au filet : `page.request` vit au niveau du contexte,
  // il reste donc utilisable même si la page s'est abîmée en route.
  await page.goto('/produits');
  const jeton = await page.evaluate(() => localStorage.getItem('pharma_smart_access_token') ?? '');

  /**
   * Rend la gamme active par l'application, quoi qu'il soit arrivé à l'écran.
   *
   * L'appel part DEPUIS LA PAGE : `page.request` vise le serveur en direct, hors du proxy du
   * serveur de développement, et échouait sans bruit — le filet ne rattrapait rien et la gamme
   * restait en veille pour les exécutions suivantes. Un `fetch` emprunte exactement le chemin
   * de l'application.
   */
  const reactiverParLApplication = async (): Promise<void> => {
    const ids = lire(
      `SELECT COALESCE(string_agg(id::text, ','), '') FROM produit
        WHERE libelle LIKE '${recherche}%' AND status <> 'ENABLE'`,
    )
      .split(',')
      .filter(Boolean);
    if (!ids.length) {
      return;
    }
    await page.evaluate(
      async ({ identifiants, cle }) => {
        for (const id of identifiants) {
          await fetch(`/api/produits/${id}/status?status=ENABLE`, {
            method: 'PATCH',
            headers: { Authorization: `Bearer ${cle}` },
          });
        }
      },
      { identifiants: ids, cle: jeton },
    );
  };

  // Le décor est rétabli AVANT de commencer : on ne veut pas qu'un incident ancien se lise
  // comme un défaut du jour.
  await reactiverParLApplication();

  const chercherAuCatalogue = async (etat: 'Produits actifs' | 'Produits désactivés'): Promise<void> => {
    await choisirDansSelect(page, 'produitFiltreEtat', etat);
    const champ = page.getByPlaceholder(/Rechercher \(CIP/);
    await champ.fill('');
    await champ.fill(recherche);
    await page.keyboard.press('Enter');
  };

  try {
    await etape(1, async () => {
      await page.goto('/produits');
      await chercherAuCatalogue('Produits actifs');
      await expect(lignes.first()).toContainText(recherche);
      // Deux lignes suffisent à montrer le lot ; l'action est la même à deux ou à deux cents.
      await lignes.nth(0).getByRole('checkbox', { name: /^Sélectionner/ }).check();
      await lignes.nth(1).getByRole('checkbox', { name: /^Sélectionner/ }).check();
      // La barre d'actions groupées n'apparaît qu'avec une sélection, et compte ce qu'elle tient.
      await expect(barre).toContainText('2 produit(s) sélectionné(s)');
    });

    await etape(2, async () => {
      await barre.getByRole('button', { name: 'Mettre en veille' }).click();
      // La confirmation redit le nombre : le lot traité est celui qu'on a sous les yeux.
      await expect(confirmation).toContainText('2 produit(s)');
    });

    await etape(3, async () => {
      await confirmation.getByRole('button', { name: 'Oui' }).click();
      await expect(confirmation).toBeHidden();
      // La sélection s'est vidée : la barre a disparu avec elle. C'est ce qui évite d'appliquer
      // deux fois le même traitement en croyant travailler sur un nouveau lot.
      await expect(barre).toBeHidden();
      // La base AVANT l'écran : si l'écriture n'a pas eu lieu, le dire ainsi plutôt que de
      // laisser le filtre « désactivés » répondre « Aucun produit trouvé », ce qui accuse la
      // recherche d'un tort qui est celui de l'enregistrement.
      await expect
        .poll(enVeille, { timeout: 15_000, message: 'les deux produits doivent passer en veille en base' })
        .toBe('2');
      await chercherAuCatalogue('Produits désactivés');
      await expect(lignes.nth(0)).toContainText('Veille');
      await expect(lignes.nth(1)).toContainText('Veille');
    });

    // ── Remise en état par l'écran : c'est la réactivation groupée, l'autre moitié du geste
    //    que ce parcours documente.
    //
    //    Les lignes sont cochées UNE À UNE, comme à l'étape 1, et non par « Tout sélectionner » :
    //    la case d'en-tête n'est pas toujours visible, et le clic s'y épuisait jusqu'au bout du
    //    délai du test. Or un test qui expire est ABANDONNÉ par Playwright : le `finally` ne
    //    reprend jamais la main, et la gamme restait en veille pour toutes les exécutions
    //    suivantes. Chaque geste de la remise en état est donc borné à dix secondes, pour que
    //    son échec laisse vivre le filet au lieu de consumer le budget du test.
    //    Le geste entier est RETENTÉ. Changer de filtre puis chercher, c'est deux appels : le
    //    second repeint la liste après que l'étape 3 l'a vue peinte, la sélection se perd avec
    //    les lignes, et la barre — dont l'affichage dépend de la sélection — disparaît sous le
    //    clic (« element is not stable », puis « detached from the DOM »). Attendre un instant
    //    calme ne suffit pas ; recommencer, oui.
    await expect(async () => {
      const aReactiver = await lignes.count();
      expect(aReactiver, 'les produits mis en veille doivent être listés').toBeGreaterThan(0);
      for (let i = 0; i < aReactiver; i++) {
        await lignes.nth(i).getByRole('checkbox', { name: /^Sélectionner/ }).check({ timeout: 5_000 });
      }
      // Lire le compte avant d'agir : la barre ne porte le bon bouton qu'une fois à jour.
      await expect(barre).toContainText(`${aReactiver} produit(s) sélectionné(s)`, { timeout: 5_000 });
      await barre.getByRole('button', { name: 'Réactiver' }).click({ timeout: 5_000 });
      await expect(confirmation).toBeVisible({ timeout: 5_000 });
    }).toPass({ timeout: 45_000, intervals: [500, 1_000, 2_000] });
    await confirmation.getByRole('button', { name: 'Oui' }).click({ timeout: 10_000 });
    await expect(confirmation).toBeHidden({ timeout: 10_000 });
    await expect(barre).toBeHidden({ timeout: 10_000 });
    await chercherAuCatalogue('Produits actifs');
    await expect(lignes.first()).toContainText('Actif', { timeout: 10_000 });
  } finally {
    // Le filet : si l'écran n'a pas pu réactiver, l'appel direct le fait. Sans lui, un seul
    // échec condamnait toutes les exécutions suivantes — et un filet qui échoue en silence
    // fait exactement le même dégât, d'où le contrôle qui suit.
    await reactiverParLApplication().catch((): void => undefined);
    const restes = enVeille();
    if (restes !== '0') {
      throw new Error(
        `Remise en état INCOMPLÈTE : ${restes} produit(s) « ${recherche} » restent en veille. ` +
          'Les réactiver avant de relancer, sinon les exécutions suivantes échoueront sur un ' +
          'catalogue sans ligne active pour cette gamme.',
      );
    }
  }
});
