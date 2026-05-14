import { Component, DestroyRef, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { map } from 'rxjs/operators';
import { Select } from 'primeng/select';
import { MultiSelect } from 'primeng/multiselect';
import { FloatLabel } from 'primeng/floatlabel';
import { IFournisseur } from '../../../../shared/model/fournisseur.model';
import { FournisseurApiService } from '../../data-access/services/fournisseur-api.service';

interface FournisseurGroup {
  label: string;
  items: IFournisseur[];
  /** true = fournisseur principal avec agences → icône hiérarchie. false = fournisseur seul → icône bâtiment. */
  hasChildren: boolean;
}

@Component({
  selector: 'app-fournisseur-select',
  templateUrl: './fournisseur-select.component.html',
  styleUrls: ['./fournisseur-select.component.scss'],
  imports: [FormsModule, Select, MultiSelect, FloatLabel],
})
export class FournisseurSelectComponent implements OnInit {
  /** Pré-sélectionne un fournisseur par son id (single mode). Réactif via effect(). */
  selectedId = input<number | null>(null);
  /** Active la sélection multiple (p-multiselect). */
  multiple = input<boolean>(false);
  /** true → /fournisseurs/parents, false → /fournisseurs (tous). */
  parentsOnly = input<boolean>(false);
  /** Groupe les items par parentId (SelectItemGroup). Ignoré si parentsOnly=true et liste plate. */
  grouped = input<boolean>(false);
  placeholder = input<string>('Fournisseur');
  disabled = input<boolean>(false);
  showClear = input<boolean>(true);
  /**
   * Cible d'accrochage de l'overlay PrimeNG.
   * Par défaut 'body' (hors modal). Passer null dans une modal et gérer
   * (dropdownShow)/(dropdownHide) pour rendre le modal-body overflow:visible.
   */
  appendTo = input<string | null>('body');

  /** Émis en mode sélection unique. */
  selectionChange = output<IFournisseur | null>();
  /** Émis en mode sélection multiple. */
  multiSelectionChange = output<IFournisseur[]>();
  /** Bubble des événements PrimeNG onShow/onHide — utile dans les modals. */
  dropdownShow = output<unknown>();
  dropdownHide = output<unknown>();

  readonly fournisseurs = signal<IFournisseur[]>([]);
  readonly groups = signal<FournisseurGroup[]>([]);

  selectedSingle: IFournisseur | null = null;
  selectedMultiple: IFournisseur[] = [];

  private readonly api = inject(FournisseurApiService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const id = this.selectedId();
      const list = this.fournisseurs();
      this.selectedSingle = id != null ? (list.find(f => f.id === id) ?? null) : null;
    });
  }

  ngOnInit(): void {
    this.load();
  }

  onSingleChange(value: IFournisseur | null): void {
    this.selectionChange.emit(value ?? null);
  }

  onMultiChange(values: IFournisseur[]): void {
    this.multiSelectionChange.emit(values ?? []);
  }

  reset(): void {
    this.selectedSingle = null;
    this.selectedMultiple = [];
    this.selectionChange.emit(null);
    this.multiSelectionChange.emit([]);
  }

  private load(): void {
    const req$ = this.parentsOnly()
      ? this.api.queryParents({page:0,size:999})
      : this.api.query({page:0,size:999});

    req$.pipe(
      takeUntilDestroyed(this.destroyRef),
      map(res => res.body ?? []),
    ).subscribe(list => {
      this.fournisseurs.set(list);
      if (this.grouped()) {
        this.groups.set(this.buildGroups(list));
      }
    });
  }

  private buildGroups(list: IFournisseur[]): FournisseurGroup[] {
    const agences = list.filter(f => f.parentId != null);
    const principals = list.filter(f => f.parentId == null);

    // Indexer les agences par parentId
    const agencesByParent = new Map<number, IFournisseur[]>();
    for (const a of agences) {
      if (!agencesByParent.has(a.parentId!)) {
        agencesByParent.set(a.parentId!, []);
      }
      agencesByParent.get(a.parentId!)!.push(a);
    }

    return principals.map(p => {
      const children = agencesByParent.get(p.id!) ?? [];
      return {
        label: p.libelle ?? '',
        hasChildren: children.length > 0,
        // Avec agences : le principal est l'en-tête, les agences sont les items (pas de doublon).
        // Sans agences  : le principal est son propre item.
        items: children.length > 0 ? children : [p],
      };
    });
  }
}
