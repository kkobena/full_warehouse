import {ChangeDetectionStrategy, Component, inject, OnInit} from "@angular/core";
import {ProduitService} from "../../produit/produit.service";
import {TableauProduitService} from "../tableau-produit.service";
import {IProduit} from "../../../shared/model";
import {HttpResponse} from "@angular/common/http";
import {ActivatedRoute} from "@angular/router";
import {ITableau} from "../../../shared/model/tableau.model";
import {FormsModule} from "@angular/forms";
import {CommonModule} from "@angular/common";
import {ButtonComponent, CardComponent, DataTableComponent, ToolbarComponent} from "../../../shared/ui";

@Component({
  selector: "app-produit-associes",
  templateUrl: "./produit-associes.component.html",
  styleUrls: ["./produit-associes.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [CommonModule, FormsModule, ButtonComponent, DataTableComponent, CardComponent, ToolbarComponent]
})
export class ProduitAssociesComponent implements OnInit {
  protected produitsSource: IProduit[] = [];
  protected produitsTarget: IProduit[] = [];
  protected statut = "ENABLE";
  protected searchSource: string;
  protected searchTarget: string;
  protected tableau: ITableau;
  private readonly produitService = inject(ProduitService);
  private readonly tableauProduitService = inject(TableauProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({tableau}) => {
      this.tableau = tableau;
    });
    this.fetchSource();
    this.fetchTarget();
  }

  protected fetchTarget(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 300,
        search: this.searchTarget || "",
        status: this.statut,
        tableauId: this.tableau.id
      })
      .subscribe({next: (res: HttpResponse<IProduit[]>) => (this.produitsTarget = res.body)});
  }

  protected previousState(): void {
    window.history.back();
  }

  protected fetchSource(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 300,
        search: this.searchSource || "",
        status: this.statut,
        tableauNot: this.tableau.id
      })
      .subscribe((res: HttpResponse<IProduit[]>) => (this.produitsSource = res.body));
  }

  protected moveToTarget(item: IProduit): void {
    this.tableauProduitService.associer(this.tableau.id, [item.id]).subscribe(() => {
      this.fetchSource();
      this.fetchTarget();
    });
  }

  protected moveToSource(item: IProduit): void {
    this.tableauProduitService.dissocier([item.id]).subscribe(() => {
      this.fetchSource();
      this.fetchTarget();
    });
  }

  protected moveAllToTarget(): void {
    const ids = this.produitsSource.map(p => p.id);
    if (!ids.length) {
      return;
    }
    this.tableauProduitService.associer(this.tableau.id, ids).subscribe(() => {
      this.fetchSource();
      this.fetchTarget();
    });
  }

  protected moveAllToSource(): void {
    const ids = this.produitsTarget.map(p => p.id);
    if (!ids.length) {
      return;
    }
    this.tableauProduitService.dissocier(ids).subscribe(() => {
      this.fetchSource();
      this.fetchTarget();
    });
  }

  protected onSourceFilter(): void {
    this.fetchSource();
  }

  protected onTargetFilter(): void {
    this.fetchTarget();
  }
}
