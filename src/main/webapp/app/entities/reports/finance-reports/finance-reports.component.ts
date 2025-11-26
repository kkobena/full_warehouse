import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import CashRegisterReportComponent from '../cash-register-report/cash-register-report.component';
import TiersPayantCreancesComponent from '../tiers-payant-creances/tiers-payant-creances.component';

@Component({
  selector: 'jhi-finance-reports',
  standalone: true,
  imports: [CommonModule, NgbNavModule, CashRegisterReportComponent, TiersPayantCreancesComponent],
  templateUrl: './finance-reports.component.html',
  styleUrl: './finance-reports.component.scss',
})
export default class FinanceReportsComponent {
  active = signal<string>('cash-register');
}
