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
import { Ajust, IAjust } from '../../shared/model/ajust.model';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../shared/constants/pagination.constants';
import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { ErrorService } from '../../shared/error.service';
import { FinalyseComponent } from './finalyse/finalyse.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { BadgeModule } from 'primeng/badge';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'jhi-ajustement-detail',
  templateUrl: './ajustement-detail.component.html',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    CardModule,
    FormsModule,
    ConfirmDialogModule,
    DynamicDialogModule,
    ToolbarModule,
    ButtonModule,
    AutoCompleteModule,
    InputTextModule,
    BadgeModule,
    TableModule,
  ],
  providers: [ConfirmationService, DialogService],
})
export class AjustementDetailComponent implements OnInit {
  protected ajustement: IAjust | null = null;
  protected produitSelected!: IProduit | null;
  protected motifSelected!: IMotifAjustement | null;
  protected isSaving = false;
  protected produits: IProduit[] = [];
  protected motifs: IMotifAjustement[] = [];
  protected items: IAjustement[] = [];
  @ViewChild('quantityBox')
  protected quantityBox?: ElementRef;
  @ViewChild('comment')
  protected comment?: ElementRef;
  protected search: string;
  protected context: any;
  @ViewChild('produitbox')
  protected produitbox?: any;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected selectedEl: IAjustement[];
  protected ref?: DynamicDialogRef;

