import { AfterViewInit, Component, DestroyRef, inject, signal, viewChild } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { InputNumber, InputNumberModule } from "primeng/inputnumber";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { saveAs } from "file-saver";
import { IProduit } from "app/shared/model/produit.model";
import { ProductsApiService } from "../../data-access/services/products-api.service";
import { NotificationService } from "app/shared/services/notification.service";

@Component({
  selector: "app-produit-etiquette-modal",
  templateUrl: "./produit-etiquette-modal.component.html",
  styleUrls: ["./produit-etiquette-modal.component.scss"],
  imports: [CommonModule, FormsModule, ButtonModule, InputNumberModule]
})
export class ProduitEtiquetteModalComponent implements AfterViewInit{
  produit!: IProduit;

  protected qty = 1;
  protected startAt = 1;
  protected loading = signal(false);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(ProductsApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly qtyInput = viewChild.required<InputNumber>("qtyInput");
  ngAfterViewInit(): void {
    setTimeout(() => {
      const el = this.qtyInput()?.input;
      if (el) {
        el.nativeElement.focus();
        el.nativeElement.select();
      }

    }, 150);
  }
  protected onPrint(): void {
    if (this.qty < 1) return;
    this.loading.set(true);
    this.api.getEtiquettes(this.produit.id!, this.qty, this.startAt)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => {
          saveAs(blob, `etiquettes-${this.produit.codeCip ?? this.produit.id}.pdf`);
          this.loading.set(false);
          this.activeModal.close(true);
        },
        error: () => {
          this.notificationService.error("Erreur lors de la génération des étiquettes");
          this.loading.set(false);
        }
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
