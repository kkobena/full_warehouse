import { Component, computed, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';
import { TableHeaderCheckbox, TableModule } from 'primeng/table';
import { LigneSelectionnes, ModeEditionReglement, ReglementParams, ResponseReglement, SelectedFacture } from '../model/reglement.model';
import { ButtonModule } from 'primeng/button';
import { DossierReglementInfoComponent } from '../dossier-reglement-info/dossier-reglement-info.component';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { ReglementFormComponent } from '../reglement-form/reglement-form.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SplitButtonModule } from 'primeng/splitbutton';
import { NgbAlertModule, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { ReglementService } from '../reglement.service';
import { ConfirmationService } from 'primeng/api';
import { ErrorService } from '../../../shared/error.service';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { FactureService } from '../../facturation/facture.service';
import { HttpResponse } from '@angular/common/http';
import { FactuesModalComponent } from '../factues-modal/factues-modal.component';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { Drawer } from 'primeng/drawer';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

@Component({
  selector: 'jhi-faire-groupe-reglement',
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
    DossierReglementInfoComponent,
    ReglementFormComponent,
    FormsModule,
    FactuesModalComponent,
    Drawer,
    IconField,
    InputIcon,
  ],
  templateUrl: './faire-groupe-reglement.component.html',
})
export class FaireGroupeReglementComponent implements OnInit {
  readonly reglementFactureDossiers = input<ReglementFactureDossier[]>([]);
  readonly dossierFactureProjection = input<DossierFactureProjection | null>(null);
  dossierFactureProjectionWritable = signal(this.dossierFactureProjection());
  reglementFactureDossiersWritable = signal(this.reglementFactureDossiers());
  checkbox = viewChild<TableHeaderCheckbox>('checkbox');
  factureDossierSelectionnes = signal<ReglementFactureDossier[]>([]);
  montantAPayer = computed(() => {
    return this.factureDossierSelectionnes()?.reduce((acc, d) => acc + this.computeMontantRestant(d), 0) || 0;
  });
  dossierIds = computed(() => {
    return this.factureDossierSelectionnes()?.map(d => d.id) || [];
  });
  reglementFormComponent = viewChild(ReglementFormComponent);
  protected readonly reglementService = inject(ReglementService);
  protected readonly modalService = inject(NgbModal);
  protected readonly confirmationService = inject(ConfirmationService);
  protected readonly errorService = inject(ErrorService);
  protected readonly factureService = inject(FactureService);
  readonly selectedFacture = output<SelectedFacture>();
  protected showSidebar = false;
  protected partialPayment = false;
  protected isSaving = false;
  protected readonly ModeEditionReglement = ModeEditionReglement;

  constructor() {
    if (this.dossierFactureProjection()) {
      this.dossierFactureProjectionWritable.set(this.dossierFactureProjection());
    }
  }
  get drawerWidth(): {} {
    return window.innerWidth <= 1280 ? { width: '85vw' } : { width: '70vw' };
  }
  openSideBar(): void {
    this.showSidebar = true;
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  onError(error: any): void {
    this.isSaving = false;
    this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
  }

  onSelectFacture(facture: SelectedFacture): void {
    this.selectedFacture.emit(facture);
    this.showSidebar = false;
  }

  ngOnInit(): void {
    this.reglementFactureDossiersWritable.set(this.reglementFactureDossiers());
  }

  protected onPartielReglement(evt: boolean): void {
    this.partialPayment = evt;
    this.factureDossierSelectionnes().forEach(d => {
      this.reglementFactureDossiersWritable().find(r => r.id === d.id).montantVerse = d.montantTotal - d.montantDetailRegle;
    });

    this.factureDossierSelectionnes.set([]);
  }

  protected onSelectChange(event: any): void {
    this.factureDossierSelectionnes.set(event as ReglementFactureDossier[]);
  }

  protected onMontantVerseChange(item: ReglementFactureDossier): void {
    this.factureDossierSelectionnes.set(this.factureDossierSelectionnes().map(d => (d.id === item.id ? item : d)));
  }

  protected onSaveReglement(params: ReglementParams): void {
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

  protected isInputEditable(item: ReglementFactureDossier): boolean {
    return this.partialPayment && this.factureDossierSelectionnes().some(r => r.id === item.id);
  }

  private onPrintReceipt(response: ResponseReglement): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous imprimer le ticket ?',
      header: 'TICKET REGLEMENT',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => {
        this.reglementService.printReceipt(response.id).subscribe();
        this.reset(response);
      },
      reject: () => this.reset(response),
      key: 'printReceipt',
    });
  }

  private computeMontantRestant(d: ReglementFactureDossier): number {
    return d.montantVerse;
  }

  private buildReglementParams(params: ReglementParams): ReglementParams {
    return {
      ...params,
      mode: this.getModeEditionReglement(),
      ligneSelectionnes: this.buildLigneSelectionnes(),
    };
  }

  private getModeEditionReglement(): ModeEditionReglement {
    return this.partialPayment ? ModeEditionReglement.GROUPE_PARTIEL : ModeEditionReglement.GROUPE_TOTAL;
  }

  private buildLigneSelectionnes(): LigneSelectionnes[] {
    if (this.partialPayment) {
      return this.factureDossierSelectionnes().map(d => {
        return {
          id: d.id,
          montantAttendu: d.montantTotal - d.montantPaye,
          montantVerse: d.montantVerse,
          montantFacture: this.dossierFactureProjectionWritable().montantTotal,
        };
      });
    }
    return [];
  }

  private reset(response: ResponseReglement): void {
    this.factureDossierSelectionnes.set([]);
    this.reglementFormComponent().reset();
    if (response.total) {
      this.dossierFactureProjectionWritable.set(null);
      this.reglementFactureDossiersWritable.set([]);
      this.reglementFormComponent().cashInput.setValue(null);
    } else {
      this.fetchFacture();
      this.reload(this.dossierFactureProjectionWritable().id);
    }
  }

  private reload(id: number): void {
    this.factureService
      .findDossierReglement(id, 'groupes', {
        page: 0,
        size: 999999,
      })
      .subscribe({
        next: (res: HttpResponse<ReglementFactureDossier[]>) => {
          this.reglementFactureDossiersWritable.set(res.body);
        },
        error: () => {
          this.reglementFactureDossiersWritable.set([]);
          this.dossierFactureProjectionWritable.set(null);
        },
      });
  }

  private fetchFacture(): void {
    this.factureService
      .findDossierFactureProjection(this.dossierFactureProjectionWritable().id, {
        isGroup: true,
      })
      .subscribe(res => {
        this.dossierFactureProjectionWritable.set(res.body);
      });
  }
}
