import { Component, ChangeDetectionStrategy } from '@angular/core';
import { SalesHomeComponent } from '../sales-home/sales-home.component';

@Component({
  selector: 'app-presale-home',
  templateUrl: './presale-home.component.html',
  styleUrls: ['./presale-home.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [SalesHomeComponent],
})
export class PresaleHomeComponent {}
