import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { TagModule } from "primeng/tag";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { IProduit } from "app/shared/model/produit.model";
import { ISubstitut } from "app/shared/model/substitut.model";
import { ProductsApiService } from "../../data-access/services/products-api.service";

@Component({
  selector: "app-produit-generiques-modal",
  templateUrl: "./produit-generiques-modal.component.html",
  styleUrls: ["./produit-generiques-modal.component.scss"],
  imports: [CommonModule, ButtonModule, TableModule, TagModule]
})
export class ProduitGeneriquesModalComponent implements OnInit {
  produit!: IProduit;

  protected substituts = signal<ISubstitut[]>([]);
  protected loading = signal(false);

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(ProductsApiService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getGeneriques(this.produit.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => { this.substituts.set(list); this.loading.set(false); },
        error: () => this.loading.set(false)
      });
  }

  protected stockSeverity(p: IProduit): "success" | "warn" | "danger" {
    const qty = p.totalQuantity ?? 0;
    if (qty <= 0) return "danger";
    if ((p.qtySeuilMini ?? 0) > 0 && qty < (p.qtySeuilMini ?? 0)) return "warn";
    return "success";
  }

  protected close(): void {
    this.activeModal.dismiss();
  }
}
