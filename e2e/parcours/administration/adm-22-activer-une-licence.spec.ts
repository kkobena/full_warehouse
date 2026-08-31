import { expect } from '@playwright/test';
import { scenario } from '../../src/scenario';

/**
 * Le renouvellement annuel se joue ici, et il tient en un fichier : le `.lic` reçu du
 * revendeur se dépose, et l'abonnement repart. Aucune intervention sur le serveur, aucun
 * redémarrage.
 *
 * La licence est SIGNÉE : un fichier retouché, destiné à une autre officine ou à un autre
 * poste est refusé, et l'abonnement en place reste tel qu'il était. C'est ce qui permet de
 * confier ce geste au pharmacien sans risque de casse.
 *
 * L'activation elle-même n'est pas jouée : elle remplacerait la licence de la base de
 * démonstration, et le manuel ne peut pas se payer un abonnement par campagne.
 */
scenario('ADM-22', async ({ etape, page }) => {
  const contenu = page.locator('#main-content');

  await etape(1, async () => {
    await page.goto('/licence');
    await expect(contenu).toContainText('Activer une licence');
  });

  await etape(2, async () => {
    // Le fichier se dépose dans la zone prévue, ou se choisit par le sélecteur : deux gestes
    // pour le même résultat, selon que l'on vient d'un courriel ou d'un dossier.
    await expect(contenu).toContainText('.lic');
    await contenu.locator('input[type="file"]').first().setInputFiles({
      name: 'pharmasmart-2026.lic',
      mimeType: 'application/octet-stream',
      buffer: Buffer.from('exemple de fichier de licence'),
    });
    // Le bouton « Activer » n'existe qu'une fois un fichier retenu : tant qu'il n'y a rien à
    // activer, il n'y a rien à cliquer.
    await expect(contenu).toContainText('pharmasmart-2026.lic');
    await expect(contenu.getByRole('button', { name: 'Activer', exact: true })).toBeVisible();
  });

  etape.horsPortee(
    3,
    "activer un fichier remplacerait la licence de la base de démonstration ; le fichier est " +
      'choisi, l’activation elle-même n’est pas jouée.',
  );
});
