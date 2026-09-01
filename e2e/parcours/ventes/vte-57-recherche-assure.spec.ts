import { expect } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * La recherche d'un assuré est le premier geste de toute vente assurance, et le seul qui
 * conditionne les suivants : ce qu'elle rapporte — les organismes, leurs taux, leurs champs
 * de bon — n'est plus à ressaisir ensuite.
 *
 * Le champ n'interroge RIEN au fil de la frappe : il attend la touche ENTRÉE (`keydown.enter`
 * dans `insurance-data-bar.component.html`). C'est là que l'utilisateur croit l'écran figé, et
 * c'est pourquoi l'étape 2 fait le geste explicitement.
 *
 * Deux comportements à connaître ensuite, que le manuel doit distinguer :
 *  * un résultat UNIQUE est retenu directement, sans liste ni clic ;
 *  * plusieurs résultats ouvrent la liste de choix, où le MATRICULE départage des homonymes.
 *
 * Parcours en LECTURE : il n'ajoute aucun produit et abandonne la vente commencée.
 */
scenario('VTE-57', async ({ etape, page }) => {
  // ASA01-000003 : deux organismes, donc deux champs de bon à l'écran — c'est ce que la
  // recherche rapporte qu'on veut montrer. Contrat figé par `04_clients.sql`.
  const matricule = 'ASA01-000003';
  const modale = page.locator('.modal-content');
  const bandeau = page.locator('app-insurance-data-bar');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Assurance/ }).click();
    await expect(page.getByPlaceholder('Rechercher un client assuré')).toBeVisible();
  });

  await etape(2, async () => {
    // Un nom de famille très porté : c'est le cas courant au comptoir, et celui qui oblige à
    // choisir.
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill('TRAORE');
    // La saisie seule ne cherche rien : c'est Entrée qui déclenche l'interrogation.
    await recherche.press('Enter');
    await expect(modale).toContainText('CLIENTS ASSURÉS');
    // La liste montre le matricule ET les organismes de chacun : de quoi vérifier la
    // couverture avant même de servir.
    await expect(modale.locator('tbody tr').first()).toBeVisible();
  });

  await etape(3, async () => {
    // Le double-clic vaut sélection ; le matricule est ce qui départage les homonymes.
    await modale.locator('tbody tr').filter({ hasText: matricule }).first().dblclick();
    await expect(modale).toBeHidden();
    await expect(bandeau).toContainText(matricule);
  });

  await etape(4, async () => {
    // Un champ de bon PAR organisme : la référence du bon rattache la vente au dossier de
    // remboursement, et chaque organisme veut le sien.
    const bons = page.getByPlaceholder('Numéro de bon');
    await expect(bons).toHaveCount(2);
    const base = Date.now().toString().slice(-8);
    for (let rang = 0; rang < 2; rang++) {
      const bon = bons.nth(rang);
      await bon.click();
      // `appKeyFilter` filtre les touches : une valeur posée d'un bloc n'atteint pas le champ.
      await bon.pressSequentially('BON' + base + rang, { delay: 25 });
      await expect(bon).toHaveValue('BON' + base + rang);
    }
  });
});
