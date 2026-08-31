import { expect } from '@playwright/test';
import { assurerCaisseFermee } from '../../src/actions';
import { scenario } from '../../src/scenario';

/**
 * Le premier geste de la journée. Le fonds saisi ici est le point de départ du contrôle du
 * soir : c'est par rapport à lui que le comptage de clôture fera apparaître un écart.
 *
 * Parcours ÉCRIVANT dans la base. Il commence par s'assurer que la caisse est fermée —
 * d'autres parcours l'ouvrent, et un scénario d'ouverture n'a de sens que caisse fermée.
 */
scenario('VTE-33', async ({ etape, page }) => {
  await assurerCaisseFermee(page);

  await etape(1, async () => {
    await page.goto('/my-cash-register');
    // Caisse fermée, l'écran ne propose que l'ouverture : c'est ce qui distingue cet état.
    await expect(page.getByText('Ouverture de caisse')).toBeVisible();
    const montant = page.locator('#cashFundAmount');
    // Attendre le focus avant de saisir : l'écran le pose 100 ms après son affichage, en
    // écrivant au passage le fonds proposé. Saisir avant, c'est écrire dans un champ qui
    // sera réécrit — le bouton d'ouverture reste alors désactivé, sans explication.
    await expect(montant).toBeFocused();
    await montant.fill('50000');
    await expect(montant).toHaveValue(/50/);
    // La validité du formulaire est ce que l'utilisateur voit : le bouton s'active.
    await expect(page.getByRole('button', { name: 'Ouvrir la caisse' })).toBeEnabled();
  });

  await etape(2, async () => {
    await page.getByRole('button', { name: 'Ouvrir la caisse' }).click();
    // La session est ouverte : le fonds saisi, l'heure d'ouverture et le statut la décrivent.
    await expect(page.getByText(/Ma caisse en cours/i)).toBeVisible();
    await expect(page.locator('#main-content')).toContainText(/50\s?000/);
    await expect(page.locator('#main-content')).toContainText(/En cours/i);
  });
});
