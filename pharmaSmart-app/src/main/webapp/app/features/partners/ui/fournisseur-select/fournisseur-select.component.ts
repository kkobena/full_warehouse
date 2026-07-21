import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  OnInit,
  output,
  signal
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule} from '@angular/forms';
import {map} from 'rxjs/operators';
import {IFournisseur} from '../../../../shared/model/fournisseur.model';
import {FournisseurApiService} from '../../data-access/services/fournisseur-api.service';
import {
  FloatLabelComponent,
  MultiSelectComponent,
  SelectSearchComponent
} from '../../../../shared/ui';

interface IFournisseurOption extends IFournisseur {
  groupLabel: string;
}

@Component({
  selector: 'app-fournisseur-select',
  templateUrl: './fournisseur-select.component.html',
  styleUrls: ['./fournisseur-select.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, SelectSearchComponent, MultiSelectComponent, FloatLabelComponent],
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


  readonly fournisseurs = signal<IFournisseur[]>([]);
  readonly groupedFournisseurs = signal<IFournisseurOption[]>([]);

  selectedSingle: IFournisseur | null = null;
  selectedMultiple: IFournisseur[] = [];

  private readonly api = inject(FournisseurApiService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const id = this.selectedId();
      const list = this.fournisseurs();
      if (id == null) {
        this.selectedSingle = null;
        return;
      }
      const found = list.find(f => f.id === id) ?? null;
      // En mode groupé, un principal qui possède des agences est un en-tête de groupe
      // (non sélectionnable) — ne pas le pré-sélectionner pour éviter l'incohérence UI.
      if (found && !found.parentId && this.grouped() && list.some(f => f.parentId === found.id)) {
        this.selectedSingle = null;
        return;
      }
      this.selectedSingle = found;
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

  /** Enrichit la clé de groupe (`groupLabel`) avec `hasChildren`, pour choisir l'icône du groupe. */
  protected groupValueFn = (key: unknown, children: unknown[]): {
    label: string;
    hasChildren: boolean
  } => ({
    label: String(key ?? ''),
    hasChildren: children.some(c => (c as IFournisseurOption).parentId != null),
  });

  private load(): void {
    const req$ = this.parentsOnly()
      ? this.api.queryParents({page: 0, size: 999})
      : this.api.query({page: 0, size: 999});

    req$.pipe(
      takeUntilDestroyed(this.destroyRef),
      map(res => res.body ?? []),
    ).subscribe(list => {
      this.fournisseurs.set(list);
      if (this.grouped()) {
        this.groupedFournisseurs.set(this.buildGroupedList(list));
      }
    });
  }

  private buildGroupedList(list: IFournisseur[]): IFournisseurOption[] {
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

    return principals.flatMap(p => {
      const children = agencesByParent.get(p.id!) ?? [];
      const groupLabel = p.libelle ?? '';
      // Avec agences : le principal est l'en-tête, les agences sont les items (pas de doublon).
      // Sans agences  : le principal est son propre item.
      return (children.length > 0 ? children : [p]).map(f => ({...f, groupLabel}));
    });
  }
}
