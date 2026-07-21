import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DataTableComponent } from './data-table.component';
import { RowTogglerDirective } from './row-toggler.directive';
import { SelectableRowDirective } from './selectable-row.directive';
import { SortableHeaderDirective } from './sortable-header.directive';
import { AppTableLazyLoadEvent } from './table.types';

interface Produit {
  id: number;
  libelle: string;
  prix: number;
}

const PRODUITS: Produit[] = [
  { id: 1, libelle: 'Zinc', prix: 300 },
  { id: 2, libelle: 'Élastoplaste', prix: 100 },
  { id: 3, libelle: 'Amoxicilline', prix: 200 },
];

@Component({
  imports: [DataTableComponent, SortableHeaderDirective, RowTogglerDirective, SelectableRowDirective],
  template: `
    <app-data-table
      [value]="produits()"
      [columns]="[]"
      [paginator]="paginator"
      [rows]="rows"
      [rowsPerPageOptions]="rowsPerPageOptions"
      [lazy]="lazy"
      [totalRecords]="totalRecords"
      [loading]="loading"
      [selectionMode]="selectionMode"
      [dataKey]="'id'"
      [stripedRows]="stripedRows"
      [scrollable]="scrollable"
      [scrollHeight]="scrollHeight"
      [showCurrentPageReport]="showCurrentPageReport"
      [(selection)]="selection"
      (onLazyLoad)="lazyEvents.push($event)"
      (onSort)="sortEvents.push($event)"
      (onPage)="pageEvents.push($event)"
    >
      <ng-template #header>
        <tr>
          <th appSortableHeader="libelle">Libellé</th>
          <th appSortableHeader="prix">Prix</th>
        </tr>
      </ng-template>

      <!-- Comme avec p-table, c'est le template qui possède son <tr>. -->
      <ng-template #body let-produit let-expanded="expanded">
        <tr [appSelectableRow]="produit">
          <td class="libelle">{{ produit.libelle }}</td>
          <td>
            <button type="button" class="toggler" [appRowToggler]="produit">{{ expanded ? 'moins' : 'plus' }}</button>
          </td>
        </tr>
      </ng-template>

      @if (withExpandedRow) {
        <ng-template #expandedrow let-produit>
          <tr class="app-table__row--expanded">
            <td class="detail" colspan="2">Détail {{ produit.libelle }}</td>
          </tr>
        </ng-template>
      }

      @if (withEmptyTemplate) {
        <ng-template #emptymessage>
          <tr>
            <td class="vide-perso">Rien à afficher ici</td>
          </tr>
        </ng-template>
      }
    </app-data-table>
  `,
})
class HostComponent {
  readonly produits = signal<Produit[]>(PRODUITS);
  paginator = false;
  rows = 10;
  rowsPerPageOptions: number[] = [];
  lazy = false;
  totalRecords: number | undefined = undefined;
  loading = false;
  selectionMode: 'single' | 'multiple' | null = null;
  stripedRows = false;
  scrollable = false;
  scrollHeight = '';
  showCurrentPageReport = false;
  withExpandedRow = false;
  withEmptyTemplate = false;
  selection: Produit | Produit[] | null = null;

  lazyEvents: AppTableLazyLoadEvent[] = [];
  sortEvents: { field: string; order: 1 | -1 }[] = [];
  pageEvents: { first: number; rows: number }[] = [];
}

