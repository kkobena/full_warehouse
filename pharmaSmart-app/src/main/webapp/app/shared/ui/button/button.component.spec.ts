import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ButtonComponent } from './button.component';

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<ButtonComponent>;

  /** Classes posées sur le `<button>` interne, seule surface réellement contractuelle. */
  const classes = (): string[] => {
    fixture.detectChanges();
    return [...fixture.nativeElement.querySelector('button').classList];
  };

  const button = (): HTMLButtonElement => {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('button');
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ButtonComponent] }).compileComponents();
    fixture = TestBed.createComponent(ButtonComponent);
  });

  describe('severity', () => {
    it('rend .btn-primary par défaut', () => {
      expect(classes()).toContain('btn-primary');
    });

    it.each([
      ['secondary', 'btn-secondary'],
      ['success', 'btn-success'],
      ['info', 'btn-info'],
      ['danger', 'btn-danger'],
      ['help', 'btn-help'],
      ['contrast', 'btn-contrast'],
    ] as const)('mappe severity "%s" sur .%s', (severity, expected) => {
      fixture.componentRef.setInput('severity', severity);
      expect(classes()).toContain(expected);
    });

    // Seul mapping où les deux vocabulaires divergent.
    it('mappe la severity PrimeNG "warn" sur .btn-warning', () => {
      fixture.componentRef.setInput('severity', 'warn');
      expect(classes()).toContain('btn-warning');
      expect(classes()).not.toContain('btn-warn');
    });
  });

  describe('variantes', () => {
    it('outlined rend .btn-outline-* au lieu de .btn-*', () => {
      fixture.componentRef.setInput('outlined', true);
      fixture.componentRef.setInput('severity', 'danger');
      expect(classes()).toContain('btn-outline-danger');
      expect(classes()).not.toContain('btn-danger');
    });

    it('text rend .btn-link coloré et sans soulignement', () => {
      fixture.componentRef.setInput('text', true);
      fixture.componentRef.setInput('severity', 'success');
      expect(classes()).toEqual(expect.arrayContaining(['btn-link', 'text-success', 'text-decoration-none']));
      expect(classes()).not.toContain('btn-success');
    });

    it('text prime sur outlined', () => {
      fixture.componentRef.setInput('text', true);
      fixture.componentRef.setInput('outlined', true);
      expect(classes()).toContain('btn-link');
      expect(classes()).not.toContain('btn-outline-primary');
    });

    it.each([
      ['small', 'btn-sm'],
      ['large', 'btn-lg'],
    ] as const)('size "%s" ajoute .%s', (size, expected) => {
      fixture.componentRef.setInput('size', size);
      expect(classes()).toContain(expected);
    });

    it('size "normal" n\'ajoute aucune classe de taille', () => {
      expect(classes()).not.toContain('btn-sm');
      expect(classes()).not.toContain('btn-lg');
    });

    it('rounded ajoute .rounded-pill', () => {
      fixture.componentRef.setInput('rounded', true);
      expect(classes()).toContain('rounded-pill');
    });

    it('raised ajoute .shadow-sm', () => {
      fixture.componentRef.setInput('raised', true);
      expect(classes()).toContain('shadow-sm');
    });

    it('buttonClass est reporté sur le <button>', () => {
      fixture.componentRef.setInput('buttonClass', 'w-100');
      expect(classes()).toContain('w-100');
    });
  });

  describe('contenu', () => {
    it('affiche le libellé', () => {
      fixture.componentRef.setInput('label', 'Enregistrer');
      expect(button().textContent?.trim()).toBe('Enregistrer');
    });

    it("affiche l'icône avant le libellé par défaut", () => {
      fixture.componentRef.setInput('label', 'Enregistrer');
      fixture.componentRef.setInput('icon', 'pi pi-save');
      const children = [...button().children].map(el => el.tagName);
      expect(children).toEqual(['I', 'SPAN']);
    });

    it('place l\'icône après le libellé quand iconPos vaut "right"', () => {
      fixture.componentRef.setInput('label', 'Suivant');
      fixture.componentRef.setInput('icon', 'pi pi-arrow-right');
      fixture.componentRef.setInput('iconPos', 'right');
      const children = [...button().children].map(el => el.tagName);
      expect(children).toEqual(['SPAN', 'I']);
    });

    it('iconOnly masque le libellé et ajoute la classe carrée', () => {
      fixture.componentRef.setInput('label', 'Supprimer');
      fixture.componentRef.setInput('icon', 'pi pi-trash');
      fixture.componentRef.setInput('iconOnly', true);
      expect(button().querySelector('span')).toBeNull();
      expect(classes()).toContain('app-btn-icon-only');
    });
  });

  describe('états', () => {
    it('loading remplace l\'icône par un spinner et désactive', () => {
      fixture.componentRef.setInput('icon', 'pi pi-save');
      fixture.componentRef.setInput('loading', true);
      expect(button().querySelector('.spinner-border')).not.toBeNull();
      expect(button().querySelector('i')).toBeNull();
      expect(button().disabled).toBe(true);
      expect(button().getAttribute('aria-busy')).toBe('true');
    });

    it('disabled désactive le bouton', () => {
      fixture.componentRef.setInput('disabled', true);
      expect(button().disabled).toBe(true);
    });

    it('type vaut "button" par défaut, pour ne pas soumettre les formulaires', () => {
      expect(button().type).toBe('button');
    });

    it('type est configurable', () => {
      fixture.componentRef.setInput('type', 'submit');
      expect(button().type).toBe('submit');
    });
  });

  describe('accessibilité', () => {
    it('ariaLabel est appliqué', () => {
      fixture.componentRef.setInput('ariaLabel', 'Fermer la fenêtre');
      expect(button().getAttribute('aria-label')).toBe('Fermer la fenêtre');
    });

    it('en iconOnly, le label sert de repli pour aria-label', () => {
      fixture.componentRef.setInput('label', 'Supprimer');
      fixture.componentRef.setInput('iconOnly', true);
      expect(button().getAttribute('aria-label')).toBe('Supprimer');
    });

    it("hors iconOnly, aucun aria-label n'est posé (le texte visible suffit)", () => {
      fixture.componentRef.setInput('label', 'Enregistrer');
      expect(button().getAttribute('aria-label')).toBeNull();
    });
  });

  describe('événements', () => {
    it('émet clicked au clic', () => {
      const spy = jest.fn();
      fixture.componentInstance.clicked.subscribe(spy);
      button().click();
      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy.mock.calls[0][0]).toBeInstanceOf(MouseEvent);
    });

    it("n'émet pas quand le bouton est désactivé", () => {
      const spy = jest.fn();
      fixture.componentInstance.clicked.subscribe(spy);
      fixture.componentRef.setInput('disabled', true);
      button().click();
      expect(spy).not.toHaveBeenCalled();
    });
  });
});
