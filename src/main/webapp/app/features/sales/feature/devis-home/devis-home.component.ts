import { Component } from '@angular/core';
import { SalesHomeComponent } from '../sales-home/sales-home.component';

@Component({
  selector: 'app-devis-home',
  templateUrl: './devis-home.component.html',
  styleUrls: ['./devis-home.component.scss'],
  imports: [SalesHomeComponent],
})
export class DevisHomeComponent {}
