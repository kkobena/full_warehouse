import { Component, ElementRef, inject, Renderer2 } from "@angular/core";
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";

import { ISales } from "app/shared/model/sales.model";
import { SalesService } from "../sales.service";
import { ButtonModule } from "primeng/button";
import { Observable } from "rxjs";
import { HttpResponse } from "@angular/common/http";
import { finalize } from "rxjs/operators";
import dayjs from "dayjs/esm";
import { TagModule } from "primeng/tag";
import { Card } from "primeng/card";
import { DatePicker } from "primeng/datepicker";
import { Toast } from "primeng/toast";
import { NotificationService } from "../../../shared/services/notification.service";

@Component({
  selector: "app-sale-update-date-modal",
  templateUrl: "./sale-update-date-modal.component.html",
  styleUrls: ["./sale-update-date-modal.component.scss"],
  imports: [ReactiveFormsModule, ButtonModule, TagModule, Card, DatePicker, Toast]
})
export class SaleUpdateDateModalComponent {
  sale: ISales | null = null; // /*const modalData = (this as any).sale;
  protected activeModal = inject(NgbActiveModal);
  protected fb = inject(FormBuilder);
  private readonly renderer = inject(Renderer2);
  private readonly elementRef = inject(ElementRef);
  protected isSaving = false;

  protected editForm = this.fb.group({
    updatedAt: new FormControl<Date | null>(null, {
      validators: [Validators.required],
      nonNullable: true
    })
  });
  private readonly salesService = inject(SalesService);
  private readonly notificationService = inject(NotificationService);

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  protected save(): void {
    this.isSaving = true;
    const formDate = this.editForm.get("updatedAt")?.value;
    const updatedSale = { ...this.sale, updatedAt: formDate ? dayjs(formDate).toISOString() : undefined };
    this.subscribeToSaveResponse(this.salesService.updateDate(updatedSale as ISales));
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.pipe(finalize(() => (this.isSaving = false))).subscribe({
      next: res => this.onSaveSuccess(res.body),
      error: () => this.onSaveError()
    });
  }

  protected onSaveSuccess(updatedSale: ISales | null): void {
    this.activeModal.close(updatedSale);
  }

  protected onDropdownShow(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.addClass(modalBody, "overflow-visible");
    }
  }

  protected onDropdownHide(event: any): void {
    const modalBody = this.elementRef.nativeElement.querySelector(".modal-body");
    if (modalBody) {
      this.renderer.removeClass(modalBody, "overflow-visible");
    }
  }

  protected onSaveError(): void {
    this.notificationService.error("Erreur", "La mise à jour de la date a échoué.");
  }
}
