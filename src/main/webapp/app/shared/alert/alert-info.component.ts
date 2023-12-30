import { Component, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'jhi-alert-info',
  templateUrl: './alert-info.component.html',
  standalone: true,
  imports: [CommonModule, NgbModule],
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
