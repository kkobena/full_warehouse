import { Component } from '@angular/core';
import { SalesService } from '../sales/sales.service';
import { MvtCaisseServiceService } from './mvt-caisse-service.service';

@Component({
  selector: 'jhi-balance-mvt-caisse',
  standalone: true,
  imports: [],
  templateUrl: './balance-mvt-caisse.component.html',
  styleUrl: './balance-mvt-caisse.component.scss',
})
export class BalanceMvtCaisseComponent {
  constructor(
    protected salesSerice: SalesService,
    protected mvtCaisseService: MvtCaisseServiceService,
  ) {}
}
