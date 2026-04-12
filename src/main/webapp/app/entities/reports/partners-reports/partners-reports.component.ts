import { Component, inject, signal } from '@angular/core';
import { AbilityService } from 'app/core/auth/ability.service';
import { CommonModule } from '@angular/common';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import CustomerSegmentationComponent from '../customer-segmentation/customer-segmentation.component';
import SupplierPerformanceComponent from '../supplier-performance/supplier-performance.component';

@Component({
  selector: 'jhi-partners-reports',
  imports: [CommonModule, NgbNavModule, CustomerSegmentationComponent, SupplierPerformanceComponent],
  templateUrl: './partners-reports.component.html',
  styleUrl: './partners-reports.component.scss',
})
export default class PartnersReportsComponent {
  active = signal<string>('customer-segmentation');

  private readonly ability = inject(AbilityService);

  protected readonly showCustomerSegmentation = this.ability.canSignal('display', 'rapport-partners.customer-segmentation');
  protected readonly showSupplierPerformance  = this.ability.canSignal('display', 'rapport-partners.supplier-performance');
}
