import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalDeplacableDirective } from './modal-deplacable.directive';

@Component({
  imports: [ModalDeplacableDirective],
  template: `
    <div class="modal-dialog">
      <div class="modal-header" appModalDeplacable>
        <h5 class="titre">Titre</h5>
        <button class="btn-close" type="button"></button>
      </div>
    </div>
  `,
})
class HoteComponent {}

/** Fenêtre modale déplaçable par son en-tête, sans jamais sortir de l'écran. */
describe('ModalDeplacableDirective', () => {
  let fixture: ComponentFixture<HoteComponent>;
  let fenetre: HTMLElement;
  let entete: HTMLElement;

  const pointeur = (type: string, x: number, y: number, cible: EventTarget = document, button = 0) => {
    const e = new MouseEvent(type, { clientX: x, clientY: y, button, bubbles: true, cancelable: true });
    cible.dispatchEvent(e);
    return e;
  };

  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1000 });
    Object.defineProperty(window, 'innerHeight', { configurable: true, value: 800 });
    fixture = TestBed.createComponent(HoteComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    fenetre = el.querySelector('.modal-dialog')!;
    entete = el.querySelector('.modal-header')!;
    // Fenêtre de 400×300 posée en (300, 200) : jsdom ne calcule aucune mise en page.
    fenetre.getBoundingClientRect = () => ({ left: 300, right: 700, top: 200, bottom: 500, width: 400, height: 300, x: 300, y: 200, toJSON: () => ({}) });
  });

  it('suit la souris tant que le bouton est enfoncé, puis s\'arrête', () => {
    pointeur('pointerdown', 500, 220, entete);
    pointeur('pointermove', 560, 250);
    expect(fenetre.style.transform).toBe('translate(60px, 30px)');

    pointeur('pointerup', 560, 250);
    pointeur('pointermove', 900, 700);
    expect(fenetre.style.transform).toBe('translate(60px, 30px)');
  });

  it('cumule les déplacements successifs', () => {
    pointeur('pointerdown', 500, 220, entete);
    pointeur('pointermove', 540, 240);
    pointeur('pointerup', 540, 240);
    fenetre.getBoundingClientRect = () => ({ left: 340, right: 740, top: 220, bottom: 520, width: 400, height: 300, x: 340, y: 220, toJSON: () => ({}) });

    pointeur('pointerdown', 540, 240, entete);
    pointeur('pointermove', 550, 250);

    expect(fenetre.style.transform).toBe('translate(50px, 30px)');
  });

  it("garde l'en-tête dans l'écran : jamais au-dessus du bord haut, au moins 48 px visibles ailleurs", () => {
    pointeur('pointerdown', 500, 220, entete);

    pointeur('pointermove', 500, -500);
    expect(fenetre.style.transform).toBe('translate(0px, -200px)');

    pointeur('pointermove', 5000, 5000);
    expect(fenetre.style.transform).toBe('translate(652px, 552px)');

    pointeur('pointermove', -5000, 220);
    expect(fenetre.style.transform).toBe('translate(-652px, 0px)');
  });

  it('ne se déplace pas quand on clique sur « Fermer », ni avec un autre bouton que le principal', () => {
    pointeur('pointerdown', 500, 220, fixture.nativeElement.querySelector('.btn-close'));
    pointeur('pointermove', 600, 300);
    expect(fenetre.style.transform).toBe('');

    pointeur('pointerdown', 500, 220, entete, 2);
    pointeur('pointermove', 600, 300);
    expect(fenetre.style.transform).toBe('');
  });

  it("n'écoute plus rien une fois détruite", () => {
    pointeur('pointerdown', 500, 220, entete);
    fixture.destroy();
    pointeur('pointermove', 600, 300);

    expect(fenetre.style.transform).toBe('');
  });
});
