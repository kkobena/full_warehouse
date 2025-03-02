import { Component, OnInit, inject } from '@angular/core';
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

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {}

  cancel(): void {
    this.activeModal.dismiss();
  }
}
