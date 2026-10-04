import { TestBed } from '@angular/core/testing';

import { ChromeThemeService } from './chrome-theme.service';
import { applyChrome, CHROMES, DEFAULT_CHROME, readStoredChrome } from './chrome-theme';

describe('ChromeThemeService', () => {
  const build = (): ChromeThemeService => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [ChromeThemeService] });
    return TestBed.inject(ChromeThemeService);
  };

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-chrome');
  });

  it('applique le défaut en l’absence de préférence', () => {
    build().loadCurrentChrome();

    expect(DEFAULT_CHROME).toBe('prevente-comptant');
    expect(document.documentElement.getAttribute('data-chrome')).toBe('prevente-comptant');
  });

  it('respecte la couleur déjà enregistrée sur le poste', () => {
    localStorage.setItem('pharmasmart_chrome', 'comptant');

    build().loadCurrentChrome();

    expect(document.documentElement.getAttribute('data-chrome')).toBe('comptant');
  });

  it('ignore une valeur enregistrée inconnue', () => {
    localStorage.setItem('pharmasmart_chrome', 'fluo');

    expect(build().chrome()).toBe(DEFAULT_CHROME);
    expect(readStoredChrome()).toBe(DEFAULT_CHROME);
  });

  it('applique et persiste le changement', () => {
    const service = build();

    service.setChrome('prevente-carnet');

    expect(service.chrome()).toBe('prevente-carnet');
    expect(document.documentElement.getAttribute('data-chrome')).toBe('prevente-carnet');
    expect(localStorage.getItem('pharmasmart_chrome')).toBe('prevente-carnet');
  });

  it('suit un changement fait dans une autre fenêtre', () => {
    const service = build();
    service.loadCurrentChrome();

    window.dispatchEvent(new StorageEvent('storage', { key: 'pharmasmart_chrome', newValue: 'assurance' }));

    expect(service.chrome()).toBe('assurance');
    expect(document.documentElement.getAttribute('data-chrome')).toBe('assurance');
  });

  it('ne touche ni au fond de page ni à sa clé', () => {
    document.documentElement.setAttribute('data-theme', 'ardoise');

    build().setChrome('comptant');

    expect(document.documentElement.getAttribute('data-theme')).toBe('ardoise');
    expect(localStorage.getItem('pharmasmart_theme')).toBeNull();
  });

  it('applyChrome pose l’attribut sur la racine donnée', () => {
    const root = document.createElement('html');

    applyChrome('assurance', root);

    expect(root.getAttribute('data-chrome')).toBe('assurance');
    expect(CHROMES[0].name).toBe(DEFAULT_CHROME);
  });
});
