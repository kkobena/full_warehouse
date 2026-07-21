import {AfterViewInit, Component, DestroyRef, ElementRef, inject, OnInit, viewChild, ChangeDetectionStrategy} from '@angular/core';
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {NgxSpinnerService} from "ngx-spinner";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {ISales} from "../../../../shared/model";
import {SalesApiService} from "../../data-access/services/sales-api.service";
import {NotificationService} from "../../../../shared/services/notification.service";
import {ErrorService} from "../../../../shared/error.service";
import {Button} from "primeng/button";
import {Card} from "primeng/card";
import {InputText} from "primeng/inputtext";

@Component({
  selector: 'app-annulation-vente-message',
  imports: [
    Button,
    Card,
    FormsModule,
    InputText,
    ReactiveFormsModule
  ],
  templateUrl: './annulation-vente-message.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './annulation-vente-message.component.scss',
})
export class AnnulationVenteMessageComponent implements OnInit, AfterViewInit {
  readonly activeModal = inject(NgbActiveModal);
  form!: FormGroup;
  sale: ISales;
  protected isSaving = false;
  private readonly api = inject(SalesApiService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly fb = inject(FormBuilder);
  protected cancelCommentInput = viewChild.required<ElementRef>('cancelComment');
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.buildForm();

  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.cancelCommentInput().nativeElement.focus();
    }, 100);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const cancelComment = this.form.getRawValue().cancelComment;

    this.isSaving = true;
    this.spinner.show();

    const updateObservable$ =
      this.sale?.natureVente === 'COMPTANT' ? this.api.cancelComptant(this.sale?.saleId, cancelComment) : this.api.cancelAssurance(this.sale?.saleId, cancelComment);
    updateObservable$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: resp => {
          this.spinner.hide();
          this.isSaving = false;
          this.activeModal.close(resp.body);
        },
        error: err => {
          this.spinner.hide();
          this.isSaving = false;
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Annulation de vente échouée');
        },
      });
  }

  cancel(): void {
    this.activeModal.dismiss();
  }


  private buildForm(): void {
    this.form = this.fb.group({
      cancelComment: [null, Validators.required]

    });
  }


}
