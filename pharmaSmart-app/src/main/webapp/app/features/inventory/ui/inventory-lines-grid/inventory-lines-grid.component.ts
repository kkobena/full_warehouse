import {Component, computed, effect, inject, input, output, ChangeDetectionStrategy, signal } from '@angular/core';
import {CommonModule, formatDate} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {
  AllCommunityModule,
  CellEditingStartedEvent,
  CellValueChangedEvent,
  ClientSideRowModelModule,
  ColDef,
  GetRowIdParams,
  GridApi,
  GridReadyEvent,
  IRowNode,
  ModuleRegistry,
  RowClickedEvent,
  themeAlpine,
} from 'ag-grid-community';
import {AgGridAngular} from 'ag-grid-angular';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {InventoryEditorFacade} from '../../data-access/facades/inventory-editor.facade';
import {InventoryStore} from '../../data-access/store/inventory.store';
import {
  IInventoryLine,
  InventoryLineFilter,
  isGapLineFilter,
  lineFiltersFor,
  renderParetoBadge,
} from '../../models';
import {IStorage} from '../../../../shared/model/magasin.model';
import {IRayon} from '../../../../shared/model/rayon.model';
import {InventoryCategoryType} from "../../../../shared/model/store-inventory.model";
import {SelectComponent} from '../../../../shared/ui';
import {AG_GRID_LOCALE_FR} from '../../../../shared/ui/ag-grid/ag-grid-locale.fr';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: 'app-inventory-lines-grid',
  imports: [CommonModule, FormsModule, AgGridAngular, SelectComponent, NgbTooltip],
  templateUrl: './inventory-lines-grid.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './inventory-lines-grid.component.scss',
})
export class InventoryLinesGridComponent {
  /** Libellés français d'AG Grid — voir shared/ui/ag-grid. */
  protected readonly localeFr = AG_GRID_LOCALE_FR;

  readonly inventoryId = input.required<number>();
  readonly inventoryCategoryType = input<InventoryCategoryType>();
  readonly blindMode = input<boolean>(false);
  readonly showSeuilMini = input<boolean>(false);
  readonly gestionLot = input<boolean>(false);
  readonly storages = input<IStorage[]>([]);
  readonly rayons = input<IRayon[]>([]);
  /** Inventaire clôturé : la grille se consulte, elle ne se saisit plus. */
  readonly readOnly = input<boolean>(false);
  readonly pageSize = input<number>(20);

  readonly filterChange = output<{
    lineFilter: InventoryLineFilter;
    storageId: number | null;
    rayonId: number | null;
    search: string;
  }>();
  readonly storageChange = output<number | null>();
  readonly nextPage = output<void>();
  readonly lineSelected = output<IInventoryLine | null>();

  readonly editorFacade = inject(InventoryEditorFacade);
  readonly store = inject(InventoryStore);

