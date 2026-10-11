import { ChangeDetectionStrategy, Component, inject, input, output, TemplateRef, viewChild, ViewEncapsulation } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-modal',
  styleUrl: './modal.component.scss',
  encapsulation: ViewEncapsulation.None,
  template: `
    <ng-template #modalContent let-modal>
      @if (header()) {
        <div class="modal-header">
          <h4 class="modal-title">
            @if (icon()) {
              <i [class]="icon()"></i>
            }
            {{ header() }}
          </h4>
          @if (closable()) {
            <button
              type="button"
              class="btn-close"
              [attr.aria-label]="closeAriaLabel()"
              (click)="modal.dismiss()"
            ></button>
          }
        </div>
      }
      <div class="modal-body">
        <ng-content />
      </div>
      @if (hasFooter()) {
        <div class="modal-footer">
          <ng-content select="[footer]" />
        </div>
      }
    </ng-template>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent {
  private readonly modalService = inject(NgbModal);
  private modalRef?: NgbModalRef;

  readonly modalContent = viewChild<TemplateRef<unknown>>('modalContent');

  readonly header = input<string>('');
  /** Classe d'icône PrimeIcons affichée avant le titre, ex. "pi pi-plus-circle". */
  readonly icon = input<string>('');
  readonly closable = input<boolean>(true);
  readonly closeAriaLabel = input<string>('Fermer');
  readonly hasFooter = input<boolean>(false);
  readonly size = input<'sm' | 'lg' | 'xl'>('lg');
  readonly centered = input<boolean>(true);
  readonly scrollable = input<boolean>(false);
  /** Classe CSS supplémentaire sur le dialog (s'ajoute au thème interne). */
  readonly customClass = input<string>('');
  readonly backdrop = input<boolean | 'static'>(true);
  readonly keyboard = input<boolean>(true);

  readonly onHide = output<unknown>();
  readonly onShow = output<void>();

  open(): Promise<unknown> {
    const content = this.modalContent();
    if (!content) {
      throw new Error('Modal content template not found');
    }

    const dialogClass = ['pharma-modal-theme', this.customClass()].filter(Boolean).join(' ');

    this.modalRef = this.modalService.open(content, {
      size: this.size(),
      centered: this.centered(),
      scrollable: this.scrollable(),
      modalDialogClass: dialogClass,
      backdrop: this.backdrop(),
      keyboard: this.keyboard(),
    });

    this.onShow.emit();

    return this.modalRef.result.then(
      result => { this.onHide.emit(result); return result; },
      reason => { this.onHide.emit(reason); return Promise.reject(reason); },
    );
  }

  close(result?: unknown): void {
    this.modalRef?.close(result);
  }

  dismiss(reason?: unknown): void {
    this.modalRef?.dismiss(reason);
  }
}
