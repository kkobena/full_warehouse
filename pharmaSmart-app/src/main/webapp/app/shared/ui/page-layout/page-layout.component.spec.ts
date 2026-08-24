import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { PageLayoutComponent } from './page-layout.component';

@Component({
  standalone: true,
  imports: [PageLayoutComponent],
  template: `
    <app-page-layout
      [ariaLabel]="ariaLabel()"
      [density]="density()"
      [labelledBy]="labelledBy()"
    >
      <div ngProjectAs="[pageHeader]" data-testid="header">En-tête</div>
      <ng-container ngProjectAs="[pageGuidance]">
        <p data-testid="guidance-one">Conseil 1</p>
        <p data-testid="guidance-two">Conseil 2</p>
      </ng-container>
      @if (showContext()) {
        <div ngProjectAs="[pageContext]" data-testid="context">Contexte</div>
      }
      <button data-testid="body" type="button" (click)="clicks.set(clicks() + 1)">Contenu</button>
    </app-page-layout>
  `,
})
class TestHostComponent {
  readonly ariaLabel = signal('Page de test');
  readonly labelledBy = signal('');
  readonly density = signal<'compact' | 'default'>('default');
  readonly showContext = signal(true);
  readonly clicks = signal(0);
}

describe('PageLayoutComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TestHostComponent] }).compileComponents();
  });

  it('projette les zones dans l’ordre header, guidance, contexte et body', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const layout = (fixture.nativeElement as HTMLElement).querySelector('.app-page-layout')!;
    const zones = Array.from(layout.children).map(element => element.className);

    expect(zones).toEqual([
      'app-page-layout__header',
      'app-page-layout__guidance',
      'app-page-layout__context',
      'app-page-layout__body',
    ]);
    expect(layout.querySelectorAll('.app-page-layout__guidance p')).toHaveLength(2);
  });

  it('conserve les événements du contenu projeté', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-testid="body"]')!.click();

    expect(fixture.componentInstance.clicks()).toBe(1);
  });

  it('laisse la zone contextuelle vide lorsque sa condition devient fausse', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    fixture.componentInstance.showContext.set(false);
    fixture.detectChanges();

    const context = (fixture.nativeElement as HTMLElement).querySelector('.app-page-layout__context')!;
    expect(context.querySelector('[data-testid="context"]')).toBeNull();
    expect(context.childElementCount).toBe(0);
  });

  it('applique la densité compacte', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.density.set('compact');
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.app-page-layout')!.classList)
      .toContain('app-page-layout--compact');
  });

  it('rend une région nommée par aria-label', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const layout = (fixture.nativeElement as HTMLElement).querySelector('.app-page-layout')!;
    expect(layout.getAttribute('role')).toBe('region');
    expect(layout.getAttribute('aria-label')).toBe('Page de test');
  });

  it('privilégie aria-labelledby lorsque le heading est référençable', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.ariaLabel.set('');
    fixture.componentInstance.labelledBy.set('page-title');
    fixture.detectChanges();

    const layout = (fixture.nativeElement as HTMLElement).querySelector('.app-page-layout')!;
    expect(layout.getAttribute('role')).toBe('region');
    expect(layout.getAttribute('aria-labelledby')).toBe('page-title');
    expect(layout.hasAttribute('aria-label')).toBe(false);
  });

  it('ne crée ni région ni main sans nom accessible', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.ariaLabel.set('');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.app-page-layout')!.hasAttribute('role')).toBe(false);
    expect(host.querySelector('main')).toBeNull();
  });
});
