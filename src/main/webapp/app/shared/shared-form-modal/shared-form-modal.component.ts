import { Component, inject, input, output } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'jhi-shared-form-modal',
  imports: [],
  template: `
    <div class="modal-header">
      <h4 class="modal-title">{{ header() }}</h4>

      <button (click)="cancelModal()" class="close" data-dismiss="modal" type="button">&times;</button>
    </div>
    <div class="ws-modal-body">
      <ng-content select="[ws-modal-body]"></ng-content>
    </div>
  `,
  styles: ``,
})
export class SharedFormModalComponent {
  header = input.required<string>();
  cancel = output<NgbActiveModal>();
  private readonly activeModal = inject(NgbActiveModal);

  cancelModal(): void {
    this.cancel.emit(this.activeModal);
  }
}
