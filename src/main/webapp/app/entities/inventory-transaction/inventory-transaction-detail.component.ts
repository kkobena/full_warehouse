import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';

@Component({
  selector: 'jhi-inventory-transaction-detail',
  templateUrl: './inventory-transaction-detail.component.html',
})
export class InventoryTransactionDetailComponent implements OnInit {
  inventoryTransaction: IInventoryTransaction | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ inventoryTransaction }) => (this.inventoryTransaction = inventoryTransaction));
  }

  previousState(): void {
    window.history.back();
  }
}