  /** Filtres d'écart retirés en mode aveugle (privilège pr-voir-stock-inventaire) */
  readonly lineFilters = computed(() => lineFiltersFor(this.blindMode()));
  protected readonly selectedLineFilter = signal<InventoryLineFilter>('NONE');
  selectedStorageId: number | null = null;
  protected readonly selectedRayonId = signal<number | null>(null);
  quickFilterText = '';
  readonly rowData = computed(() => this.store.lines());
  readonly columnDefs = computed<ColDef[]>(() => [
    {
      field: 'produitCip',
      headerName: 'Code CIP',
      width: 120,
      editable: false,
      sortable: true,
      filter: true,
      type: 'text',
    },
    {
      field: 'produitLibelle',
      headerName: 'Produit',
      flex: 1,
      editable: false,
      sortable: true,
      filter: true,
    },
    {
      field: 'quantityInit',
      headerName: 'Stock actuel',
      width: 100,
      editable: false,

      type: ['rightAligned', 'numericColumn'],
      hide: this.blindMode(),
      valueFormatter: params => (params.value ?? '—'),
    },
    {
      field: 'seuilMini',
      headerName: 'Seuil mini',
      width: 100,
      editable: false,
      type: ['rightAligned', 'numericColumn'],
      hide: !this.showSeuilMini(),
      valueFormatter: params => (params.value ?? '—'),
      cellStyle: params => {
        const seuil = params.value;
        const stock = params.data?.quantityInit;
        if (seuil != null && stock != null && stock <= seuil) {
          return {color: '#dc3545', fontWeight: 'bold'};
        }
        return null;
      },
    },
    {
      field: 'quantityOnHand',
      headerName: 'Qté inventoriée',
      width: 130,
      // Seule colonne saisissable — et seulement tant que l'inventaire est ouvert.
      editable: !this.readOnly(),
      type: ['rightAligned', 'numericColumn'],
      cellStyle: params => {
        // Sauvegarde échouée : la quantité affichée n'est pas enregistrée. La grille
        // n'étant plus rechargée à chaque ligne, c'est le seul signal qui distingue
        // une ligne comptée d'une ligne perdue.
        if (params.data?.saveFailed) {
          return {backgroundColor: '#fde8ea', color: '#dc3545', fontWeight: 'bold'};
        }
        if (params.data?.updated) {
          return {backgroundColor: '#f0fff4', fontWeight: 'bold'};
        }
        return null;
      },
      cellEditorParams: {
        preventStepping: true
      }
    },
    {
      field: 'gap',
      headerName: 'Écart',
      width: 100,
      editable: false,
      type: 'numericColumn',
      // L'écart se déduit directement du stock théorique : même privilège que lui
      hide: this.blindMode(),
      cellStyle: params => {
        const val = params.value;
        if (val == null) {
          return null;
        }
        if (val < 0) {
          return {color: '#dc3545', backgroundColor: '#fde8ea', fontWeight: 'bold'};
        }
        if (val > 0) {
          return {color: '#0d6efd', backgroundColor: '#e7f1ff', fontWeight: 'bold'};
        }
        return null;
      },
    },
    {
      field: 'lotCount',
      headerName: 'Lots',
      width: 70,
      editable: false,
      hide: !this.gestionLot(),
      type: 'numericColumn',
      cellStyle: params => {
        if (params.value > 0) {
          return {color: '#0d6efd', fontWeight: 'bold', cursor: 'pointer'};
        }
        return null;
      },
      valueFormatter: params => params.value > 0 ? `${params.value}` : '—',
    },
    {
      field: 'classePareto',
      headerName: 'ABC',
      width: 70,
      editable: false,
      cellStyle: {textAlign: 'center'},
      headerClass: 'ag-header-cell-center',
      hide: this.inventoryCategoryType() !== 'ABC',
      cellRenderer: (params: any) => renderParetoBadge(params.value),
    },
    {
      field: 'updated',
      headerName: 'Saisi',
      width: 80,
      editable: false,
      cellStyle: {textAlign: 'center'},
      headerClass: 'ag-header-cell-center',
      cellRenderer: (params: any) => {
        if (params.data?.saveFailed) {
          return '<i class="pi pi-exclamation-triangle text-danger" title="Non enregistré — à ressaisir"></i>';
        }
        return params.value ? '<i class="pi pi-check text-success"></i>' : '';
      },
    },
    {
      field: 'countedBy',
      headerName: 'Compté par',
      width: 150,
      editable: false,
      hide: true,
      headerClass: 'ag-header-cell-center',
      // Traçabilité : compteur + horodatage du dernier comptage
      valueGetter: params => {
        const who = params.data?.countedBy;
        if (!who) {
          return '';
        }
        const at = params.data?.updatedAt;
        return at ? `${who} — ${formatDate(at, 'dd/MM HH:mm', 'fr')}` : who;
      },
    },
  ]);
  readonly defaultColDef: ColDef = {
    resizable: true,
    suppressMovable: false,
  };
  /**
   * Identité stable des lignes : sans elle, chaque `rowData` recrée les nœuds et
   * l'index d'une ligne n'a plus aucune continuité d'un chargement à l'autre.
   */
  readonly getRowId = (params: GetRowIdParams) => this.rowIdOf(params.data);
  protected readonly theme = themeAlpine;
  private pendingFocusRow0 = false;
  private gridApi: GridApi | null = null;
  /** Page reçue pendant une saisie, en attente de fermeture de l'éditeur */
  private pendingLines: IInventoryLine[] | null = null;
  /**
   * Ligne dont l'éditeur est en cours d'ouverture.
   *
   * `startEditingCell` est asynchrone et vise une *position*. Entre l'appel et l'ouverture
   * effective, aucune cellule n'est en édition : appliquer une page dans cette fenêtre
   * retire la ligne comptée, décale la grille, et l'éditeur s'ouvre sur la ligne qui a
   * glissé à cette position — celle qu'on visait est sautée. Ce drapeau ferme la fenêtre.
   */
  private focusRowId: string | null = null;
  /** Index de la ligne saisie, relevé avant sauvegarde — cf. {@link resolveNextIndex} */
  private lastKnownIndex = 0;
  /**
   * Ligne dont l'opérateur a ouvert l'éditeur et dont la validation n'a pas encore été
   * traitée. Sert de laissez-passer : cf. {@link onCellValueChanged}.
   */
  private userEditRowId: string | null = null;

