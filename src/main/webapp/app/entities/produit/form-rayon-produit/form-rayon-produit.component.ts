import { Component, OnInit } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
  selector: 'jhi-form-rayon-produit',
  templateUrl: './form-rayon-produit.component.html',
  styleUrls: ['./form-rayon-produit.component.scss'],
  standalone: true,
  imports: [WarehouseCommonModule],
})
export class FormRayonProduitComponent implements OnInit {
  constructor() {}

  ngOnInit(): void {}
}
