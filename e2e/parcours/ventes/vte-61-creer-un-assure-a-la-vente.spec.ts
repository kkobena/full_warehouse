import { expect } from '@playwright/test';
import { assurerCaisseOuverte, assurerPanierVide, chercherDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Un assuré se présente pour la première fois. Le renvoyer au fichier client, ce serait perdre
 * la vente en cours et faire attendre la file — l'écran de vente s'en charge.
 *
 * Le geste n'est pas celui qu'on attend : il n'y a AUCUN bouton « nouveau client » sur le
 * bandeau. C'est la RECHERCHE INFRUCTUEUSE qui ouvre le formulaire de création — chercher
 * d'abord, créer ensuite, dans cet ordre imposé. La conséquence est heureuse : on ne peut pas
 * créer un doublon sans avoir vu que le client n'existait pas.
 *
 * Parcours ÉCRIVANT dans la base : il crée un client. Son matricule porte l'horodatage, faute
 * de quoi le parcours ne serait jouable qu'une fois — le numéro d'adhérent est unique.
 */
scenario('VTE-61', async ({ etape, page }) => {
  const suffixe = Date.now().toString().slice(-6);
  const nom = 'NGUESSAN';
  const prenom = 'Adjoua ' + suffixe;
  // Sans tiret : `appKeyFilter="alphanum"` refuse la ponctuation, et le numéro saisi ne
  // serait pas celui qu'on croit.
  const matricule = 'CNPS01' + suffixe;
  const modale = page.locator('.modal-content');
  const bandeau = page.locator('app-insurance-data-bar');

  await assurerCaisseOuverte(page);
  await assurerPanierVide(page);

  await etape(1, async () => {
    await page.getByRole('tab', { name: /Assurance/ }).click();
    // La recherche d'abord : c'est le geste réel du comptoir, et c'est son échec qui justifie
    // la création. Créer sans avoir cherché, c'est fabriquer des doublons.
    const recherche = page.getByPlaceholder('Rechercher un client assuré');
    await recherche.fill(nom + ' ' + suffixe);
    await recherche.press('Enter');
    // Aucun résultat : le formulaire de création s'ouvre de lui-même, prérempli de rien —
    // c'est la réponse de l'application à « ce client n'existe pas encore ».
    await expect(modale).toContainText("FORMULAIRE D'AJOUT DE NOUVEAU CLIENT");
  });

  await etape(2, async () => {

    // Le formulaire s'initialise APRÈS son ouverture : une saisie trop rapide est effacée
    // sans bruit, et le champ redevient vide. On vérifie donc chaque valeur écrite plutôt que
    // d'attendre un focus que le formulaire ne donne pas toujours.
    const remplir = async (selecteur: string, valeur: string): Promise<void> => {
      const champ = modale.locator(selecteur);
      await expect(champ).toBeVisible();
      await champ.fill(valeur);
      await expect(champ).toHaveValue(valeur);
    };

    // Cinq champs obligatoires, et ce sont exactement ceux dont la vente a besoin : qui est
    // le client, qui paie pour lui, sous quel numéro et à quel taux.
    await remplir('#field_firstName', nom);
    await remplir('#field_lastName', prenom);
    // Le champ « Tiers payant » interroge le serveur : il faut taper pour qu'il propose
    // quelque chose. On cherche par le SIGLE — c'est ce que dit le client — et l'option
    // s'affiche sous sa raison sociale complète, que le sigle ne contient pas.
    await chercherDansSelect(page, 'tiersPayantId', 'CNPS', 'CAISSE NATIONALE DE PREVOYANCE');
    // Matricule et taux portent `appKeyFilter` : ces champs filtrent les TOUCHES, et une
    // valeur posée d'un bloc ne les atteint pas — elle laisse le contrôle vide, donc le
    // formulaire invalide, et le bouton « Enregistrer » ne s'affiche même pas.
    const matriculeChamp = modale.locator('#field_num');
    await matriculeChamp.click();
    await matriculeChamp.pressSequentially(matricule, { delay: 25 });
    const tauxChamp = modale.locator('#field_taux');
    await tauxChamp.click();
    await tauxChamp.pressSequentially('70', { delay: 40 });
    await expect(matriculeChamp).toHaveValue(matricule);
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Enregistrer' }).first().click();
    await expect(modale).toBeHidden();

    // Le client n'est pas seulement créé : il est RATTACHÉ à la vente en cours, avec son
    // organisme et son taux. La vente peut reprendre là où elle s'était arrêtée.
    await expect(bandeau).toContainText(nom);
    await expect(bandeau).toContainText(matricule);
    await expect(bandeau).toContainText('CNPS');
    await expect(bandeau).toContainText(/Taux\s*:\s*70/);
  });
});
