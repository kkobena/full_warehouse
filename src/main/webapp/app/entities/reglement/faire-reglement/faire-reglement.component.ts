import { Component, Input, ViewEncapsulation } from '@angular/core';
import { SidebarModule } from 'primeng/sidebar';
import { FaireGroupeReglementComponent } from '../faire-groupe-reglement/faire-groupe-reglement.component';
import { RegelementFactureIndividuelleComponent } from '../regelement-facture-individuelle/regelement-facture-individuelle.component';
import { DossierFactureProjection, ReglementFactureDossier } from '../model/reglement-facture-dossier.model';

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
  protected showSidebar = false;

  constructor() {}

  closeSideBar(booleanValue: boolean): void {
    this.showSidebar = booleanValue;
  }

  openSideBar(): void {
    this.showSidebar = true;
  }
}
