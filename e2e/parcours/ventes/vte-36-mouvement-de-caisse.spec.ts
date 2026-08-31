import { expect } from '@playwright/test';
import { assurerCaisseOuverte, choisirDansSelect } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Tout ce qui entre ou sort du tiroir sans être une vente doit être écrit quelque part :
 * l'appoint apporté le matin, la course payée en liquide, le prélèvement du soir. Sans ces
 * mouvements, la caisse comptée le soir ne peut pas tomber juste, et l'écart reste
 * inexplicable.
 *
 * Chaque mouvement porte un TYPE (entrée ou sortie), un mode de règlement, un montant et un
 * commentaire — c'est ce dernier qui, au contrôle, dira ce que les chiffres taisent.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre un mouvement de caisse réel.
 */
scenario('VTE-36', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const modale = page.locator('.modal-content');

  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await page.goto('/mvt-caisse');
    await expect(contenu).toContainText('Mouvements de caisse');
    // La liste tient l'historique : type, référence, montant, mode, opérateur. C'est elle
    // qu'on rapprochera du comptage.
    await expect(contenu).toContainText('Liste des mouvements');
    await page.getByRole('button', { name: 'Nouveau' }).click();
    await expect(modale).toContainText("FORMULAIRE D'AJOUT DE MOUVEMENT DE CAISSE");
  });

  await etape(2, async () => {
    await choisirDansSelect(page, 'field_typeFinancialTransaction', 'Sortie de caisse');
    const montant = modale.locator('#field_amount');
    await montant.click();
    await montant.pressSequentially('5000', { delay: 40 });
    // Le commentaire n'est pas décoratif : c'est la seule chose qui expliquera ce mouvement
    // au moment du contrôle de caisse.
    await modale.locator('#field_commentaire').fill('Achat de fournitures pour le comptoir');
  });

  await etape(3, async () => {
    await modale.getByRole('button', { name: 'Valider' }).click();
    // Un mouvement de caisse se justifie sur papier : l'application propose son ticket dans
    // la foulée. Ici on décline — le parcours n'a pas d'imprimante.
    await expect(modale).toContainText('imprimer le ticket');
    await modale.getByRole('button', { name: 'Non' }).click();
    await expect(modale).toBeHidden();

    // Le mouvement rejoint la liste, avec son montant et son opérateur.
    await expect(contenu).toContainText('5 000');
  });
});
