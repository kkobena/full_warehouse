import { Component, inject, OnInit } from '@angular/core';
import { CardModule } from 'primeng/card';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';
import { PanelModule } from 'primeng/panel';
import { FacturesRegleesComponent } from './factures-reglees/factures-reglees.component';
import { FaireReglementComponent } from './faire-reglement/faire-reglement.component';
import { FormsModule } from '@angular/forms';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ActivatedRoute } from '@angular/router';
import { DossierFactureProjection, ReglementFactureDossier } from './model/reglement-facture-dossier.model';
import { FactureService } from '../facturation/facture.service';

@Component({
    selector: 'jhi-reglement',
    imports: [
        WarehouseCommonModule,
        CardModule,
        FacturesRegleesComponent,
        FaireReglementComponent,
        NgbNav,
        NgbNavContent,
        NgbNavItem,
        NgbNavLink,
        NgbNavLinkBase,
        PanelModule,
        FormsModule,
    ],
    templateUrl: './reglement.component.html',
    styleUrl: './reglement.component.scss'
})
export class ReglementComponent implements OnInit {
  activatedRoute = inject(ActivatedRoute);
  factureService = inject(FactureService);
  protected active = 'factures-reglees';
  protected reglementFactureDossiers: ReglementFactureDossier[] = [];
  protected dossierFactureProjection: DossierFactureProjection | null = null;
  protected isGroupe = false;

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ factureDossiers }) => {
      if (factureDossiers?.length > 0) {
        this.active = 'faire-reglement';
        this.reglementFactureDossiers = factureDossiers;
        this.isGroupe = factureDossiers[0].groupe;
        this.factureService
          .findDossierFactureProjection(factureDossiers[0].parentId, {
            isGroup: this.isGroupe,
          })
          .subscribe(res => {
            this.dossierFactureProjection = res.body;
          });
      }
    });
  }
}
