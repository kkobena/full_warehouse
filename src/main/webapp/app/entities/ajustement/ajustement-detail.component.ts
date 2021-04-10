import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Ajustement, IAjustement } from 'app/shared/model/ajustement.model';
import { IProduit } from '../../shared/model/produit.model';
import { ProduitService } from '../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AjustementService } from './ajustement.service';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';

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
  produitSelected!: IProduit | null;
  isSaving = false;
  produits: IProduit[] = [];
  @ViewChild('quantyBox', { static: false })
  quantyBox?: ElementRef;

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected produitService: ProduitService,
    protected modalService: NgbModal,
    protected ajustementService: AjustementService
  ) {
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 1.5,
      },
      {
        headerName: 'Quantité ajustée',
        width: 100,
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
    ];
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ ajustement }) => (this.ajustementId = ajustement));
    this.loadProduits();
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
        })
      );
    } else {
      this.subscribeToAjustementResponse(
        this.ajustementService.create({
          produitId: this.produitSelected?.id,
          qtyMvt: event.target.value,
          ajustId: this.ajustementId,
        })
      );
    }
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
    };
  }

  protected onSaveSuccess(ajsut: IAjustement | null): void {
    this.isSaving = false;
    this.ajustementId = ajsut?.ajustId!;
    this.myId = 0;
    this.loadAll(this.ajustementId);
    this.quantyBox!.nativeElement.value = '';
    this.event.searchInput.nativeElement.focus();
    this.event.searchInput.nativeElement.value = '';
    this.produitSelected = null;
  }

  loadAll(ajsut: number | null): void {
    this.ajustementService.query({ id: ajsut }).subscribe((res: HttpResponse<IAjustement[]>) => (this.rowData = res.body || []));
  }

  onFilterTextBoxChanged(event: any): void {
    if (this.produitSelected !== null && this.produitSelected !== undefined && Number(event.target.value) !== 0) {
      this.add(event);
    }
  }

  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 9999,
        withdetail: true,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => this.onProduitSuccess(res.body));
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  onSelect(event: any): void {
    this.event = event;
    if (this.quantyBox) {
      this.quantyBox.nativeElement.focus();
    }
  }

  save(): void {
    this.isSaving = true;
    this.subscribeToFinalyseResponse(this.ajustementService.save(this.ajustementId));
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
}
