import { Component, computed, Input, signal, viewChild } from '@angular/core';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';
import { ButtonModule } from 'primeng/button';
import { TableHeaderCheckbox, TableModule } from 'primeng/table';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SplitButtonModule } from 'primeng/splitbutton';
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap';
import { FieldsetModule } from 'primeng/fieldset';
import { SidebarModule } from 'primeng/sidebar';
import { DossierReglementInfoComponent } from '../dossier-reglement-info/dossier-reglement-info.component';
import { ReglementFormComponent } from '../reglement-form/reglement-form.component';
import { ModeEditionReglement, ReglementParams } from '../model/reglement.model';

@Component({
  selector: 'jhi-regelement-facture-individuelle',
  standalone: true,
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
  protected showSidebar = false;
  protected partialPayment = false;
  protected readonly ModeEditionReglement = ModeEditionReglement;

  openSideBar(): void {
    this.showSidebar = true;
  }

  protected onPartielReglement(evt: boolean): void {
    this.partialPayment = evt;
    this.factureDossierSelectionnes.set([]);
  }

  protected onSelectChange(event: any): void {
    this.factureDossierSelectionnes.set(event as ReglementFactureDossier[]);
  }

  protected selectAllClik(): void {
    console.log('ss');
    //this.selectionLength.emit(this.selections.length);
  }

  protected onSaveReglement(params: ReglementParams): void {
    console.log(params);
  }

  private computeMontantRestant(d: ReglementFactureDossier): number {
    return d.montantTotal - d.montantPaye;
  }
}