  constructor(
    protected activatedRoute: ActivatedRoute,
    protected produitService: ProduitService,
    protected modalService: NgbModal,
    private errorService: ErrorService,
    protected ajustementService: AjustementService,
    protected modifAjustementService: ModifAjustementService,
    private confirmationService: ConfirmationService,
    private dialogService: DialogService,
  ) {
    this.search = '';
    this.selectedEl = [];
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ ajustement }) => {
      if (ajustement.id) {
        this.ajustement = ajustement;
        this.items = ajustement.ajustements;
      }
    });

    this.loadProduits();
    this.loadMotifs('');
  }

  previousState(): void {
    window.history.back();
  }

  confirmGoBack(): void {
    this.confirmationService.confirm({
      message: ' Vous aller être rediriger à la parge précedente  ?',
      header: ' REDIRECTION',
      icon: 'pi pi-warning-circle',
      accept: () => this.previousState(),
      key: 'redirect',
    });
  }

  deleteSelectedItems(): void {
    const ids = this.selectedEl.map(e => e.id);
    this.ajustementService.deleteItemsByIds(ids).subscribe(() => {
      this.selectedEl = [];
      this.onSuccess();
    });
  }

  confirmDeleteItem(item: IAjustement): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette ligne ?',
      header: 'SUPPRESSION  ',
      icon: 'pi pi-info-circle',
      accept: () => this.removeLine(item),
      reject: () => {
        this.focusPrdoduitBox();
      },
      key: 'deleteItem',
    });
  }

  showWarningMessage(): void {
    this.confirmationService.confirm({
      message: ' Vous devez selectionner le motif',
      header: 'MOTIF AJUSTEMENT  ',
      icon: 'pi pi-times-circle',
      reject() {},
      key: 'warningMessage',
    });
  }

  confirmDeleteItems(): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer toutes les lignes  ?',
      header: 'SUPPRESSION  ',
      icon: 'pi pi-info-circle',
      accept: () => this.deleteSelectedItems(),
      reject: () => {
        this.focusPrdoduitBox();
      },
      key: 'deleteItem',
    });
  }

  onUpdateQuantity(ajustement: IAjustement, event: any): void {
    const newQuantityRequested: number = Number(event.target.value);
    ajustement.qtyMvt = newQuantityRequested;
    this.subscribeAddItemResponse(this.ajustementService.updateItem(ajustement));
  }

  protected onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    this.onAddItem(qytMvt);
  }

  protected onAddItem(qytMvt: number): void {
    if (this.produitSelected) {
      if (!this.motifSelected) {
        this.showWarningMessage();
      } else {
        if (this.ajustement.id !== undefined) {
          this.subscribeAddItemResponse(this.ajustementService.addItem(this.createItem(this.produitSelected, qytMvt)));
        } else {
          this.subscribeCreateNewResponse(this.ajustementService.create(this.createAjustement(this.produitSelected, qytMvt)));
        }
      }
    }
  }

  protected loadAll(ajsut: number | null): void {
    this.ajustementService.query({ ajustementId: ajsut }).subscribe((res: HttpResponse<IAjustement[]>) => (this.items = res.body || []));
  }

  protected loadProduits(query?: string): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 10,
        withdetail: true,
        search: query,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => this.onProduitSuccess(res.body));
  }

  protected loadMotifs(query?: string): void {
    this.modifAjustementService
      .query({
        page: 0,
        size: 9999,
        search: query,
      })
      .subscribe((res: HttpResponse<IMotifAjustement[]>) => this.onMotifSuccess(res.body));
  }

  protected onSave(): void {
    this.ref = this.dialogService.open(FinalyseComponent, {
      data: { entity: this.ajustement },
      width: '40%',
      height: '350',
      header: "Finalisation de l'ajustement",
    });
    this.ref.onClose.subscribe(() => this.onSaveFinalyseSuccess());
  }

  protected onSelect(): void {
    this.setQuantityBoxFocused();
  }

  protected searchFn(event: any): void {
    this.loadProduits(event.query);
  }

  protected searchMotif(event: any): void {
    this.loadMotifs(event.query);
  }

  protected onSelectMotif(event: any): void {
    this.focusPrdoduitBox();
  }

  protected removeLine(ajustement: IAjustement): void {
    this.ajustementService.deleteItem(ajustement.id).subscribe(() => {
      this.loadAll(this.ajustement.id);
    });
  }

  protected onFilterItems(): void {
    const query = {
      ajustementId: this.ajustement.id,
      search: this.search,
    };
    this.ajustementService.query(query).subscribe((res: HttpResponse<IAjustement[]>) => (this.items = res.body || []));
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveFinalyseSuccess(),
      error: (err: any) => this.onSaveError(err),
    });
  }

  protected onSaveFinalyseSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onMotifSuccess(data: IMotifAjustement[] | null): void {
    this.motifs = data || [];
  }

  protected subscribeCreateNewResponse(result: Observable<HttpResponse<IAjust>>): void {
    result.subscribe({
      next: (res: HttpResponse<IAjust>) => this.onSaveSuccess(res.body),
      error: (err: any) => this.onSaveError(err),
    });
  }

  protected subscribeAddItemResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: (res: HttpResponse<{}>) => this.onSaveSuccess(),
      error: (err: any) => this.onSaveError(err),
    });
  }

  protected onSaveError(err: any): void {
    this.isSaving = false;
    this.onCommonError(err);
  }

  protected onSaveSuccess(ajsut?: IAjustement): void {
    this.isSaving = false;
    this.onSuccess(ajsut);
  }

  protected onSuccess(ajsut?: IAjust): void {
    if (ajsut) {
      this.ajustement = ajsut;
    }
    this.loadAll(this.ajustement.id);
    this.produitSelected = null;
    this.quantityBox.nativeElement.value = 1;
    this.focusPrdoduitBox();
  }

  protected refresh(): void {
    this.subscribeToSaveResponse(this.ajustementService.find(this.ajustement.id));
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IAjust>>): void {
    result.subscribe({
      next: (res: HttpResponse<IAjust>) => this.onUpdateLineSuccess(res.body),
      error: () => this.onSaveLineError(),
    });
  }

  protected onUpdateLineSuccess(ajsut: IAjust | null): void {
    this.items = ajsut.ajustements;
  }

  protected onSaveLineError(): void {}

  private openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  private onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        error: () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
      });
    }
  }

  private focusPrdoduitBox(): void {
    setTimeout(() => {
      this.produitbox.inputEL.nativeElement.focus();
      this.produitbox.inputEL.nativeElement.select();
    }, 50);
  }

  private createAjustement(produit: IProduit, quantity: number): IAjust {
    return {
      ...new Ajust(),
      ajustements: [this.createItem(produit, quantity)],
    };
  }

  private createItem(produit: IProduit, quantity: number): IAjustement {
    return {
      ...new Ajustement(),
      produitId: produit.id,
      qtyMvt: quantity,
      ajustId: this.ajustement.id,
      motifAjustementId: this.motifSelected.id,
    };
  }

  private setQuantityBoxFocused(): void {
    setTimeout(() => {
      const el = this.quantityBox.nativeElement;
      el.focus();
      el.value = 1;
      el.select();
    }, 100);
  }

  private updateFromForm(ajustement: IAjustement): IAjustement {
    return {
      ...new Ajustement(),
      produitId: ajustement.produitId,
      id: ajustement.id,
      ajustId: ajustement.ajustId,
      qtyMvt: ajustement.qtyMvt,
      commentaire: this.comment.nativeElement.value,
    };
  }
}
