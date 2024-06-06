import { Component } from '@angular/core';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { PickListModule } from 'primeng/picklist';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { IProduit } from '../../../shared/model/produit.model';
import { ProduitService } from '../../produit/produit.service';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { IRemise } from '../../../shared/model/remise.model';
import { RemiseService } from '../remise.service';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { KeyFilterModule } from 'primeng/keyfilter';

@Component({
  selector: 'jhi-produit-remise',
  standalone: true,
  imports: [
    WarehouseCommonModule,
    FormsModule,
    PickListModule,
    ToolbarModule,
    ButtonModule,
    InputTextModule,
    RippleModule,
    ConfirmDialogModule,
    DialogModule,
    KeyFilterModule,
  ],
  templateUrl: './produit-remise.component.html',
  styleUrl: './produit-remise.component.scss',
})
export class ProduitRemiseComponent {
  produitsSource: IProduit[] = [];
  produitsTarget: IProduit[] = [];
  statut: string = 'ENABLE';
  searchSource: string;
  searchTarget: string;
  remise: IRemise;

  constructor(
    protected produitService: ProduitService,
    private remiseService: RemiseService,
    protected activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ remise }) => {
      this.remise = remise;
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
        remiseId: this.remise.id,
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
        remiseNot: this.remise.id,
      })
      .subscribe((res: HttpResponse<IProduit[]>) => (this.produitsSource = res.body));
  }

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id;
  }

  moveToTarget(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.remiseService.associer(this.remise.id, ids).subscribe(() => this.fetchTarget());
  }

  moveToSource(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.remiseService.dissocier(ids).subscribe(() => this.fetchSource());
  }

  moveAllToTarget(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.remiseService.associer(this.remise.id, ids).subscribe(() => this.fetchTarget());
  }

  moveAllToSource(event: any): void {
    const ids = event.items.map((el: any) => el.id);
    this.remiseService.dissocier(ids).subscribe(() => this.fetchSource());
  }

  sourceFilter(): void {
    this.fetchSource();
  }

  targeFilter(): void {
    this.fetchTarget();
  }
}
