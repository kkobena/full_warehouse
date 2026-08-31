import { expect, Page } from '@playwright/test';

/**
 * Les rapports sont regroupés en quatre pôles — ventes, stock, partenaires, finance — chacun
 * derrière une adresse unique (`/reports/sales`, `/reports/stock`…) et un menu vertical. Un
 * rapport n'a donc pas d'URL propre : on l'atteint en ouvrant son pôle puis sa section.
 *
 * Ce détour est celui de l'utilisateur, et les captures doivent le montrer tel quel.
 */
export async function ouvrirRapport(page: Page, pole: string, section: string | RegExp): Promise<void> {
  await page.goto(`/reports/${pole}`);
  const lien = page.locator('.pharma-nav-vertical-link').filter({ hasText: section }).first();
  await expect(lien).toBeVisible({ timeout: 20000 });
  await lien.click();
}
