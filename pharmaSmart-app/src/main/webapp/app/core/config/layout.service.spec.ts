import { TestBed } from '@angular/core/testing';

import { LayoutService } from './layout.service';

describe('LayoutService', () => {
  const build = (): LayoutService => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [LayoutService] });
    return TestBed.inject(LayoutService);
  };

  beforeEach(() => localStorage.clear());

  describe('état replié de la sidebar', () => {
    it('démarre sur le rail réduit en l’absence de préférence', () => {
      expect(build().isSidebarCollapsed()).toBe(true);
    });

    it('respecte une préférence « déployé » déjà enregistrée', () => {
      localStorage.setItem('pharmasmart_sidebar_collapsed', 'false');

      expect(build().isSidebarCollapsed()).toBe(false);
    });

    it('persiste la bascule', () => {
      const service = build();

      service.toggleSidebarCollapsed();

      expect(service.isSidebarCollapsed()).toBe(false);
      expect(localStorage.getItem('pharmasmart_sidebar_collapsed')).toBe('false');
    });
  });
});
