import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { AbilityService } from "app/core/auth/ability.service";
import { CommonModule } from "@angular/common";
import { NgbNavModule } from "@ng-bootstrap/ng-bootstrap";

import CustomerSegmentationComponent from "../customer-segmentation/customer-segmentation.component";
import SupplierPerformanceComponent from "../supplier-performance/supplier-performance.component";

import { NavSidebarComponent } from 'app/shared/ui/nav-sidebar/nav-sidebar.component';
import { NavSectionLinkComponent } from 'app/shared/ui/nav-sidebar/nav-section-link.component';
@Component({
  selector: "jhi-partners-reports",
  imports: [NavSidebarComponent, NavSectionLinkComponent, CommonModule, NgbNavModule, CustomerSegmentationComponent, SupplierPerformanceComponent],
  templateUrl: "./partners-reports.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./partners-reports.component.scss"
})
export default class PartnersReportsComponent implements OnInit {
  active = signal<string>("customer-segmentation");


  /** Menu replié : ces rapports affichent de larges tableaux et graphiques. */
  protected readonly menuReplie = signal(false);
  private readonly ability = inject(AbilityService);

  protected readonly showCustomerSegmentation = this.ability.canSignal("display", "rapport-partners.customer-segmentation");
  protected readonly showSupplierPerformance = this.ability.canSignal("display", "rapport-partners.supplier-performance");

  ngOnInit(): void {
    if (this.active() === "customer-segmentation" && !this.showCustomerSegmentation()) {
      if (this.showSupplierPerformance()) {
        this.active.set("supplier-performance");
      } else {

        //TODO: handle no access to any tab ajout d'un tab commun Access denied
      }

    }
  }
}
