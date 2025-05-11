import { Component, inject, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-alert-info',
  templateUrl: './alert-info.component.html',
  imports: [NgbModule],
})
export class AlertInfoComponent implements OnInit {
  activeModal = inject(NgbActiveModal);

  message?: string;
  infoClass?: string;

  ngOnInit(): void {}

  cancel(): void {
    this.activeModal.dismiss();
  }
}
