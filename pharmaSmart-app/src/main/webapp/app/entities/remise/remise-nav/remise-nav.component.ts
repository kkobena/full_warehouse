import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import { NgbNav, NgbNavContent, NgbNavItem, NgbNavLink, NgbNavLinkBase } from "@ng-bootstrap/ng-bootstrap";
import { FormsModule } from "@angular/forms";
import { RemiseProduitsComponent } from "../remise-produits/remise-produits.component";
import { CodeRemiseProduitComponent } from "../code-remise-produit/code-remise-produit.component";

import { NavSidebarComponent } from "app/shared/ui/nav-sidebar/nav-sidebar.component";
import { NavSectionLinkComponent } from "app/shared/ui/nav-sidebar/nav-section-link.component";

@Component({
  selector: "app-remise-nav",
  imports: [NavSidebarComponent, NavSectionLinkComponent,
    NgbNav,
    NgbNavContent,
    NgbNavItem,
    NgbNavLink,
    NgbNavLinkBase,
    FormsModule,
    RemiseProduitsComponent,
    CodeRemiseProduitComponent
  ],
  templateUrl: "./remise-nav.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ["./remise-nav.scss"]
})
export class RemiseNavComponent {
  protected active = "remise-produit";

  /** Menu replié : rend la largeur au contenu quand il en manque. */
  protected readonly menuReplie = signal(false);
}
