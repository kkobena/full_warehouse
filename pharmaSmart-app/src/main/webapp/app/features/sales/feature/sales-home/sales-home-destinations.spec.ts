import { listerDestinations } from './sales-home-destinations';

describe('listerDestinations', () => {
  const tout = { comptoir: true, prevente: true, proforma: true };

  it('propose les autres écrans dans l’ordre, jamais celui où l’on est', () => {
    expect(listerDestinations(tout, 'comptoir').map(d => d.id)).toEqual(['prevente', 'proforma']);
    expect(listerDestinations(tout, 'prevente').map(d => d.id)).toEqual(['comptoir', 'proforma']);
    expect(listerDestinations(tout, 'proforma').map(d => d.id)).toEqual(['comptoir', 'prevente']);
  });

  it('porte l’adresse de chaque écran', () => {
    expect(listerDestinations(tout, 'comptoir').map(d => d.url)).toEqual(['/sales-home/prevente', '/sales-home/devis']);
  });

  it('retire ce que l’habilitation refuse', () => {
    expect(listerDestinations({ ...tout, proforma: false }, 'comptoir').map(d => d.id)).toEqual(['prevente']);
    expect(listerDestinations({ ...tout, comptoir: false }, 'prevente').map(d => d.id)).toEqual(['proforma']);
  });

  it('n’affiche rien quand il n’y a nulle part où aller', () => {
    expect(listerDestinations({ comptoir: true, prevente: false, proforma: false }, 'comptoir')).toEqual([]);
    expect(listerDestinations({ comptoir: false, prevente: false, proforma: false }, 'comptoir')).toEqual([]);
  });
});
