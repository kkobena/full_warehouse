import { Component, inject, OnInit } from '@angular/core';
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

@Component({
  selector: 'jhi-produit-associes',
  templateUrl: './produit-associes.component.html',
  imports: [WarehouseCommonModule, FormsModule, PickListModule, ToolbarModule, ButtonModule, InputTextModule, ButtonModule],
})
export class ProduitAssociesComponent implements OnInit {
  protected produitsSource: IProduit[] = [];
  protected produitsTarget: IProduit[] = [];
  protected statut = 'ENABLE';
  protected searchSource: string;
  protected searchTarget: string;
  protected tableau: ITableau;
  protected scrollHeight = 'calc(100vh - 350px)';
  private readonly produitService = inject(ProduitService);
  private readonly tableauProduitService = inject(TableauProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ tableau }) => {
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
        search: this.searchTarget || '',
        status: this.statut,
        tableauId: this.tableau.id,
      })
      .subscribe({ next: (res: HttpResponse<IProduit[]>) => (this.produitsTarget = res.body) });
  }

  protected previousState(): void {
    window.history.back();
  }

  protected fetchSource(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 300,
        search: this.searchSource || '',
        status: this.statut,
        tableauNot: this.tableau.id,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => (this.produitsSource = res.body));
  }

  protected trackId(index: number, item: IProduit): number {
    return item.id;
  }

  protected moveToTarget(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.associer(this.tableau.id, ids).subscribe(() => this.fetchTarget());
  }

  protected moveToSource(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.dissocier(ids).subscribe(() => this.fetchSource());
  }

  protected moveAllToTarget(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.associer(this.tableau.id, ids).subscribe(() => this.fetchTarget());
  }

  protected moveAllToSource(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.tableauProduitService.dissocier(ids).subscribe(() => this.fetchSource());
  }

  protected onSourceFilter(): void {
    this.fetchSource();
  }

  protected onTargetFilter(): void {
    this.fetchTarget();
  }
}
