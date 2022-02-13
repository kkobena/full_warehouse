import { Component, OnInit } from '@angular/core';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

@Component({
  selector: 'jhi-uninsured-customer-list',
  templateUrl: './uninsured-customer-list.component.html',
  styleUrls: ['./uninsured-customer-list.component.scss'],
})
export class UninsuredCustomerListComponent implements OnInit {
  constructor(public ref: DynamicDialogRef) {}

  ngOnInit(): void {}
  cancel(): void {
    this.ref.close();
  }
}
