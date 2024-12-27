import { Component, inject, Input, ViewEncapsulation } from '@angular/core';
import { SidebarModule } from 'primeng/sidebar';
import { FaireGroupeReglementComponent } from '../faire-groupe-reglement/faire-groupe-reglement.component';
import { RegelementFactureIndividuelleComponent } from '../regelement-facture-individuelle/regelement-facture-individuelle.component';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';
import { SelectedFacture } from '../model/reglement.model';
import { FactureService } from '../../facturation/facture.service';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-faire-reglement',
  standalone: true,
  imports: [SidebarModule, FaireGroupeReglementComponent, RegelementFactureIndividuelleComponent],
  templateUrl: './faire-reglement.component.html',

  styleUrls: ['./faire-reglement.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class FaireReglementComponent {
  @Input() isGroupe = false;
  @Input() reglementFactureDossiers: ReglementFactureDossier[] = [];
  @Input() dossierFactureProjection: DossierFactureProjection | null = null;
  factureService = inject(FactureService);

  constructor() {}

  onSelectFacture(facture: SelectedFacture) {
    if (facture) {
      this.isGroupe = facture.isGroup;
      this.fetchFacture(facture);
      const path = this.isGroupe ? 'groupes' : 'individuelle';
      this.reload(facture.facture?.factureId, path);
    }
  }

  private fetchFacture(facture: SelectedFacture): void {
    this.factureService
      .findDossierFactureProjection(facture?.facture?.factureId, {
        isGroup: facture.isGroup,
      })
      .subscribe(res => {
        this.dossierFactureProjection = res.body;
      });
  }

  private reload(id: number, path: string): void {
    this.factureService
      .findDossierReglement(id, path, {
        page: 0,
        size: 999999,
      })
      .subscribe({
        next: (res: HttpResponse<ReglementFactureDossier[]>) => {
          this.reglementFactureDossiers = res.body;
        },
        error: () => {
          this.reglementFactureDossiers = [];
          this.dossierFactureProjection = null;
        },
      });
  }
}
