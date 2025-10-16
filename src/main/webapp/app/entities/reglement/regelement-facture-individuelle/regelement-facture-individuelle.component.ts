import { Component, computed, effect, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';
import { ButtonModule } from 'primeng/button';
import { TableHeaderCheckbox, TableModule } from 'primeng/table';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { SplitButtonModule } from 'primeng/splitbutton';
import { NgbAlertModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FieldsetModule } from 'primeng/fieldset';
import { DossierReglementInfoComponent } from '../dossier-reglement-info/dossier-reglement-info.component';
import { ReglementFormComponent } from '../reglement-form/reglement-form.component';
import { ModeEditionReglement, ReglementParams, ResponseReglement, SelectedFacture } from '../model/reglement.model';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ErrorService } from '../../../shared/error.service';
import { ReglementService } from '../reglement.service';
import { FactureService } from '../../facturation/facture.service';
import { HttpResponse } from '@angular/common/http';
import { FactuesModalComponent } from '../factues-modal/factues-modal.component';
import { Drawer } from 'primeng/drawer';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { FactureId } from '../../facturation/facture.model';

@Component({
  selector: 'jhi-regelement-facture-individuelle',

  imports: [
    WarehouseCommonModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    SplitButtonModule,
    NgbAlertModule,
    FieldsetModule,
    DossierReglementInfoComponent,
    ReglementFormComponent,
    FactuesModalComponent,
    Drawer,
    IconField,
    InputIcon,
    ConfirmDialogComponent,
  ],
  templateUrl: './regelement-facture-individuelle.component.html',
  styleUrl: './regelement-facture-individuelle.component.scss',
})
export class RegelementFactureIndividuelleComponent implements OnInit {
  readonly reglementFactureDossiers = input<ReglementFactureDossier[]>([]);
  reglementFactureDossiersSignal = signal(this.reglementFactureDossiers());
  readonly dossierFactureProjection = input<DossierFactureProjection | null>(null);
  dossierFactureProjectionSignal = signal(this.dossierFactureProjection());
  checkbox = viewChild<TableHeaderCheckbox>('checkbox');
  factureDossierSelectionnes = signal<ReglementFactureDossier[]>([]);
  montantAPayer = computed(() => {
    return this.factureDossierSelectionnes()?.reduce((acc, d) => acc + this.computeMontantRestant(d), 0) || 0;
  });
  dossierIds = computed(() => {
    return this.factureDossierSelectionnes()?.map(d => d.id) || [];
  });
  reglementFormComponent = viewChild(ReglementFormComponent);
  readonly selectedFacture = output<SelectedFacture>();
  protected showSidebar = false;
  protected partialPayment = false;
  protected readonly ModeEditionReglement = ModeEditionReglement;
  protected isSaving = false;
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly reglementService = inject(ReglementService);
  private readonly factureService = inject(FactureService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');

  constructor() {
    effect(() => {
      if (this.montantAPayer()) {
        this.reglementFormComponent().cashInput.setValue(this.montantAPayer());
      } else {
        this.reglementFormComponent().cashInput.setValue(this.montantAttendu);
      }
      if (this.dossierFactureProjection()) {
        this.dossierFactureProjectionSignal.set(this.dossierFactureProjection());
      }
    });
  }

  get totalAmount(): number {
    if (!this.partialPayment) {
      return this.montantAttendu;
    }
    return this.montantAPayer();
  }

  get drawerWidth(): {} {
    return window.innerWidth <= 1280 ? { width: '85vw' } : { width: '70vw' };
  }

  private get montantAttendu(): number {
    const dossierFactureProjection = this.dossierFactureProjection();
    if (dossierFactureProjection) {
      return dossierFactureProjection.montantTotal - dossierFactureProjection.montantDetailRegle;
    }
    return null;
  }

  openSideBar(): void {
    this.showSidebar = true;
  }

  onError(error: any): void {
    this.isSaving = false;
    this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  onSelectFacture(facture: SelectedFacture): void {
    this.selectedFacture.emit(facture);
    this.showSidebar = false;
  }

  ngOnInit(): void {
    this.reglementFactureDossiersSignal.set(this.reglementFactureDossiers());
  }

  protected onPartielReglement(evt: boolean): void {
    this.partialPayment = evt;
    this.factureDossierSelectionnes.set([]);
  }

  protected onSelectChange(event: any): void {
    this.factureDossierSelectionnes.set(event as ReglementFactureDossier[]);
  }

  protected onSaveReglement(params: ReglementParams): void {
    this.isSaving = true;
    this.reglementService.doReglement(this.buildReglementParams(params)).subscribe({
      next: res => {
        this.isSaving = false;
        if (res.body) {
          this.onPrintReceipt(res.body);
        }
      },
      error: err => this.onError(err),
    });
  }

  private onPrintReceipt(response: ResponseReglement): void {
    this.confimDialog().onConfirm(
      () => {
        this.reglementService.printReceipt(response.id).subscribe();
        this.reset(response);
      },
      'TICKET REGLEMENT',
      ' Voullez-vous imprimer le ticket ?',
      'pi pi-info-circle',
      () => this.reset(response),
    );
  }

  private computeMontantRestant(d: ReglementFactureDossier): number {
    return d.montantTotal - d.montantPaye;
  }

  private getModeEditionReglement(): ModeEditionReglement {
    return this.partialPayment ? ModeEditionReglement.FACTURE_PARTIEL : ModeEditionReglement.FACTURE_TOTAL;
  }

  private buildReglementParams(params: ReglementParams): ReglementParams {
    console.warn(params);
    return {
      ...params,
      mode: this.getModeEditionReglement(),
      dossierIds: this.dossierIds(),
    };
  }

  private reset(response: ResponseReglement): void {
    this.factureDossierSelectionnes.set([]);
    this.reglementFormComponent().reset();
    if (response.total) {
      this.dossierFactureProjectionSignal.set(null);
      this.reglementFactureDossiersSignal.set([]);
      this.reglementFormComponent().cashInput.setValue(null);
    } else {
      this.fetchFacture();
      const factureId: FactureId = {
        id: this.dossierFactureProjection().id,
        invoiceDate: this.dossierFactureProjection().invoiceDate,
      };

      this.reload(factureId);
    }
  }

  private reload(id: FactureId): void {
    this.factureService
      .findDossierReglement(id, 'individuelle', {
        page: 0,
        size: 999999,
      })
      .subscribe({
        next: (res: HttpResponse<ReglementFactureDossier[]>) => {
          this.reglementFactureDossiersSignal.set(res.body);
          this.reglementFormComponent().cashInput.setValue(this.montantAttendu);
        },
        error: () => {
          this.reglementFactureDossiersSignal.set([]);
          this.dossierFactureProjectionSignal.set(null);
        },
      });
  }

  private fetchFacture(): void {
    const factureId = this.dossierFactureProjection().factureItemId;
    this.factureService
      .findDossierFactureProjection(factureId, {
        isGroup: false,
      })
      .subscribe(res => {
        this.dossierFactureProjectionSignal.set(res.body);
      });
  }
}