  constructor() {
    // Filtre d'écart déjà actif au moment où le mode aveugle s'applique : on le
    // retombe sur « Tous », sinon la grille resterait filtrée sur une information
    // que l'opérateur n'a pas le droit de voir.
    effect(() => {
      if (this.blindMode() && isGapLineFilter(this.selectedLineFilter())) {
        this.selectedLineFilter.set('NONE');
        this.emitFilterChange();
      }
    });

    effect(() => {
      const lines = this.rowData();
      if (!this.gridApi) {
        return;
      }
      const applied = this.applyLines(lines);
      if (applied && this.pendingFocusRow0 && lines.length > 0) {
        this.pendingFocusRow0 = false;
        setTimeout(() => {
          this.gridApi?.ensureIndexVisible(0, 'top');
          this.gridApi?.startEditingCell({rowIndex: 0, colKey: 'quantityOnHand'});
        }, 100);
      }
    });
  }

  /**
   * Chargement initial. Au-delà, la grille n'a **qu'un seul** écrivain : {@link applyLines}.
   *
   * `[rowData]` n'est délibérément plus lié dans le template : l'entrée Angular poussait
   * chaque nouvelle page directement dans la grille, sans passer par le différé ni le
   * verrou d'ouverture. Le décalage se produisait donc par ce chemin, pendant que la
   * saisie était en cours. Ne pas rétablir cette liaison.
   */
  onGridReady(event: GridReadyEvent): void {
    this.gridApi = event.api;
    this.gridApi.setGridOption('rowData', this.rowData());
  }

  private rowIdOf(line: IInventoryLine): string {
    return String(line.id);
  }

  /**
   * Applique une page rechargée — sauf si une cellule est en cours de saisie, auquel cas
   * elle est mise de côté et appliquée dès la fermeture de l'éditeur.
   *
   * La grille ne doit jamais être mutée sous un éditeur ouvert : le repositionnement des
   * lignes fait perdre le focus à l'input, et `stopEditingWhenCellsLoseFocus` referme alors
   * l'éditeur en validant sa valeur — ce qui relance un `cellValueChanged`, donc une
   * seconde sauvegarde et une seconde navigation. Ni `setGridOption('rowData')` ni
   * `applyTransaction` n'y échappent.
   *
   * Différer plutôt qu'abandonner : l'ancien drapeau `suppressRowDataRefresh` sortait
   * simplement de l'effect, et comme le signal ne réémet pas la même valeur, la page
   * fraîche était perdue — la grille dérivait de son store et affichait encore des lignes
   * déjà comptées.
   *
   * @returns true si la page a été appliquée, false si elle a été différée.
   */
  private applyLines(lines: IInventoryLine[]): boolean {
    if (!this.gridApi) {
      return false;
    }
    if (this.isEntryInProgress()) {
      this.pendingLines = lines;
      return false;
    }
    this.pendingLines = null;
    this.gridApi.setGridOption('rowData', lines);
    return true;
  }

  /**
   * Saisie en cours : cellule en édition, ou éditeur en cours d'ouverture. Dans les deux
   * cas la grille ne doit pas bouger sous l'opérateur.
   */
  private isEntryInProgress(): boolean {
    return this.focusRowId !== null || (this.gridApi?.getEditingCells().length ?? 0) > 0;
  }

