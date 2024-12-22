import { Component, computed, Input, signal, viewChild } from '@angular/core';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';
import { TableHeaderCheckbox, TableModule } from 'primeng/table';
import { LigneSelectionnes, ModeEditionReglement, ReglementParams } from '../model/reglement.model';
import { ButtonModule } from 'primeng/button';
import { DossierReglementInfoComponent } from '../dossier-reglement-info/dossier-reglement-info.component';
import { FieldsetModule } from 'primeng/fieldset';
import { InputTextModule } from 'primeng/inputtext';
import { ReglementFormComponent } from '../reglement-form/reglement-form.component';
import { SidebarModule } from 'primeng/sidebar';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SplitButtonModule } from 'primeng/splitbutton';
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'jhi-faire-groupe-reglement',
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
    FormsModule,
  ],
  templateUrl: './faire-groupe-reglement.component.html',
})
export class FaireGroupeReglementComponent {
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
  protected isSaving = false;
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

  protected onMontantVerseChange(item: ReglementFactureDossier): void {
    this.factureDossierSelectionnes.set(this.factureDossierSelectionnes().map(d => (d.id === item.id ? item : d)));
  }

  protected onSaveReglement(params: ReglementParams): void {
    // this.isSaving = true;
    console.log(params);
    const reglementParams = this.buildReglementParams(params);
    console.log(reglementParams);
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
    console.log(this.partialPayment);
    return this.partialPayment ? ModeEditionReglement.GROUPE_PARTIEL : ModeEditionReglement.GROUPE_TOTAL;
  }

  private buildLigneSelectionnes(): LigneSelectionnes[] {
    if (this.partialPayment) {
      return this.factureDossierSelectionnes().map(d => {
        return {
          id: d.id,
          montantAttendu: d.montantTotal - d.montantPaye,
          montantVerse: d.montantVerse,
        };
      });
    }
    return [];
  }
}
