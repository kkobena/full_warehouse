import { Component, ChangeDetectionStrategy } from '@angular/core';
import { SalesHomeComponent } from '../sales-home/sales-home.component';

@Component({
  selector: 'app-devis-home',
  templateUrl: './devis-home.component.html',
  styleUrls: ['./devis-home.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [SalesHomeComponent],
})
export class DevisHomeComponent {}
