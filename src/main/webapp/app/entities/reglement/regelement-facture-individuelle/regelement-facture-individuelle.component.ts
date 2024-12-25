import { Component, computed, effect, inject, Input, signal, viewChild } from '@angular/core';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';
import { ButtonModule } from 'primeng/button';
import { TableHeaderCheckbox, TableModule } from 'primeng/table';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SplitButtonModule } from 'primeng/splitbutton';
import { NgbAlertModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FieldsetModule } from 'primeng/fieldset';
import { SidebarModule } from 'primeng/sidebar';
import { DossierReglementInfoComponent } from '../dossier-reglement-info/dossier-reglement-info.component';
import { ReglementFormComponent } from '../reglement-form/reglement-form.component';
import { ModeEditionReglement, ReglementParams, ResponseReglement } from '../model/reglement.model';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { ConfirmationService } from 'primeng/api';
import { ErrorService } from '../../../shared/error.service';
import { ReglementService } from '../reglement.service';
import { FactureService } from '../../facturation/facture.service';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-regelement-facture-individuelle',
  standalone: true,
  providers: [ConfirmationService],
  imports: [
    WarehouseCommonModule,
    TableModule,
    InputTextModule,
    ButtonModule,
    RippleModule,
    TooltipModule,
    ConfirmDialogModule,
    SplitButtonModule,
    NgbAlertModule,
    FieldsetModule,
    SidebarModule,
    DossierReglementInfoComponent,
    ReglementFormComponent,
  ],
  templateUrl: './regelement-facture-individuelle.component.html',
})
export class RegelementFactureIndividuelleComponent {
  @Input() reglementFactureDossiers: ReglementFactureDossier[] = [];
  @Input() dossierFactureProjection: DossierFactureProjection | null = null;
  checkbox = viewChild<TableHeaderCheckbox>('checkbox');
  factureDossierSelectionnes = signal<ReglementFactureDossier[]>([]);
  montantAPayer = computed(() => {
    return this.factureDossierSelectionnes()?.reduce((acc, d) => acc + this.computeMontantRestant(d), 0) || 0;
  });
  dossierIds = computed(() => {
    return this.factureDossierSelectionnes()?.map(d => d.id) || [];
  });
  reglementFormComponent = viewChild(ReglementFormComponent);
  modalService = inject(NgbModal);
  confirmationService = inject(ConfirmationService);
  errorService = inject(ErrorService);
  reglementService = inject(ReglementService);
  factureService = inject(FactureService);
  protected showSidebar = false;
  protected partialPayment = false;
  protected readonly ModeEditionReglement = ModeEditionReglement;
  protected isSaving = false;

  constructor() {
    effect(() => {
      if (this.montantAPayer()) {
        this.reglementFormComponent()?.cashInput?.setValue(this.montantAPayer());
      } else {
        this.reglementFormComponent()?.cashInput?.setValue(this.montantAttendu);
      }
    });
  }

  get totalAmount(): number {
    if (!this.partialPayment) {
      return this.montantAttendu;
    }
    return this.montantAPayer();
  }

  private get montantAttendu(): number {
    return this.dossierFactureProjection?.montantTotal - this.dossierFactureProjection?.montantDetailRegle;
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
    this.reset(response);
  }

  private computeMontantRestant(d: ReglementFactureDossier): number {
    return d.montantTotal - d.montantPaye;
  }

  private getModeEditionReglement(): ModeEditionReglement {
    return this.partialPayment ? ModeEditionReglement.FACTURE_PARTIEL : ModeEditionReglement.FACTURE_TOTAL;
  }

  private buildReglementParams(params: ReglementParams): ReglementParams {
    return {
      ...params,
      mode: this.getModeEditionReglement(),
      dossierIds: this.dossierIds(),
    };
  }

  private reset(response: ResponseReglement): void {
    this.factureDossierSelectionnes.set([]);
    this.reglementFormComponent()?.reset();
    if (response.total) {
      this.dossierFactureProjection = null;
      this.reglementFactureDossiers = [];
      this.reglementFormComponent()?.cashInput?.setValue(null);
    } else {
      this.fetchFacture();
      this.reload(this.dossierFactureProjection?.id);
    }
  }

  private reload(id: number): void {
    this.factureService
      .findDossierReglement(id, 'individuelle', {
        page: 0,
        size: 999999,
      })
      .subscribe({
        next: (res: HttpResponse<ReglementFactureDossier[]>) => {
          this.reglementFactureDossiers = res.body;
          this.reglementFormComponent()?.cashInput?.setValue(this.montantAttendu);
        },
        error: (err: any) => {
          console.log(err);
          this.reglementFactureDossiers = [];
          this.dossierFactureProjection = null;
        },
      });
  }

  private fetchFacture(): void {
    this.factureService
      .findDossierFactureProjection(this.dossierFactureProjection?.id, {
        isGroup: false,
      })
      .subscribe(res => {
        this.dossierFactureProjection = res.body;
      });
  }
}