  /**
   * Applique la page mise de côté pendant la saisie. Appelée à la fermeture de l'éditeur
   * et juste avant de calculer la ligne suivante : l'index n'a d'importance qu'à cet
   * instant, et il doit être calculé sur la liste à jour.
   */
  protected flushPendingLines(): void {
    const pending = this.pendingLines;
    if (!pending || !this.gridApi || this.isEntryInProgress()) {
      return;
    }
    this.pendingLines = null;
    this.gridApi.setGridOption('rowData', pending);
  }

  /**
   * L'éditeur est ouvert : la fenêtre d'ouverture est refermée, le garde d'édition prend
   * le relais, et la ligne reçoit le laissez-passer qui autorisera **une** sauvegarde.
   */
  protected onCellEditingStarted(event: CellEditingStartedEvent): void {
    this.focusRowId = null;
    this.userEditRowId = event.node.id ?? null;
  }

  /** L'éditeur est fermé : plus rien ne retient la page en attente. */
  protected onCellEditingStopped(): void {
    this.focusRowId = null;
    this.flushPendingLines();
  }

  onQuickFilterChange(): void {
    this.gridApi?.setGridOption('quickFilterText', this.quickFilterText);
    this.emitFilterChange();
  }

  onLineFilterChange(): void {
    this.emitFilterChange();
  }

  onStorageFilterChange(): void {
    this.selectedRayonId.set(null);
    this.storageChange.emit(this.selectedStorageId);
    this.emitFilterChange();
  }

  onRayonFilterChange(): void {
    this.emitFilterChange();
  }

  onRowClicked(event: RowClickedEvent): void {
    if (!this.gestionLot()) {
      return;
    }
    const line = event.data as IInventoryLine;
    if (line?.lotCount && line.lotCount > 0) {
      this.lineSelected.emit(line);
    }
  }

  onCellValueChanged(event: CellValueChangedEvent): void {
    // `editable` bloque déjà la saisie ; ce garde couvre les écritures programmatiques
    // (setDataValue, collage) qui, elles, ne passent pas par l'éditeur.
    if (event.column.getColId() !== 'quantityOnHand' || this.readOnly()) {
      return;
    }
    // Seule une saisie de l'opérateur déclenche une sauvegarde, et une seule fois : le
    // laissez-passer est posé à l'ouverture de l'éditeur et consommé ici.
    //
    // `setDataValue` — qui restaure la valeur après un refus — émet lui aussi un
    // `cellValueChanged`. Sans ce garde, la restauration repartait en sauvegarde, qui
    // échouait, qui restaurait : boucle infinie, un toast par tour. Le critère porte sur
    // l'existence d'une session d'édition, et non sur un drapeau posé le temps de l'appel,
    // car `setDataValue` n'émet pas son événement de façon synchrone.
    if (this.userEditRowId === null || this.userEditRowId !== event.node.id) {
      return;
    }
    this.userEditRowId = null;

    const newValue = event.newValue;
    const lineData = event.data as IInventoryLine;
    if (lineData?.id == null) {
      return;
    }

    const numValue = Number(newValue);
    const isValid = Number.isFinite(numValue) && numValue >= 0;

    if (!isValid) {
      // Invalid input: revert and stay editing the same cell
      this.revertQuantity(event);
      return;
    }

    // Build the line payload for the API
    const lineToSave: IInventoryLine = {
      ...lineData,
      quantityOnHand: numValue,
      storeInventoryId: this.inventoryId(),
    };

    this.lastKnownIndex = event.rowIndex!;

    // La ligne n'est tenue pour comptée qu'une fois le serveur d'accord, et le curseur
    // n'avance qu'à ce moment-là. Marquer et naviguer sans attendre laissait l'opérateur
    // enchaîner des lignes que le serveur refusait — backend coupé, l'inventaire se
    // remplissait de comptages qui n'existaient pas.
    this.editorFacade.saveLine(lineToSave).subscribe({
      next: () => {
        event.node.setDataValue('updated', true);
        this.navigateToNextRow(event.node);
      },
      error: () => {
        // Saisie invalidée : la valeur précédente est restaurée et le curseur reste sur
        // la cellule, pour que la ligne soit reprise plutôt que tenue pour comptée.
        this.revertQuantity(event);
      },
    });
  }

