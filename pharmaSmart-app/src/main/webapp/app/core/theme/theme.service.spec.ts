import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  const build = (): ThemeService => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [ThemeService] });
    return TestBed.inject(ThemeService);
  };

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('applique Menthe en l’absence de préférence', () => {
    build().loadCurrentTheme();

    expect(document.documentElement.getAttribute('data-theme')).toBe('menthe');
  });

  it('respecte le thème déjà enregistré sur le poste', () => {
    localStorage.setItem('pharmasmart_theme', 'ardoise');

    build().loadCurrentTheme();

    expect(document.documentElement.getAttribute('data-theme')).toBe('ardoise');
  });

  it('ignore une valeur enregistrée inconnue', () => {
    localStorage.setItem('pharmasmart_theme', 'sombre');

    expect(build().theme()).toBe('menthe');
  });

  it('applique et persiste le changement de thème', () => {
    const service = build();

    service.setTheme('clair');

    expect(service.theme()).toBe('clair');
    expect(document.documentElement.getAttribute('data-theme')).toBe('clair');
    expect(localStorage.getItem('pharmasmart_theme')).toBe('clair');
  });
});
