import { expect } from '@playwright/test';
import {
  assurerCaisseOuverte,
  ouvrirFactureARegler,
  ouvrirOnglet,
  rechercher,
  traverserConfirmations,
} from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le tiers payant qui règle au comptoir repart avec un justificatif : le ticket de règlement.
 * L'application le propose immédiatement après l'enregistrement — c'est le seul moment où il
 * est utile, et le proposer plus tard reviendrait à le faire oublier.
 *
 * L'impression emprunte deux chemins selon le poste : l'impression du navigateur, ou le flux
 * ESC/POS de l'application desktop vers l'imprimante thermique. Le geste, lui, est le même.
 *
 * Parcours ÉCRIVANT dans la base : il enregistre un règlement d'un montant symbolique pour
 * atteindre la proposition d'impression, puis la décline — imprimer n'aurait rien à montrer
 * d'autre qu'un fichier.
 */
scenario('FAC-48', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');
  const lignes = page.locator('tbody tr').filter({ visible: true });
  const modale = page.locator('.modal-content');

  // Un règlement de tiers payant ENTRE en caisse : il lui faut une caisse ouverte, comme un
  // encaissement au comptoir. C'est le serveur qui le dit — « Votre caisse est fermée ».
  await assurerCaisseOuverte(page);

  await etape(1, async () => {
    await page.goto('/facturation');
    await ouvrirOnglet(page, /Rapprochement/);
    await rechercher(page);
    const regler = await ouvrirFactureARegler(page);
    await expect(contenu).toContainText('N° Facture');
    await regler.click();
    await expect(modale).toBeVisible();

    // Un montant symbolique : le parcours vient chercher la proposition d'impression, pas
    // solder une créance.
    // L'identifiant porte sur le composant `app-input-number`, pas sur son champ : c'est
    // l'input qu'il enveloppe qu'il faut viser.
    const montant = modale.locator('#amount input');
    await montant.click();
    await montant.fill('');
    await montant.pressSequentially('1000', { delay: 30 });
    // Les espèces, explicitement : un chèque réclamerait la banque, son numéro et le
    // bénéficiaire, et la validation resterait fermée tant qu'ils manquent.
    await modale.locator('ng-select').first().click();
    await page.locator('.ng-option').filter({ hasText: /ESPECE/i }).first().click();
    await modale.getByRole('button', { name: 'Valider' }).click();
  });

  await etape(2, async () => {
    // La proposition arrive dans la foulée de l'enregistrement, sans avoir à la demander.
    const question = page.locator('.modal-content:visible').first();
    await expect(question).toContainText(/Ticket règlement/);
    await expect(question).toContainText(/imprimer le ticket/i);
    // On décline : l'impression n'a rien de plus à montrer.
    await traverserConfirmations(page, { refus: [/Ticket règlement/] });
  });
});
