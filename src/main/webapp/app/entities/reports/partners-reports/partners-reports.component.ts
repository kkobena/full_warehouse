import { Component, signal } from '@angular/core';
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
}
