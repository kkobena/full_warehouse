import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Ajustement, IAjustement } from 'app/shared/model/ajustement.model';
import { IProduit } from '../../shared/model/produit.model';
import { ProduitService } from '../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AjustementService } from './ajustement.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { IMotifAjustement } from '../../shared/model/motif-ajustement.model';
import { ModifAjustementService } from '../modif-ajustement/motif-ajustement.service';
import { AjustementBtnRemoveComponent } from './btn-remove/ajustement-btn-remove.component';
import { IAjust } from '../../shared/model/ajust.model';

@Component({
  selector: 'jhi-ajustement-detail',
  templateUrl: './ajustement-detail.component.html',
})
export class AjustementDetailComponent implements OnInit {
  myId = -1;
  ajustementId = -1;
  columnDefs: any[];
  rowData: any = [];
  event: any;
  motifCmp: any;
  produitSelected!: IProduit | null;
  motifSelected!: IMotifAjustement | null;
  isSaving = false;
  produits: IProduit[] = [];
  motifs: IMotifAjustement[] = [];
  @ViewChild('quantyBox', { static: false })
  quantyBox?: ElementRef;
  @ViewChild('comment', { static: false })
  comment?: ElementRef;
  search: string;
  defaultColDef: any;
  frameworkComponents: any;
  context: any;

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected produitService: ProduitService,
    protected modalService: NgbModal,
    protected ajustementService: AjustementService,
    protected modifAjustementService: ModifAjustementService
  ) {
    this.search = '';
    this.columnDefs = [
      {
        headerName: 'Code cip',
        field: 'codeCip',
        sortable: true,
        filter: 'agTextColumnFilter',
      },
      {
        headerName: 'Libellé produit',
        field: 'produitLibelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 1.5,
      },
      {
        headerName: 'Quantité ajustée',
        field: 'qtyMvt',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
      },

      {
        headerName: 'Stock avant ajustement',
        field: 'stockBefore',
        type: ['rightAligned', 'numericColumn'],
        editable: true,

        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Stock après ajustement',
        field: 'stockAfter',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        field: ' ',
        cellRenderer: 'btnCellRenderer',
        width: 50,
      },
    ];
    this.defaultColDef = {
      // flex: 1,
      // cellClass: 'align-right',
      enableCellChangeFlash: true,
      //   resizable: true,
      /* valueFormatter: function (params) {
         return formatNumber(params.value);
       },*/
    };
    this.frameworkComponents = {
      btnCellRenderer: AjustementBtnRemoveComponent,
    };
    this.context = { componentParent: this };
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ ajustement }) => (this.ajustementId = ajustement));
    this.loadProduits('');
    this.loadMotifs('');
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  previousState(): void {
    window.history.back();
  }

  onCellValueChanged(params: any): void {
    this.subscribeToAjustementResponse(this.ajustementService.update(this.updateFromForm(params.data)));
  }

  add(event: any): void {
    if (this.myId === -1) {
      this.subscribeToAjustementResponse(
        this.ajustementService.create({
          produitId: this.produitSelected?.id,
          qtyMvt: event.target.value,
          commentaire: this.comment!.nativeElement.value,
          motifAjustementId: this.motifSelected?.id,
        })
      );
    } else {
      this.subscribeToAjustementResponse(
        this.ajustementService.create({
          produitId: this.produitSelected?.id,
          qtyMvt: event.target.value,
          ajustId: this.ajustementId,
          commentaire: this.comment!.nativeElement.value,
          motifAjustementId: this.motifSelected?.id,
        })
      );
    }
  }

  loadAll(ajsut: number | null): void {
    this.ajustementService.query({ id: ajsut }).subscribe((res: HttpResponse<IAjustement[]>) => (this.rowData = res.body || []));
  }

  onFilterTextBoxChanged(event: any): void {
    if (this.produitSelected !== null && this.produitSelected !== undefined && Number(event.target.value) !== 0) {
      this.add(event);
    }
  }

  loadProduits(query: string): void {
    let search = '';
    if (query) {
      search = query;
    }
    this.produitService
      .query({
        page: 0,
        size: 4,
        withdetail: true,
        search,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => this.onProduitSuccess(res.body));
  }

  loadMotifs(query: string): void {
    let search = '';
    if (query) {
      search = query;
    }
    this.modifAjustementService
      .query({
        page: 0,
        size: 9999,
        search,
      })
      .subscribe((res: HttpResponse<IMotifAjustement[]>) => this.onMotifSuccess(res.body));
  }

  save(): void {
    this.isSaving = true;
    this.subscribeToFinalyseResponse(
      this.ajustementService.save({
        ajustId: this.ajustementId,
        produitId: 0,
        commentaire: this.comment!.nativeElement.value,
      })
    );
  }

  onSelect(event: any): void {
    this.event = event;
    if (this.quantyBox) {
      this.quantyBox.nativeElement.focus();
      this.quantyBox.nativeElement.value = 1;
    }
  }

  searchFn(event: any): void {
    const key = event.key;
    if (
      key !== 'ArrowDown' &&
      key !== 'ArrowUp' &&
      key !== 'ArrowRight' &&
      key !== 'ArrowLeft' &&
      key !== 'NumLock' &&
      key !== 'CapsLock' &&
      key !== 'Control' &&
      key !== 'PageUp' &&
      key !== 'PageDown'
    ) {
      this.loadProduits(event.target.value);
    }
  }

  produitComponentSearch(term: string, item: IProduit): boolean {
    if (item) return true;
    return false;
  }

  searchMotif(event: any): void {
    const key = event.key;
    if (
      key !== 'ArrowDown' &&
      key !== 'ArrowUp' &&
      key !== 'ArrowRight' &&
      key !== 'ArrowLeft' &&
      key !== 'NumLock' &&
      key !== 'CapsLock' &&
      key !== 'Control' &&
      key !== 'PageUp' &&
      key !== 'PageDown'
    ) {
      this.loadMotifs(event.target.value);
    }
  }

  onSelectMotif(event: any): void {
    this.motifCmp = event;
    if (this.quantyBox) {
      this.quantyBox.nativeElement.focus();
      this.quantyBox.nativeElement.value = 1;
    }
  }

  removeLine(data: any): void {
    this.ajustementService.delete(data.id).subscribe(() => {
      this.loadAll(this.ajustementId);
    });
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe(
      () => this.onSaveFinalyseSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveFinalyseSuccess(): void {
    this.isSaving = false;
    this.myId = -1;
    this.previousState();
  }

  protected onMotifSuccess(data: IMotifAjustement[] | null): void {
    this.motifs = data || [];
  }

  protected subscribeToAjustementResponse(result: Observable<HttpResponse<IAjustement>>): void {
    result.subscribe(
      (res: HttpResponse<IAjustement>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  private updateFromForm(ajustement: IAjustement): IAjustement {
    return {
      ...new Ajustement(),
      produitId: ajustement.produitId,
      id: ajustement.id,
      ajustId: ajustement.ajustId,
      qtyMvt: ajustement.qtyMvt,
      commentaire: this.comment!.nativeElement.value,
    };
  }

  protected onSaveSuccess(ajsut: IAjustement | null): void {
    this.isSaving = false;
    this.ajustementId = ajsut?.ajustId!;
    this.myId = 0;
    this.loadAll(this.ajustementId);
    this.quantyBox!.nativeElement.value = 1;
    this.event.searchInput.nativeElement.focus();
    this.event.searchInput.nativeElement.value = '';
    this.produitSelected = null;
  }

  protected refresh(): void {
    this.subscribeToSaveResponse(this.ajustementService.find(this.ajustementId));
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IAjust>>): void {
    result.subscribe(
      (res: HttpResponse<IAjust>) => this.onUpdateLineSuccess(res.body),
      () => this.onSaveLineError()
    );
  }

  protected onUpdateLineSuccess(ajsut: IAjust | null): void {
    this.rowData = ajsut?.ajustements;
  }

  protected onSaveLineError(): void {}
}
