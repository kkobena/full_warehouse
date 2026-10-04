import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppSubtab, SubtabBarComponent } from './subtab-bar.component';

@Component({
  standalone: true,
  imports: [SubtabBarComponent],
  template: `<app-subtab-bar ariaLabel="Sources" [tabs]="onglets()" [active]="actif()" (activeChange)="actif.set($event)" />`,
})
class Hote {
  readonly onglets = signal<AppSubtab[]>([
    { id: 'a', label: 'Propositions', icon: 'pi pi-inbox', badge: 3, badgeSeverity: 'primary' },
    { id: 'b', label: 'Commandes', badge: 0, badgeSeverity: 'success' },
    { id: 'c', label: 'Réceptions', badge: null },
  ]);
  readonly actif = signal('a');
}

describe('SubtabBarComponent', () => {
  let fixture: ComponentFixture<Hote>;
  let element: HTMLElement;
  const onglets = (): HTMLButtonElement[] => Array.from(element.querySelectorAll<HTMLButtonElement>('[role="tab"]'));

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Hote] }).compileComponents();
    fixture = TestBed.createComponent(Hote);
    fixture.detectChanges();
    element = fixture.nativeElement;
  });

  it('expose une liste d’onglets nommée et un onglet sélectionné', () => {
    expect(element.querySelector('[role="tablist"]')?.getAttribute('aria-label')).toBe('Sources');
    expect(onglets().map(o => o.getAttribute('aria-selected'))).toEqual(['true', 'false', 'false']);
  });

  it('n’a qu’un onglet dans l’ordre de tabulation (tabulation itinérante)', () => {
    expect(onglets().map(o => o.getAttribute('tabindex'))).toEqual(['0', '-1', '-1']);
  });

  it('le premier onglet est focalisable tant qu’aucun n’est actif', () => {
    fixture.componentInstance.actif.set('inconnu');
    fixture.detectChanges();
    expect(onglets().map(o => o.getAttribute('tabindex'))).toEqual(['0', '-1', '-1']);
  });

  it('un clic active l’onglet et émet activeChange', () => {
    onglets()[1].click();
    fixture.detectChanges();
    expect(fixture.componentInstance.actif()).toBe('b');
    expect(onglets()[1].getAttribute('aria-selected')).toBe('true');
    expect(onglets()[1].classList).toContain('app-subtab--active');
  });

  it('affiche la pastille d’un compteur à 0, mais pas celle d’un compteur nul', () => {
    const badges = Array.from(element.querySelectorAll('.app-subtab__badge')).map(b => b.textContent?.trim());
    expect(badges).toEqual(['3', '0']);
    expect(element.querySelector('.app-subtab__badge--success')).not.toBeNull();
  });

  it('les flèches déplacent la sélection et le focus, en boucle', () => {
    const liste = element.querySelector('[role="tablist"]') as HTMLElement;
    liste.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.actif()).toBe('b');
    expect(document.activeElement).toBe(onglets()[1]);

    liste.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    liste.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.actif()).toBe('c');
  });

  it('Début et Fin vont au premier et au dernier onglet', () => {
    const liste = element.querySelector('[role="tablist"]') as HTMLElement;
    liste.dispatchEvent(new KeyboardEvent('keydown', { key: 'End', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.actif()).toBe('c');
    liste.dispatchEvent(new KeyboardEvent('keydown', { key: 'Home', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.actif()).toBe('a');
  });

  it('ignore les autres touches', () => {
    const liste = element.querySelector('[role="tablist"]') as HTMLElement;
    const evenement = new KeyboardEvent('keydown', { key: 'a', bubbles: true, cancelable: true });
    liste.dispatchEvent(evenement);
    expect(evenement.defaultPrevented).toBe(false);
    expect(fixture.componentInstance.actif()).toBe('a');
  });
});
