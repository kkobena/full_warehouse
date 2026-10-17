import { expect } from '@playwright/test';
import { ouvrirBonDeReception, ouvrirOnglet } from '../../src/actions';
import { executer, lire } from '../../src/base-de-donnees';
import { scenario } from '../../src/scenario';

/**
 * Le problème que ce parcours montre est la règle, pas l'exception : en Côte d'Ivoire les
 * grossistes ne reprennent pas les codes du DataMatrix du fabricant. Le code imprimé sur la
 * boîte n'est donc PAS celui du catalogue, et le scan ne résout rien — alors que la boîte
 * porte justement ce qu'on voudrait ne pas ressaisir : le lot et la péremption.
 *
 * L'écran ne jette plus ce scan. Il rappelle le code lu, affiche ce que la boîte portait, et
 * demande seulement de désigner la ligne. Le code est alors MÉMORISÉ sur l'article : la
 * deuxième boîte du même carton passe seule, et le même code sera reconnu au comptoir.
 *
 * Le code scanné ici est une trame GS1 complète, écrite avec le séparateur en notation
 * textuelle `{GS}` — c'est ce que produisent les scanners mal configurés et les imports de
 * fichier, et l'application la normalise comme un vrai 0x1D. Le GTIN qu'elle porte
 * (07353163006031, clé de contrôle juste) n'est dans aucune fiche : c'est tout l'intérêt.
 *
 * Le rejeu du scan ouvre la confirmation d'EXCÉDENT, et ce n'est pas un accident du parcours :
 * dans le jeu de démonstration, un bon livré arrive avec ses quantités reçues déjà égales aux
 * quantités commandées — le grossiste a servi la commande. Toute boîte scannée en plus dépasse
 * donc le commandé, et l'écran le demande avant de l'accepter. On répond « Oui », puisque la
 * boîte est bien là : c'est l'aboutissement du scénario, la ligne reçoit sa quantité.
 *
 * Parcours ÉCRIVANT : il mémorise un code et laisse le scan rejoué incrémenter une ligne du
 * bon — qui n'est pas finalisé, donc sans effet sur le stock. Le code mémorisé est retiré à
 * la fin, par son code, qui n'appartient qu'à ce parcours.
 */
const TRAME = '0107353163006031{GS}10ABC101{GS}17270131';
const CODE_MEMORISE = '7353163006031';

scenario('ACH-85', async ({ etape, page }) => {
  const ecran = page.locator('#main-content');
  const modale = page.locator('.modal-content');
  const resolution = page.locator('app-code-resolution');

  // ── Décor : un bon encore vierge de saisie, en VUE TABLEAU — le mode séquentiel, lui,
  //    traite le code inconnu par un bandeau sur la ligne courante (ACH-86).
  await page.goto('/commande');
  await ouvrirOnglet(page, /Commandes & Réceptions/);
  await page.getByRole('tab', { name: /^Réceptions/ }).click();
  await ouvrirBonDeReception(page, 'aucun');
  await page.getByRole('button', { name: 'Grille' }).click();
  await expect(page.locator('app-reception-concordance')).toBeVisible();

  await etape(1, async () => {
    const champ = ecran.getByPlaceholder(/Scanner CIP/);
    await champ.fill(TRAME);
    await champ.press('Enter');
    // La fenêtre s'ouvre d'elle-même : c'est le refus qui demande une décision, et non
    // l'utilisateur qui doit aller la chercher.
    await expect(modale).toContainText(/Code non reconnu/i);
    await expect(resolution).toBeVisible();
  });

  await etape(2, async () => {
    // Ce que la boîte portait est affiché AVANT tout choix : c'est ce qui permet au
    // pharmacien de vérifier qu'il tient bien l'article qu'il va désigner. « Lu sur la
    // boîte » et non « déduit » : la trame respecte le standard, rien n'est deviné.
    await expect(resolution).toContainText(CODE_MEMORISE);
    await expect(resolution).toContainText('Lu sur la boîte');
    await expect(resolution).toContainText('ABC101');
    await expect(resolution).toContainText('31/01/2027');
    // Cochée d'avance : la mémorisation est le cas normal, l'oubli serait le défaut.
    await expect(resolution.getByRole('checkbox', { name: /Mémoriser ce code/ })).toBeChecked();
  });

  await etape(3, async () => {
    // Les lignes du bon, et non tout le catalogue : la boîte scannée appartient forcément
    // au carton qu'on est en train de compter.
    const premiere = resolution.locator('.cr-line').first();
    await expect(premiere).toBeVisible();
    await premiere.click();
    await resolution.getByRole('button', { name: 'Confirmer' }).click();
    // Le scan est REJOUÉ une fois le code mémorisé : la ligne est reconnue cette fois-ci,
    // et c'est le chemin ordinaire qui applique la quantité — contrôle d'excédent compris.
    //
    // Le rejeu repasse par le SERVEUR : la question d'excédent n'arrive donc pas dans le même
    // cycle que la fermeture de la fenêtre de résolution. Tester sa présence aussitôt la
    // trouvait absente, on enchaînait, et la question s'ouvrait ensuite pour rester sans
    // réponse. On attend donc l'un OU l'autre des deux aboutissements possibles.
    const excedent = page.locator('.modal-content:visible').filter({ hasText: 'Excédent détecté' });
    const badge = ecran.locator('.cr-scan-badge--ok');
    await expect(excedent.first().or(badge).first()).toBeVisible();
    if (await excedent.first().isVisible().catch(() => false)) {
      await excedent.first().getByRole('button', { name: /Oui/ }).click();
    }
    await expect(badge).toBeVisible();
    await expect(page.locator('.modal-content')).toHaveCount(0);
  });

  // ── Remise en état : le code mémorisé n'appartient qu'à ce parcours, et le poste repart
  //    en saisie séquentielle, mode nominal mémorisé dans le stockage local (cf. ACH-84).
  const memorises = lire(`SELECT count(*) FROM produit_code_alias WHERE code = '${CODE_MEMORISE}'`);
  expect(Number(memorises), 'le code scanné n’a pas été mémorisé').toBeGreaterThan(0);
  executer(`DELETE FROM produit_code_alias WHERE code = '${CODE_MEMORISE}'`);
  await page.getByRole('button', { name: 'Séquentiel' }).click();
  await expect(page.locator('app-reception-sequential')).toBeVisible();
});
