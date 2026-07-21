import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, CardComponent } from '../ui';

@Component({
  selector: 'jhi-alert-info',
  templateUrl: './alert-info.component.html',
  imports: [NgbModule, ButtonComponent, CardComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['../../entities/common-modal.component.scss'],
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