  /**
   * Restaure la valeur précédente d'une cellule sans relancer le cycle de saisie, puis
   * redonne la main sur cette même cellule.
   *
   * `setDataValue` émet un `cellValueChanged` : sans le drapeau, la restauration serait
   * traitée comme une nouvelle saisie et repartirait en sauvegarde.
   */
  private revertQuantity(event: CellValueChangedEvent): void {
    event.node.setDataValue('quantityOnHand', event.oldValue);
    setTimeout(() => {
      // Même cellule, mais son index a pu bouger si un rechargement a abouti entre-temps
      const rowIndex = event.node.rowIndex ?? event.rowIndex!;
      this.gridApi?.startEditingCell({rowIndex, colKey: 'quantityOnHand'});
    });
  }

  /**
   * Index de la ligne à saisir ensuite, calculé à l'instant où l'on navigue.
   *
   * `event.rowIndex` est figé au moment où la cellule est validée. Si un rechargement
   * intervient entre-temps — et il intervient : chaque sauvegarde en déclenche un — sous
   * un filtre qui masque les lignes comptées, la page remonte d'un cran et cet index
   * désigne alors la ligne d'après : une ligne sur deux était sautée. `node.rowIndex`
   * suit le nœud, à condition que son identité soit stable (`getRowId`).
   *
   * Nœud sorti de la grille — la ligne qu'on vient de compter, sous filtre « non comptés »,
   * quand le rechargement a été appliqué avant qu'on navigue : la ligne qui la suivait a
   * pris sa place, c'est donc *son propre* index qu'il faut viser, et non l'index suivant.
   *
   * L'appartenance à la grille se teste par `getRowNode`, et surtout pas par le
   * `rowIndex` du nœud reçu : AG Grid abandonne le nœud retiré sans réinitialiser cette
   * propriété, qui garde donc son dernier index et ferait passer une ligne disparue pour
   * une ligne toujours présente.
   */
  private resolveNextIndex(node: IRowNode): number {
    const live = node.id != null ? this.gridApi?.getRowNode(node.id) : null;
    return live?.rowIndex != null ? live.rowIndex + 1 : this.lastKnownIndex;
  }

  /**
   * Fait suivre la grille : la ligne suivante est amenée dans le viewport avant
   * d'ouvrir son éditeur. Sans `ensureIndexVisible`, une ligne sortie du viewport
   * n'est plus rendue (virtualisation AG Grid) et `startEditingCell` reste sans
   * effet — la saisie s'arrêtait au bas de l'écran.
   */
  private navigateToNextRow(node: IRowNode): void {
    setTimeout(() => {
      // L'éditeur est fermé : la page mise de côté peut être appliquée sans risque.
      // Tout ce qui suit — index cible comme nombre de lignes — se calcule ensuite,
      // sur la liste à jour.
      this.flushPendingLines();

      const nextIndex = this.resolveNextIndex(node);
      const rowCount = this.gridApi?.getDisplayedRowCount() ?? 0;
      if (nextIndex >= rowCount) {
        // Last row of the server page: load next page and focus row 0
        this.pendingFocusRow0 = true;
        this.nextPage.emit();
        return;
      }
      // Verrou posé AVANT l'appel : `startEditingCell` n'ouvre pas l'éditeur sur-le-champ,
      // et une page appliquée dans l'intervalle décalerait la ligne visée.
      this.focusRowId = this.gridApi?.getDisplayedRowAtIndex(nextIndex)?.id ?? null;
      this.gridApi?.ensureIndexVisible(nextIndex, 'middle');
      this.gridApi?.startEditingCell({rowIndex: nextIndex, colKey: 'quantityOnHand'});

      // Filet : si l'éditeur ne s'ouvre pas (ligne non éditable, grille clôturée…),
      // le verrou ne doit pas geler indéfiniment les rechargements.
      setTimeout(() => {
        if (this.focusRowId !== null) {
          this.focusRowId = null;
          this.flushPendingLines();
        }
      }, 500);
    }, 50);
  }

  private emitFilterChange(): void {
    this.filterChange.emit({
      lineFilter: this.selectedLineFilter(),
      storageId: this.selectedStorageId,
      rayonId: this.selectedRayonId(),
      search: this.quickFilterText,
    });
  }
}
