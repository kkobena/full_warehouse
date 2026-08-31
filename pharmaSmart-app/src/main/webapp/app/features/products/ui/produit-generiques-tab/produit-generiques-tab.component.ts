import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  signal
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

import { BadgeComponent, DataTableComponent } from "app/shared/ui";
import { IProduit } from "app/shared/model/produit.model";
import { ISubstitut } from "app/shared/model/substitut.model";
import { ProductsApiService } from "../../data-access/services/products-api.service";
import { DevisePipe } from "app/shared/utils/devise";

/**
 * Onglet « Génériques » du panneau détail : les produits qui partagent la MOLÉCULE du produit
 * consulté, et peuvent donc le remplacer au comptoir.
 *
 * Cette liste vivait dans une fenêtre modale que rien n'ouvrait — aucune entrée de menu ne la
 * déclenchait. Elle est ici un onglet, à côté du stock et des rayons : c'est là qu'on la
 * cherche quand un client attend et que le princeps manque.
 */
@Component({
  selector: "app-produit-generiques-tab",
  templateUrl: "./produit-generiques-tab.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, DataTableComponent, BadgeComponent, DevisePipe]
})
export class ProduitGeneriquesTabComponent {
  readonly produit = input.required<IProduit>();

  protected readonly substituts = signal<ISubstitut[]>([]);
  protected readonly loading = signal(false);

  private readonly api = inject(ProductsApiService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    // Le panneau garde ses onglets montés d'un produit à l'autre : c'est le CHANGEMENT de
    // produit qui doit relancer la recherche, sinon l'onglet garderait les équivalents du
    // produit précédent — les plus trompeurs qui soient, puisqu'ils sont plausibles.
    effect(() => {
      const id = this.produit()?.id;
      if (!id) {
        this.substituts.set([]);
        return;
      }
      this.loading.set(true);
      this.api
        .getGeneriques(id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: list => {
            this.substituts.set(list);
            this.loading.set(false);
          },
          error: () => this.loading.set(false)
        });
    });
  }

  protected stockSeverity(p: IProduit): "success" | "warn" | "danger" {
    const qty = p.totalQuantity ?? 0;
    if (qty <= 0) {
      return "danger";
    }
    if ((p.qtySeuilMini ?? 0) > 0 && qty < (p.qtySeuilMini ?? 0)) {
      return "warn";
    }
    return "success";
  }
}
