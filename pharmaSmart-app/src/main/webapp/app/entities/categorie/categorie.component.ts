import { Component, inject, OnInit, ChangeDetectionStrategy, signal } from "@angular/core";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";

import { ICategorie } from "app/shared/model/categorie.model";

import { ITEMS_PER_PAGE } from "app/shared/constants/pagination.constants";
import { CategorieService } from "./categorie.service";
import { CategorieDeleteDialogComponent } from "./categorie-delete-dialog.component";

import { RouterModule } from "@angular/router";
import TranslateDirective from "../../shared/language/translate.directive";
import { AlertErrorComponent } from "../../shared/alert/alert-error.component";
import { FaIconComponent } from "@fortawesome/angular-fontawesome";
import { AlertComponent } from "../../shared/alert/alert.component";

@Component({
  selector: "app-categorie",
  templateUrl: "./categorie.component.html",
  styleUrls: ["./categorie.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterModule, TranslateDirective, AlertErrorComponent, FaIconComponent, AlertComponent]
})
export class CategorieComponent implements OnInit {
  protected readonly categories = signal<ICategorie[] | undefined>(undefined);
  protected readonly itemsPerPage = signal<number | undefined>(undefined);
  protected readonly links = signal<any | undefined>(undefined);
  protected readonly page = signal<number | undefined>(undefined);
  protected readonly predicate = signal<string | undefined>(undefined);
  protected readonly ascending = signal<boolean | undefined>(undefined);
  protected categorieService = inject(CategorieService);
  protected modalService = inject(NgbModal);

  constructor() {
    this.categories.set([]);
    this.itemsPerPage.set(ITEMS_PER_PAGE);
    this.page.set(0);
    this.links.set({
      last: 0
    });
    this.predicate.set("id");
    this.ascending.set(true);
  }

  loadAll(): void {
    this.categorieService
      .query({
        sort: this.sort()
      })
      .subscribe((res: HttpResponse<ICategorie[]>) => this.paginateCategories(res.body, res.headers));
  }

  /** Vide la liste et la recharge depuis la première page. */
  reset(): void {
    this.page.set(0);
    this.categories.set([]);
    this.loadAll();
  }

  loadPage(page: number): void {
    this.page.set(page);
    this.loadAll();
  }

  ngOnInit(): void {
    this.loadAll();
  }

  trackId(index: number, item: ICategorie): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  delete(categorie: ICategorie): void {
    const modalRef = this.modalService.open(CategorieDeleteDialogComponent, {
      size: "lg",
      backdrop: "static"
    });
    modalRef.componentInstance.categorie = categorie;
    // `close()` = suppression confirmée, `dismiss()` = annulation. Seul le premier
    // chemin doit recharger ; sans ce `then`, la ligne supprimée restait affichée.
    modalRef.result.then(
      (): void => this.reset(),
      (): void => {
        // Modale annulée : rien à recharger.
      }
    );
  }

  sort(): string[] {
    const result = [this.predicate() + "," + (this.ascending() ? "asc" : "desc")];
    if (this.predicate() !== "id") {
      result.push("id");
    }
    return result;
  }

  protected paginateCategories(data: ICategorie[] | null, headers: HttpHeaders): void {
    this.categories.set(data || []);
  }
}
