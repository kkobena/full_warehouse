import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BadgeComponent } from './badge.component';

describe('BadgeComponent', () => {
  let fixture: ComponentFixture<BadgeComponent>;

  const host = (): HTMLElement => {
    fixture.detectChanges();
    return fixture.nativeElement;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [BadgeComponent] }).compileComponents();
    fixture = TestBed.createComponent(BadgeComponent);
  });

  it('affiche le libellé', () => {
    fixture.componentRef.setInput('label', 'Actif');
    expect(host().textContent?.trim()).toBe('Actif');
  });

  it('porte la classe de severity par défaut', () => {
    expect([...host().classList]).toEqual(expect.arrayContaining(['app-badge', 'app-badge--primary']));
  });

  it.each(['secondary', 'success', 'info', 'warn', 'danger', 'help', 'contrast'] as const)(
    'porte la classe pour la severity "%s"',
    severity => {
      fixture.componentRef.setInput('severity', severity);
      expect([...host().classList]).toContain(`app-badge--${severity}`);
    },
  );

  it('rounded ajoute la classe pilule', () => {
    fixture.componentRef.setInput('rounded', true);
    expect([...host().classList]).toContain('app-badge--rounded');
  });

  it("affiche l'icône quand elle est fournie", () => {
    fixture.componentRef.setInput('icon', 'pi pi-check');
    expect(host().querySelector('i.pi.pi-check')).not.toBeNull();
  });

  it("n'affiche pas de bouton de fermeture par défaut", () => {
    expect(host().querySelector('button')).toBeNull();
  });

  it('émet dismissed au clic sur la croix', () => {
    const spy = jest.fn();
    fixture.componentInstance.dismissed.subscribe(spy);
    fixture.componentRef.setInput('dismissible', true);
    host().querySelector('button')!.click();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('expose un aria-label sur la croix', () => {
    fixture.componentRef.setInput('dismissible', true);
    expect(host().querySelector('button')!.getAttribute('aria-label')).toBe('Retirer');
  });
});
