import { Component, OnInit } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

@Component({
    selector: 'jhi-detail-produit-form',
    templateUrl: './detail-produit-form.component.html',
    styleUrls: ['./detail-produit-form.component.scss'],
    imports: [WarehouseCommonModule]
})
export class DetailProduitFormComponent implements OnInit {
  constructor() {}

  ngOnInit(): void {}
}
