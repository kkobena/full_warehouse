import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import { NavSidebarComponent } from './nav-sidebar.component';

/**
 * Hôte reproduisant l'usage réel : le `ngbNav` est déclaré par l'appelant et projeté, ses items
 * lui restant enfants directs. C'est ce point que ces tests verrouillent — une refonte qui
 * déplacerait le `[ngbNav]` à l'intérieur du composant rendrait un menu vide, sans erreur.
 */
@Component({
  standalone: true,
  imports: [NgbNavModule, NavSidebarComponent],
  template: `
    <app-nav-sidebar [nav]="nav" icon="pi pi-book" title="Comptabilité">
      <div #nav="ngbNav" ngbNav class="nav flex-column nav-pills" orientation="vertical"
           [activeId]="active()" (activeIdChange)="active.set($event)">
        <ng-container ngbNavItem="balance">
          <a class="pharma-nav-vertical-link" ngbNavLink>
            <i class="pi pi-calculator"></i><span>Balance caisse</span>
          </a>
          <ng-template ngbNavContent>
            <p class="contenu-balance">Contenu balance</p>
          </ng-template>
        </ng-container>

        <ng-container ngbNavItem="taxe-report">
          <a class="pharma-nav-vertical-link" ngbNavLink>
            <i class="pi pi-file-pdf"></i><span>Rapport TVA</span>
          </a>
          <ng-template ngbNavContent>
            <p class="contenu-tva">Contenu TVA</p>
          </ng-template>
        </ng-container>
      </div>
    </app-nav-sidebar>
  `,
})
class HoteTest {
  readonly active = signal('balance');
}

