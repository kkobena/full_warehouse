import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DetailFieldComponent } from './detail-field.component';
import { DetailSectionComponent } from './detail-section.component';

@Component({
  standalone: true,
  imports: [DetailSectionComponent, DetailFieldComponent],
  template: `
    <app-detail-section header="Prix" storageKey="test.prix">
      <app-detail-field label="Vide" [value]="vide()" unit="u" />
      <app-detail-field label="CMM" [value]="1234.56" format="nombre" [decimals]="1" unit="u/m" />
      <app-detail-field label="Libellé" value="Doliprane 1000 mg" />
      <app-detail-field label="Riche"><b class="projete">ok</b></app-detail-field>
    </app-detail-section>
  `,
})
class HoteTest {
  readonly vide = signal<number | null>(null);
}

const valeurs = (el: HTMLElement): string[] =>
  Array.from(el.querySelectorAll('.detail-field-value')).map(v => (v.textContent ?? '').replace(/\s+/g, ' ').trim());

describe('AppDetailSection / AppDetailField', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({ imports: [HoteTest] }).compileComponents();
  });

  it('affiche — sans unité pour une valeur absente, et formate les nombres', () => {
    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();
    const [vide, cmm] = valeurs(f.nativeElement);
    expect(vide).toBe('—');
    expect(cmm).toMatch(/^1\s?234,6 u\/m$/);
  });

  it('pose le texte entier en title pour lire un libellé tronqué', () => {
    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();
    const libelle = f.nativeElement.querySelectorAll('.detail-field-value')[2] as HTMLElement;
    expect(libelle.getAttribute('title')).toBe('Doliprane 1000 mg');
  });

  it('remplace la valeur par le contenu projeté', () => {
    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();
    expect(f.nativeElement.querySelector('.detail-field-value .projete')).not.toBeNull();
  });

  it('replie la section et mémorise l’état pour la prochaine ouverture', () => {
    const f = TestBed.createComponent(HoteTest);
    f.detectChanges();
    (f.nativeElement.querySelector('.detail-section-toggle') as HTMLButtonElement).click();
    f.detectChanges();
    expect(f.nativeElement.querySelector('.detail-section-body')).toBeNull();
    expect(localStorage.getItem('detail-section:test.prix')).toBe('true');

    const g = TestBed.createComponent(HoteTest);
    g.detectChanges();
    expect(g.nativeElement.querySelector('.detail-section-body')).toBeNull();
  });
});
