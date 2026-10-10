import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AncresSectionsComponent } from './ancres-sections.component';

@Component({
  imports: [AncresSectionsComponent],
  template: `
    <div class="zone" style="overflow-y: auto; height: 100px">
      <div>
        <app-ancres-sections [ancres]="ancres" />
        <h3 id="essai-a">A</h3>
        <h3 id="essai-b">B</h3>
      </div>
    </div>
  `,
})
class Hote {
  readonly ancres = [
    { id: 'essai-a', libelle: 'Section A' },
    { id: 'essai-b', libelle: 'Section B' },
  ];
}

describe('Ancres des sections du pilotage', () => {
  beforeEach(() => {
    localStorage.clear();
    Element.prototype.scrollIntoView = jest.fn();
  });

  it('retient la section choisie, par onglet', () => {
    const fixture = TestBed.createComponent(Hote);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.ancre-lien')[1].click();
    fixture.detectChanges();

    expect(localStorage.getItem('pilotage.section.essai-a')).toBe('essai-b');
    expect((fixture.nativeElement as HTMLElement).querySelector('.ancre-lien.active')?.textContent?.trim()).toBe('Section B');
  });

  it('au retour, revient à la section mémorisée', async () => {
    localStorage.setItem('pilotage.section.essai-a', 'essai-b');
    const fixture = TestBed.createComponent(Hote);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(Element.prototype.scrollIntoView).toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).querySelector('.ancre-lien.active')?.textContent?.trim()).toBe('Section B');
  });
});
