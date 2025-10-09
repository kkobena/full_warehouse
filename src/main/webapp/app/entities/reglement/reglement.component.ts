import { Component, inject, OnInit } from '@angular/core';
import { CardModule } from 'primeng/card';
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from '@ng-bootstrap/ng-bootstrap';
import { PanelModule } from 'primeng/panel';
import { FacturesRegleesComponent } from './factures-reglees/factures-reglees.component';
import { FaireReglementComponent } from './faire-reglement/faire-reglement.component';
import { FormsModule } from '@angular/forms';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { ActivatedRoute } from '@angular/router';
import { ReglementFactureDossier } from './model/reglement-facture-dossier.model';
import { Divider } from 'primeng/divider';

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
    Divider,
  ],
  templateUrl: './reglement.component.html',
  styleUrls: ['./reglement.component.scss'],
})
export class ReglementComponent implements OnInit {
  protected active = 'factures-reglees';
  protected reglementFactureDossiers: ReglementFactureDossier[] = [];
  protected isGroupe = false;
  private readonly activatedRoute = inject(ActivatedRoute);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ factureDossiers }) => {
      if (factureDossiers?.length > 0) {
        this.active = 'faire-reglement';
        this.reglementFactureDossiers = factureDossiers;
        this.isGroupe = factureDossiers[0].groupe;
      }
    });
  }
}
