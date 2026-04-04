import { Component } from '@angular/core';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { FacturationHomeComponent } from '../facturation-home/facturation-home.component';
import { HistoriqueReglementsComponent } from '../historique-reglements/historique-reglements.component';
import { FacturationEditionComponent } from '../facturation-edition/facturation-edition.component';

@Component({
  selector: 'app-facturation-layout',
  imports: [NgbNavModule, FacturationHomeComponent, HistoriqueReglementsComponent, FacturationEditionComponent],
  templateUrl: './facturation-layout.component.html',
  styleUrl: './facturation-layout.component.scss',
})
export class FacturationLayoutComponent {
  protected active = 'factures';
}
