import { Component, OnInit } from '@angular/core';
import { ProduitService } from '../../produit/produit.service';
import { TableauProduitService } from '../tableau-produit.service';
import { IProduit } from '../../../shared/model/produit.model';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { ITableau } from '../../../shared/model/tableau.model';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PickListModule } from 'primeng/picklist';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';

@Component({
  selector: 'jhi-produit-associes',
  templateUrl: './produit-associes.component.html',
  imports: [WarehouseCommonModule, FormsModule, PickListModule, ToolbarModule, ButtonModule, InputTextModule, ButtonModule, DividerModule],
})
export class ProduitAssociesComponent implements OnInit {
  produitsSource: IProduit[] = [];
  produitsTarget: IProduit[] = [];
  statut: string = 'ENABLE';
  searchSource: string;
  searchTarget: string;
  tableau: ITableau;
  protected scrollHeight = 'calc(100vh - 350px)';

  constructor(
    protected produitService: ProduitService,
    private tableauProduitService: TableauProduitService,
    protected activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ tableau }) => {
      this.tableau = tableau;
    });
    this.fetchSource();
    this.fetchTarget();
  }

  fetchTarget(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 50,
        search: this.searchTarget || '',
        status: this.statut,
        tableauId: this.tableau.id,
      })
      .subscribe({ next: (res: HttpResponse<IProduit[]>) => (this.produitsTarget = res.body) });
  }

  previousState(): void {
    window.history.back();
  }

  fetchSource(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 50,
        search: this.searchSource || '',
        status: this.statut,
        tableauNot: this.tableau.id,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => (this.produitsSource = res.body));
  }

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id;
  }

  moveToTarget(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.associer(this.tableau.id, ids).subscribe(() => this.fetchTarget());
  }

  moveToSource(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.dissocier(ids).subscribe(() => this.fetchSource());
  }

  moveAllToTarget(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.associer(this.tableau.id, ids).subscribe(() => this.fetchTarget());
  }

  moveAllToSource(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.dissocier(ids).subscribe(() => this.fetchSource());
  }

  sourceFilter(): void {
    this.fetchSource();
  }

  targeFilter(): void {
    this.fetchTarget();
  }
}
