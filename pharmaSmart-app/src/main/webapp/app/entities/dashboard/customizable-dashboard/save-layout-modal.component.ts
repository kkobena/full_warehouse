import { Component, inject, Input, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';

import { DashboardScope } from 'app/shared/model/dashboard-layout.model';

@Component({
  selector: 'jhi-save-layout-modal',
  imports: [CommonModule, FormsModule, ButtonModule, SelectModule, InputTextModule],
  templateUrl: './save-layout-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './save-layout-modal.component.scss',
})
export class SaveLayoutModalComponent {
  activeModal = inject(NgbActiveModal);

  layoutName = '';
  layoutDescription = '';
  layoutScope: DashboardScope = DashboardScope.PRIVATE;

  scopeOptions = [
    { label: 'Privé', value: DashboardScope.PRIVATE },
    { label: 'Partagé', value: DashboardScope.SHARED },
    { label: 'Public', value: DashboardScope.PUBLIC },
  ];

  dismiss(): void {
    this.activeModal.dismiss();
  }

  save(): void {
    if (!this.layoutName) {
      return;
    }

    this.activeModal.close({
      name: this.layoutName,
      description: this.layoutDescription,
      scope: this.layoutScope,
    });
  }
}
