import { Component, ChangeDetectionStrategy, input, output, inject, TemplateRef, viewChild } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';

/**
 * Reusable modal/dialog component wrapping NgBootstrap modal
 * 
 * @example
 * // In component:
 * modalService = inject(NgbModal);
 * 
 * openModal(content: TemplateRef<any>) {
 *   this.modalService.open(content, { size: 'lg', centered: true });
 * }
 * 
 * // In template:
 * <ng-template #content let-modal>
 *   <div class="modal-header">
 *     <h4 class="modal-title">Confirm Action</h4>
 *     <button type="button" class="btn-close" (click)="modal.dismiss()"></button>
 *   </div>
 *   <div class="modal-body">
 *     <p>Are you sure you want to continue?</p>
 *   </div>
 *   <div class="modal-footer">
 *     <button type="button" class="btn btn-secondary" (click)="modal.close('cancel')">Cancel</button>
 *     <button type="button" class="btn btn-primary" (click)="modal.close('confirm')">Confirm</button>
 *   </div>
 * </ng-template>
 * 
 * <button (click)="openModal(content)">Open Modal</button>
 */
@Component({
  selector: 'app-modal',
  imports: [CommonModule],
  template: `
    <ng-template #modalContent let-modal>
      @if (header()) {
        <div class="modal-header">
          <h4 class="modal-title">{{ header() }}</h4>
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
  private modalService = inject(NgbModal);
  private modalRef?: NgbModalRef;
  
  modalContent = viewChild<TemplateRef<any>>('modalContent');
  
  /** Dialog header text */
  header = input<string>('');
  
  /** Show close button */
  closable = input<boolean>(true);
  
  /** Close button aria label */
  closeAriaLabel = input<string>('Close');
  
  /** Has footer content */
  hasFooter = input<boolean>(false);
  
  /** Modal size */
  size = input<'sm' | 'lg' | 'xl'>('lg');
  
  /** Center modal vertically */
  centered = input<boolean>(false);
  
  /** Allow scrolling inside modal body */
  scrollable = input<boolean>(false);
  
  /** Custom CSS class */
  customClass = input<string>('');
  
  /** Backdrop configuration */
  backdrop = input<boolean | 'static'>(true);
  
  /** Close on ESC key */
  keyboard = input<boolean>(true);
  
  /** Hide event */
  onHide = output<any>();
  
  /** Show event */
  onShow = output<void>();
  
  /**
   * Open the modal programmatically
   */
  open(): Promise<any> {
    const content = this.modalContent();
    if (!content) {
      throw new Error('Modal content template not found');
    }
    
    this.modalRef = this.modalService.open(content, {
      size: this.size(),
      centered: this.centered(),
      scrollable: this.scrollable(),
      modalDialogClass: this.customClass(),
      backdrop: this.backdrop(),
      keyboard: this.keyboard(),
    });
    
    this.onShow.emit();
    
    return this.modalRef.result
      .then(
        (result) => {
          this.onHide.emit(result);
          return result;
        },
        (reason) => {
          this.onHide.emit(reason);
          return Promise.reject(reason);
        }
      );
  }
  
  /**
   * Close the modal programmatically
   */
  close(result?: any): void {
    if (this.modalRef) {
      this.modalRef.close(result);
    }
  }
  
  /**
   * Dismiss the modal programmatically
   */
  dismiss(reason?: any): void {
    if (this.modalRef) {
      this.modalRef.dismiss(reason);
    }
  }
}
