import { Component, inject, Input, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ButtonModule } from 'primeng/button';
import { Tag } from 'primeng/tag';

import { IDashboardLayout } from 'app/shared/model/dashboard-layout.model';

@Component({
  selector: 'jhi-load-layout-modal',
  imports: [CommonModule, ButtonModule, Tag],
  templateUrl: './load-layout-modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './load-layout-modal.component.scss',
})
export class LoadLayoutModalComponent {
  activeModal = inject(NgbActiveModal);

  @Input() layouts = signal<IDashboardLayout[]>([]);
  @Input() isLoading = signal<boolean>(false);

  dismiss(): void {
    this.activeModal.dismiss();
  }

  loadLayout(layout: IDashboardLayout): void {
    this.activeModal.close(layout);
  }

  deleteLayout(id: number): void {
    this.activeModal.close({ action: 'delete', id });
  }
}
