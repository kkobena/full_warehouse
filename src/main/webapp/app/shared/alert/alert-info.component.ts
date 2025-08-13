import { Component, inject, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { Card } from 'primeng/card';
import { Button } from 'primeng/button';

@Component({
  selector: 'jhi-alert-info',
  templateUrl: './alert-info.component.html',
  imports: [NgbModule, Card, Button],
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
