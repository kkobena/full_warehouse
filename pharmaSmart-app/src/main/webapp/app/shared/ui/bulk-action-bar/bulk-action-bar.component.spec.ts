import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { BulkActionBarComponent } from './bulk-action-bar.component';

@Component({
  standalone: true,
  imports: [BulkActionBarComponent],
  template: `
    <app-bulk-action-bar
      ariaLabel="Actions groupées"
      [count]="count()"
      label="produit sélectionné"
      labelPlural="produits sélectionnés"
    >
      <button type="button" (click)="actions.set(actions() + 1)">Appliquer</button>
    </app-bulk-action-bar>
  `,
})
class TestHostComponent {
  readonly count = signal(1);
  readonly actions = signal(0);
}

describe('BulkActionBarComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TestHostComponent] }).compileComponents();
  });

  it('accorde le libellé au singulier et au pluriel', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const count = () => (fixture.nativeElement as HTMLElement).querySelector('.app-bulk-action-bar__count')!.textContent!.trim();

    expect(count()).toBe('1 produit sélectionné');

    fixture.componentInstance.count.set(3);
    fixture.detectChanges();

    expect(count()).toBe('3 produits sélectionnés');
  });

  it('projette les actions sans intercepter leurs événements', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('button')!.click();

    expect(fixture.componentInstance.actions()).toBe(1);
  });

  it('expose une région nommée et un compteur annoncé', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const bar = host.querySelector('.app-bulk-action-bar')!;
    expect(bar.getAttribute('role')).toBe('region');
    expect(bar.getAttribute('aria-label')).toBe('Actions groupées');
    expect(host.querySelector('.app-bulk-action-bar__count')!.getAttribute('aria-live')).toBe('polite');
  });
});
