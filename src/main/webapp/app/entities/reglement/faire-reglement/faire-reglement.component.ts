import { Component, inject, input, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { SidebarModule } from 'primeng/sidebar';
import { FaireGroupeReglementComponent } from '../faire-groupe-reglement/faire-groupe-reglement.component';
import { RegelementFactureIndividuelleComponent } from '../regelement-facture-individuelle/regelement-facture-individuelle.component';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';
import { SelectedFacture } from '../model/reglement.model';
import { FactureService } from '../../facturation/facture.service';
import { HttpResponse } from '@angular/common/http';

@Component({
  selector: 'jhi-faire-reglement',
  imports: [SidebarModule, FaireGroupeReglementComponent, RegelementFactureIndividuelleComponent],
  templateUrl: './faire-reglement.component.html',
  styleUrls: ['./faire-reglement.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class FaireReglementComponent implements OnInit {
  readonly isGroupe = input(false);
  readonly isGroupeSignal = signal(this.isGroupe());
  readonly reglementFactureDossiers = input<ReglementFactureDossier[]>([]);
  readonly dossierFactureProjection = input<DossierFactureProjection | null>(null);
  factureService = inject(FactureService);
  protected reglementFactureDossiersSignal = signal(this.reglementFactureDossiers());
  protected dossierFactureProjectionSignal = signal(this.dossierFactureProjection());

  constructor() {}

  onSelectFacture(facture: SelectedFacture) {
    if (facture) {
      this.isGroupeSignal.set(facture.isGroup);
      this.fetchFacture(facture);
      const path = this.isGroupe() ? 'groupes' : 'individuelle';
      this.reload(facture.facture?.factureId, path);
    }
  }

  ngOnInit(): void {
    this.isGroupeSignal.set(this.isGroupe());
    this.dossierFactureProjectionSignal.set(this.dossierFactureProjection());
    this.reglementFactureDossiersSignal.set(this.reglementFactureDossiers());
  }

  private fetchFacture(facture: SelectedFacture): void {
    this.factureService
      .findDossierFactureProjection(facture?.facture?.factureId, {
        isGroup: facture.isGroup,
      })
      .subscribe(res => {
        this.dossierFactureProjectionSignal.set(res.body);
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
          this.reglementFactureDossiersSignal.set(res.body);
        },
        error: () => {
          this.reglementFactureDossiersSignal.set([]);
          this.dossierFactureProjectionSignal.set(null);
        },
      });
  }
}
