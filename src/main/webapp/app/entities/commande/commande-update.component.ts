import { Component, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';
import { OrderLineService } from '../order-line/order-line.service';
import { ProduitService } from '../produit/produit.service';
import { IProduit } from 'app/shared/model/produit.model';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModules } from '@ag-grid-community/all-modules';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertInfoComponent } from 'app/shared/alert/alert-info.component';
@Component({
  selector: 'jhi-commande-update',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }
      .active {
        background-color: #95caf9 !important;
      }
      .master {
        padding: 14px 12px;
        border-radius: 12px;
        box-shadow: 0 4px 8px rgb(0 0 0 / 16%);
        justify-content: space-between;
      }
      .ag-theme-alpine {
        max-height: 700px;
        height: 500px;
        min-height: 400px;
      }
    `,
  ],
  templateUrl: './commande-update.component.html',
})
export class CommandeUpdateComponent implements OnInit {
  isSaving = false;
  receiptDateDp: any;
  produits?: any[] = [];
  commande?: ICommande;
  public columnDefs: any[];
  rowData: any = [];
  produitSelect?: IProduit[] = [];
  searchValue?: string;
  @ViewChild('productGrid') productGrid!: AgGridAngular;
  public modules: any[] = AllCommunityModules;
  constructor(
    protected commandeService: CommandeService,
    protected orderLineService: OrderLineService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected modalService: NgbModal
  ) {
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'libelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 2,
      },
      {
        headerName: 'Stock',
        field: 'quantity',
        minWidth: 100,
        sortable: true,
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Prix achat',
        minWidth: 150,
        field: 'costAmount',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Prix unitaire',
        minWidth: 150,
        field: 'regularUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Quantité reçue',
        width: 150,
        field: 'quantityReceived',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
      },
      {
        width: 40,
        headerCheckboxSelection: true,
        headerCheckboxSelectionFilteredOnly: true,
        checkboxSelection: true,
      },
    ];
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ commande }) => {
      if (!commande.id) {
        this.commande = commande;
        const today = moment().startOf('day');
        commande.createdAt = today;
        commande.updatedAt = today;
      }
    });
    this.loadProduits();
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.produitSelect = this.getSelectedRows();
    if (this.produitSelect.length === 0) {
      this.openDialog('Veuillez sélectionner les produits à ajouter à la commande');
    } else {
      if (!this.produitSelect.some(this.checkQuantityNotSet)) {
        this.isSaving = true;
        this.subscribeToSaveResponse(this.commandeService.createNewCommand(this.produitSelect));
      } else {
        this.openDialog('Veuillez saisir les quantités de tous les produits séléctionnés');
      }
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProduit[]>>): void {
    result.subscribe(
      () => this.onSaveSuccess(),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
  }
  getSelectedRows(): IProduit[] {
    const selectedNodes = this.productGrid.api.getSelectedNodes();
    const selectedData = selectedNodes.map(node => node.data);
    //  const selectedDataStringPresentation = selectedData.map(node => `${node.make} ${node.model}`).join(', ');

    return selectedData;
  }
  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 9999,
        withdetail: false,
      })
      .subscribe(
        (res: HttpResponse<any[]>) => this.onProduitSuccess(res.body),
        () => this.onError()
      );
  }
  protected onProduitSuccess(data: IProduit[] | null): void {
    this.rowData = data || [];
  }
  protected onError(): void {}

  onFilterTextBoxChanged(event: any): void {
    this.searchValue = event.target.value;
  }
  checkQuantityNotSet(produit: IProduit): boolean {
    return produit.quantityReceived === 0;
  }
  openDialog(message: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static' });
    modalRef.componentInstance.message = message;
  }
}
