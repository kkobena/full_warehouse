import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IStoreInventory } from 'app/shared/model/store-inventory.model';

@Component({
  selector: 'jhi-store-inventory-detail',
  templateUrl: './store-inventory-detail.component.html',
})
export class StoreInventoryDetailComponent implements OnInit {
  storeInventory: IStoreInventory | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ storeInventory }) => (this.storeInventory = storeInventory));
  }

  previousState(): void {
    window.history.back();
  }
}
