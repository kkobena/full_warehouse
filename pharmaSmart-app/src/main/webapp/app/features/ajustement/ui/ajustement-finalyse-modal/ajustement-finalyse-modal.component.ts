import {AfterViewInit, Component, ElementRef, inject, viewChild, ChangeDetectionStrategy} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-ajustement-finalyse-modal',
  templateUrl: './ajustement-finalyse-modal.component.html',
  styleUrls: ['./ajustement-finalyse-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, ButtonModule, InputTextModule],
})
export class AjustementFinalyseModalComponent implements AfterViewInit {
  readonly activeModal = inject(NgbActiveModal);
  private commentaireInput = viewChild.required<ElementRef>('commentaireInput');
  protected commentaire = '';
  protected isSaving = false;

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.commentaireInput().nativeElement.focus();
    }, 100);
  }

  protected confirm(): void {
    if (!this.commentaire.trim()) return;
    this.activeModal.close(this.commentaire.trim());
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