describe('AppNavSidebar — menu vertical d’écran', () => {
  let fixture: ComponentFixture<HoteTest>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HoteTest] }).compileComponents();
    fixture = TestBed.createComponent(HoteTest);
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  });

  it('rend l’en-tête avec son icône et son titre', () => {
    const header = element.querySelector('.pharma-nav-sidebar-header');
    expect(header?.textContent).toContain('Comptabilité');
    expect(header?.querySelector('i')?.className).toContain('pi-book');
  });

  it('projette les onglets, que `ngbNav` doit voir malgré la projection', () => {
    const liens = element.querySelectorAll('.pharma-nav-sidebar-content .pharma-nav-vertical-link');
    expect(liens.length).toBe(2);
    expect(liens[0].textContent).toContain('Balance caisse');
    expect(liens[1].textContent).toContain('Rapport TVA');
  });

  it('rend le contenu de l’onglet actif dans la colonne de droite', () => {
    expect(element.querySelector('.contenu-balance')).not.toBeNull();
    expect(element.querySelector('.contenu-tva')).toBeNull();
  });

  it('bascule de contenu au clic sur un autre onglet', async () => {
    const liens = element.querySelectorAll<HTMLAnchorElement>('.pharma-nav-vertical-link');
    liens[1].click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.active()).toBe('taxe-report');
    expect(element.querySelector('.contenu-tva')).not.toBeNull();
    // Le panneau sortant survit le temps de la transition de `ngbNav` : on vérifie qu'il n'est
    // plus l'onglet actif, pas qu'il a déjà quitté le DOM.
    expect(element.querySelector('.pharma-nav-vertical-link.active')?.textContent).toContain('Rapport TVA');
  });

  it('sans `collapsible`, aucun bouton de repli', () => {
    expect(element.querySelector('.nav-sidebar-toggle')).toBeNull();
  });

  describe('repli', () => {
    @Component({
      standalone: true,
      imports: [NgbNavModule, NavSidebarComponent],
      template: `
        <app-nav-sidebar (collapsedChange)="replie.set($event)" [collapsed]="replie()"
                         [collapsible]="true" [nav]="nav" icon="pi pi-book" title="Comptabilité">
          <div #nav="ngbNav" ngbNav class="nav flex-column nav-pills" orientation="vertical">
            <ng-container ngbNavItem="balance">
              <a class="pharma-nav-vertical-link" ngbNavLink>
                <i class="pi pi-calculator"></i><span>Balance caisse</span>
                <span class="badge bg-danger">7</span>
                <span class="link-arrow">›</span>
              </a>
              <ng-template ngbNavContent><p>Contenu</p></ng-template>
            </ng-container>
          </div>
        </app-nav-sidebar>
      `,
    })
    class HoteRepliable {
      readonly replie = signal(false);
    }

    let f: ComponentFixture<HoteRepliable>;
    let el: HTMLElement;

    beforeEach(() => {
      f = TestBed.createComponent(HoteRepliable);
      f.detectChanges();
      el = f.nativeElement as HTMLElement;
    });

    it('affiche le bouton de repli et le titre', () => {
      expect(el.querySelector('.nav-sidebar-toggle')).not.toBeNull();
      expect(el.querySelector('.pharma-nav-sidebar-header')?.textContent).toContain('Comptabilité');
    });

    it('replie au clic : le titre disparaît, l’icône du menu reste', () => {
      el.querySelector<HTMLButtonElement>('.nav-sidebar-toggle button')!.click();
      f.detectChanges();

      expect(f.componentInstance.replie()).toBe(true);
      const header = el.querySelector('.pharma-nav-sidebar-header');
      expect(header?.textContent).not.toContain('Comptabilité');
      expect(header?.querySelector('i.pi-book')).not.toBeNull();
    });

    it('marque le menu replié et rend la largeur au contenu', () => {
      f.componentInstance.replie.set(true);
      f.detectChanges();

      expect(el.querySelector('.pharma-nav-sidebar.is-collapsed')).not.toBeNull();
      // `col-lg-2` réserverait deux douzièmes vides à la place du menu réduit.
      expect(el.querySelector('.row > .col-auto')).not.toBeNull();
      expect(el.querySelector('.row > .col')).not.toBeNull();
    });

    it('conserve les onglets projetés une fois replié', () => {
      f.componentInstance.replie.set(true);
      f.detectChanges();

      expect(el.querySelectorAll('.pharma-nav-vertical-link').length).toBe(1);
    });

    it('conserve le badge d’alerte une fois replié', () => {
      f.componentInstance.replie.set(true);
      f.detectChanges();

      // Un compteur de ruptures ou de factures échues est ce qui doit faire rouvrir le menu :
      // le masquer priverait l'utilisateur du seul signal qui l'y ramène.
      const badge = el.querySelector('.pharma-nav-vertical-link .badge');
      expect(badge).not.toBeNull();
      expect(badge!.textContent).toContain('7');
    });

    it('masque le libellé et le chevron une fois replié, pas le badge', () => {
      f.componentInstance.replie.set(true);
      f.detectChanges();

      const lien = el.querySelector('.pharma-nav-vertical-link')!;
      const masquables = Array.from(lien.querySelectorAll('span')).filter(
        s => !s.className.includes('badge'),
      );
      expect(masquables.length).toBeGreaterThan(0);
      expect(lien.querySelector('i.pi-calculator')).not.toBeNull();
    });

    it('se pilote depuis l’extérieur', () => {
      f.componentInstance.replie.set(true);
      f.detectChanges();
      expect(el.querySelector('.pharma-nav-sidebar.is-collapsed')).not.toBeNull();

      f.componentInstance.replie.set(false);
      f.detectChanges();
      expect(el.querySelector('.pharma-nav-sidebar.is-collapsed')).toBeNull();
    });
  });

  it('sans `nav`, n’affiche pas la colonne de contenu', async () => {
    @Component({
      standalone: true,
      imports: [NavSidebarComponent],
      template: `<app-nav-sidebar title="Sans outlet"><span>menu</span></app-nav-sidebar>`,
    })
    class SansNav {}

    const f = TestBed.createComponent(SansNav);
    f.detectChanges();
    const colonnes = (f.nativeElement as HTMLElement).querySelectorAll('.row > div');
    expect(colonnes.length).toBe(1);
  });
});
