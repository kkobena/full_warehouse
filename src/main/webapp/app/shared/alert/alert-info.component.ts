import { Component, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-alert-info',
  templateUrl: './alert-info.component.html',
})
export class AlertInfoComponent implements OnInit {
  message?: string;
  infoClass?: string;
  constructor(public activeModal: NgbActiveModal) {}

  ngOnInit(): void {}
  cancel(): void {
    this.activeModal.dismiss();
  }
}
