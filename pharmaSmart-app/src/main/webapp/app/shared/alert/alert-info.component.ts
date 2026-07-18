import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-alert-info',
  templateUrl: './alert-info.component.html',
  imports: [NgbModule, Card, Button],
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
