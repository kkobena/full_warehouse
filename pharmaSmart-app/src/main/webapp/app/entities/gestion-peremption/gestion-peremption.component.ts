import { Component, inject, OnInit, signal } from "@angular/core";
import { AbilityService } from "app/core/auth/ability.service";
import {
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLink,
  NgbNavLinkBase,
  NgbNavOutlet
} from "@ng-bootstrap/ng-bootstrap";
import { TranslatePipe } from "@ngx-translate/core";
import { LotPerimesComponent } from "./lot-perimes/lot-perimes.component";
import { LotADetruireComponent } from "./lot-a-detruire/lot-a-detruire.component";
import { LotService } from "../commande/lot/lot.service";
import { ProductToDestroyService } from "./product-to-destroy.service";
import { LotFilterParam } from "./model/lot-perimes";
import { ProductToDestroyFilter } from "./model/product-to-destroy";
import { PeremptionAlertService } from "../../shared/services/peremption-alert.service";
import { RouterLink } from "@angular/router";
import { CommonModule } from "@angular/common";

@Component({
  selector: "jhi-gestion-peremption",
  imports: [
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    TranslatePipe,
    LotPerimesComponent,
    LotADetruireComponent,
    CommonModule,
    RouterLink,
    NgbNavOutlet
  ],
  templateUrl: "./gestion-peremption.component.html",
  styleUrls: ["./gestion-peremption.scss"]
})
export class GestionPeremptionComponent implements OnInit {
  protected active = "lot-perimes";
  protected lotPerimesCount = 0;
  protected lotADetruireCount = 0;
  protected alertDismissed = signal(true);

  protected readonly peremptionAlertService = inject(PeremptionAlertService);
  private readonly ability = inject(AbilityService);

  protected readonly showLotPerimes    = this.ability.canSignal('display', 'peremptions.lot-perimes');
  protected readonly showLotADetruire  = this.ability.canSignal('display', 'peremptions.lot-a-detruire');
  private readonly lotService = inject(LotService);
  private readonly productToDestroyService = inject(ProductToDestroyService);

  ngOnInit(): void {
    this.loadCounts();
  }

  protected dismissAlert(): void {
    this.alertDismissed.set(true);
  }

  protected loadCounts(): void {
    this.lotService.getSum({} as LotFilterParam).subscribe({
      next: res => {
        console.log(res.body);
        this.lotPerimesCount = res.body?.count ?? 0;
      },
      error: () => {
        this.lotPerimesCount = 0;
      }
    });
    this.productToDestroyService.getSum({ destroyed: false, editing: false } as ProductToDestroyFilter).subscribe({
      next: res => {
        this.lotADetruireCount = res.body?.productCount ?? 0;
      },
      error: () => {
        this.lotADetruireCount = 0;
      }
    });
  }
}