describe('DataTableComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  const render = (): HTMLElement => {
    fixture.detectChanges();
    return fixture.nativeElement;
  };

  const labels = (): string[] => [...render().querySelectorAll('td.libelle')].map(td => td.textContent!.trim());

  const headers = (): HTMLElement[] => [...render().querySelectorAll('th')] as HTMLElement[];

  /**
   * Clique un numéro de page. `NgbPagination.pageChange` est un `EventEmitter`
   * **asynchrone** : il délivre sur une macrotâche, d'où l'attente explicite.
   */
  const goToPage = async (page: number): Promise<void> => {
    const link = [...render().querySelectorAll('.page-link')].find(el => el.textContent?.trim() === String(page));
    (link as HTMLElement).click();
    await new Promise(resolve => setTimeout(resolve, 0));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
  });

  describe('rendu', () => {
    it('projette le header et une ligne par élément', () => {
      expect(headers()).toHaveLength(2);
      expect(labels()).toEqual(['Zinc', 'Élastoplaste', 'Amoxicilline']);
    });

    it('rend une <table> Bootstrap', () => {
      expect([...render().querySelector('table')!.classList]).toEqual(expect.arrayContaining(['table', 'app-table']));
    });

    it('applique stripedRows', () => {
      host.stripedRows = true;
      expect([...render().querySelector('table')!.classList]).toContain('table-striped');
    });

    it('fige l\'en-tête et borne la hauteur en mode scrollable', () => {
      host.scrollable = true;
      host.scrollHeight = '400px';
      const scroller = render().querySelector('.app-table__scroller') as HTMLElement;
      expect([...scroller.classList]).toContain('app-table__scroller--scrollable');
      expect(scroller.style.maxHeight).toBe('400px');
      expect([...render().querySelector('thead')!.classList]).toContain('app-table__head--sticky');
    });

    it('affiche un voile de chargement', () => {
      host.loading = true;
      expect(render().querySelector('.app-table__loading')).not.toBeNull();
    });
  });

  describe('état vide', () => {
    beforeEach(() => host.produits.set([]));

    it('affiche le message par défaut', () => {
      expect(render().textContent).toContain('Aucune donnée');
    });

    it('préfère le template #emptymessage quand il est fourni', () => {
      host.withEmptyTemplate = true;
      expect(render().querySelector('.vide-perso')).not.toBeNull();
      expect(render().textContent).not.toContain('Aucune donnée');
    });

    it('ne montre pas le message vide pendant le chargement', () => {
      host.loading = true;
      expect(render().textContent).not.toContain('Aucune donnée');
    });
  });

  describe('tri client-side', () => {
    it('trie à l\'ascendant au premier clic', () => {
      headers()[0].click();
      expect(labels()).toEqual(['Amoxicilline', 'Élastoplaste', 'Zinc']);
    });

    it('inverse le sens au second clic sur la même colonne', () => {
      headers()[0].click();
      headers()[0].click();
      expect(labels()).toEqual(['Zinc', 'Élastoplaste', 'Amoxicilline']);
    });

    it('repart à l\'ascendant en changeant de colonne', () => {
      headers()[0].click();
      headers()[0].click();
      headers()[1].click();
      expect(labels()).toEqual(['Élastoplaste', 'Amoxicilline', 'Zinc']);
      expect(host.sortEvents.at(-1)).toEqual({ field: 'prix', order: 1 });
    });

    it('classe les accents correctement (comparaison française)', () => {
      // Avec un tri binaire, « Élastoplaste » se retrouverait après « Zinc ».
      headers()[0].click();
      expect(labels()[1]).toBe('Élastoplaste');
    });

    it('renseigne aria-sort sur la colonne triée', () => {
      headers()[0].click();
      fixture.detectChanges();
      expect(headers()[0].getAttribute('aria-sort')).toBe('ascending');
      expect(headers()[1].getAttribute('aria-sort')).toBe('none');
    });

    it('se déclenche aussi au clavier', () => {
      headers()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
      expect(labels()[0]).toBe('Amoxicilline');
    });
  });

  describe('pagination client-side', () => {
    beforeEach(() => {
      host.paginator = true;
      host.rows = 2;
    });

    it('ne montre que la première page', () => {
      expect(labels()).toEqual(['Zinc', 'Élastoplaste']);
    });

    it('navigue vers la page suivante', async () => {
      await goToPage(2);
      expect(labels()).toEqual(['Amoxicilline']);
      expect(host.pageEvents.at(-1)).toEqual({ first: 2, rows: 2 });
    });

    it('affiche le compteur au format p-table', () => {
      host.showCurrentPageReport = true;
      expect(render().textContent).toContain('Affichage de 1 à 2 sur 3 entrées');
    });

    it('propose le sélecteur de taille de page', () => {
      host.rowsPerPageOptions = [2, 5];
      const select = render().querySelector('.app-table__page-size') as HTMLSelectElement;
      expect(select.options).toHaveLength(2);

      select.value = '5';
      select.dispatchEvent(new Event('change'));
      expect(labels()).toHaveLength(3);
      expect(host.pageEvents.at(-1)).toEqual({ first: 0, rows: 5 });
    });

    it('revient en première page après un tri', async () => {
      await goToPage(2);
      headers()[0].click();
      expect(labels()).toEqual(['Amoxicilline', 'Élastoplaste']);
    });
  });

  describe('mode lazy', () => {
    beforeEach(() => {
      host.lazy = true;
      host.paginator = true;
      host.rows = 2;
      host.totalRecords = 30;
    });

    it('affiche les lignes telles quelles, sans repaginer', () => {
      expect(labels()).toHaveLength(3);
    });

    it('ne retrie pas localement — le serveur fait foi', () => {
      headers()[0].click();
      expect(labels()).toEqual(['Zinc', 'Élastoplaste', 'Amoxicilline']);
    });

    it('émet onLazyLoad au changement de page, au format p-table', async () => {
      await goToPage(2);
      expect(host.lazyEvents.at(-1)).toEqual({ first: 2, rows: 2, sortField: undefined, sortOrder: 1 });
    });

    it('émet onLazyLoad avec le tri demandé', () => {
      headers()[1].click();
      expect(host.lazyEvents.at(-1)).toEqual({ first: 0, rows: 2, sortField: 'prix', sortOrder: 1 });
    });

    it('appuie la pagination sur totalRecords', () => {
      // 30 enregistrements / 2 par page = 15 pages, malgré les 3 lignes présentes.
      expect(render().textContent).toContain('15');
    });

    it('n\'émet pas onLazyLoad hors mode lazy', () => {
      host.lazy = false;
      headers()[0].click();
      expect(host.lazyEvents).toHaveLength(0);
    });
  });

  describe('sélection', () => {
    // On cible les lignes du corps par leur cellule libellé : en dépliage, des <tr>
    // supplémentaires s'intercalent et fausseraient un simple index sur `tbody tr`.
    const rowAt = (index: number): HTMLElement =>
      ([...render().querySelectorAll('td.libelle')][index] as HTMLElement).closest('tr')!;

    it('ne rend qu\'une ligne par élément', () => {
      // Garde-fou : envelopper le <tr> projeté dans un <tr> de la table dupliquerait tout.
      expect(render().querySelectorAll('tbody tr')).toHaveLength(PRODUITS.length);
    });

    it('ne sélectionne rien sans selectionMode', () => {
      rowAt(0).click();
      expect(host.selection).toBeNull();
    });

    it('sélectionne une ligne en mode single', () => {
      host.selectionMode = 'single';
      rowAt(0).click();
      expect(host.selection).toEqual(PRODUITS[0]);
      expect([...rowAt(0).classList]).toContain('table-active');
    });

    it('déselectionne au second clic en mode single', () => {
      host.selectionMode = 'single';
      rowAt(0).click();
      fixture.detectChanges();
      rowAt(0).click();
      expect(host.selection).toBeNull();
    });

    it('accumule puis retire en mode multiple', () => {
      host.selectionMode = 'multiple';
      rowAt(0).click();
      fixture.detectChanges();
      rowAt(2).click();
      expect(host.selection).toEqual([PRODUITS[0], PRODUITS[2]]);

      fixture.detectChanges();
      rowAt(0).click();
      expect(host.selection).toEqual([PRODUITS[2]]);
    });
  });

  describe('dépliage de ligne', () => {
    beforeEach(() => (host.withExpandedRow = true));

    const toggler = (index: number): HTMLElement => [...render().querySelectorAll('.toggler')][index] as HTMLElement;

    it('ne déplie rien au départ', () => {
      expect(render().querySelector('.detail')).toBeNull();
    });

    it('déplie puis replie la ligne', () => {
      toggler(0).click();
      expect(render().querySelector('.detail')!.textContent).toContain('Détail Zinc');

      toggler(0).click();
      expect(render().querySelector('.detail')).toBeNull();
    });

    it('expose l\'état déplié au template #body', () => {
      expect(toggler(0).textContent!.trim()).toBe('plus');
      toggler(0).click();
      expect(toggler(0).textContent!.trim()).toBe('moins');
    });

    it('permet plusieurs lignes dépliées simultanément', () => {
      toggler(0).click();
      toggler(1).click();
      expect(render().querySelectorAll('.detail')).toHaveLength(2);
    });

    it('ne déclenche pas la sélection de la ligne', () => {
      host.selectionMode = 'single';
      toggler(0).click();
      expect(host.selection).toBeNull();
    });
  });
});
