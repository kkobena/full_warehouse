import { estRetournable, montantRetour } from './retour-client-api.service';

describe('montantRetour', () => {
  it('rend la part de la quantité retournée : le montant remboursable vaut pour toute la ligne', () => {
    expect(montantRetour({ quantitySold: 3, quantiteRetour: 1, montantRemboursableClient: 1_350 })).toBe(450);
    expect(montantRetour({ quantitySold: 2, quantiteRetour: 2, montantRemboursableClient: 900 })).toBe(900);
    expect(montantRetour({ quantitySold: 2, quantiteRetour: 0, montantRemboursableClient: 900 })).toBe(0);
  });
});

describe('estRetournable', () => {
  it('écarte une ligne interdite et une ligne en promotion refusée', () => {
    expect(estRetournable({})).toBe(true);
    expect(estRetournable({ retourInterdit: true })).toBe(false);
    expect(estRetournable({ motifRefus: 'vente à crédit' })).toBe(false);
  });
});
