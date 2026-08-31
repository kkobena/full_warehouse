import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Un inventaire général se compte souvent hors de l'application : sur un terminal portable,
 * sur un tableur, ou avec la douchette d'un prestataire. Le résultat arrive alors sous forme
 * de fichier, et le ressaisir ligne à ligne annulerait tout le bénéfice.
 *
 * L'import vient renseigner la quantité comptée des lignes de l'inventaire — il ne les crée
 * pas : un code absent de l'inventaire n'y entre pas par la bande. Le compte rendu distingue
 * donc trois sorts : les lignes IMPORTÉES, celles IGNORÉES et celles REJETÉES, et c'est
 * cette dernière colonne qu'on regarde en premier.
 *
 * L'import complète la saisie manuelle (STK-05) et le scan (STK-06) plutôt qu'il ne les
 * remplace : on peut importer le gros du comptage puis corriger deux lignes à la main.
 *
 * Parcours en LECTURE : il ouvre l'import sans envoyer de fichier.
 */
scenario('STK-26', async ({ etape, page }) => {
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');
  let codeCip: string | null = null;

  await etape(1, async () => {
    await page.goto('/inventaire');
    await expect(lignes.first()).toContainText(/en cours|créé/i);
    await lignes.first().getByRole('button', { name: 'Ouvrir' }).click();
    await expect(page.getByRole('button', { name: 'Importer CSV' })).toBeVisible();

    // On retient le code d'une ligne réellement inventoriée : c'est lui qu'on comptera.
    // Le détail d'un inventaire est rendu par AG Grid, pas par un `<table>` : ses lignes
    // sont des `.ag-row`, et les chercher dans un `tbody` n'aboutit jamais.
    const premiereLigne = page.locator('.ag-row').first();
    await expect(premiereLigne).toBeVisible();
    codeCip = /\b\d{6,8}\b/.exec(await premiereLigne.innerText())?.[0] ?? null;
    expect(codeCip).not.toBeNull();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Importer CSV' }).click();
    await expect(modale).toContainText(/Importer un fichier CSV/);
    await expect(modale).toContainText(/Fichier CSV/);
  });

  await etape(3, async () => {
    // Le format attendu est annoncé : code CIP ou EAN, puis quantité. On envoie deux lignes,
    // dont une au code inconnu de l'inventaire — c'est elle qui alimentera les « ignorées ».
    //
    // Le PREMIER code est lu sur l'écran plutôt que gravé ici : les lignes d'un inventaire
    // dépendent du jeu de données, et un code figé finit par ne désigner personne. L'import
    // ne compte alors rien, et le parcours accuse l'application à tort.
    await expect(modale).toContainText(/Format attendu/);
    await modale.locator('input[type="file"]').first().setInputFiles({
      name: 'comptage-terminal.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from([`${codeCip ?? '9999998'};12`, '9999999;4', ''].join('\n'), 'utf-8'),
    });
    await modale.getByRole('button', { name: 'Importer' }).click();
    // Le compte rendu en trois colonnes : entrées, ignorées, rejetées. C'est la dernière
    // qu'on regarde en premier — un code absent de l'inventaire n'y entre pas par la bande.
    await expect(modale).toContainText(/Importées/);
    await expect(modale).toContainText(/Ignorées/);
    await expect(modale).toContainText(/Rejetées/);
  });
});
