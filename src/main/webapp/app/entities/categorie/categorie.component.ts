import { Component, OnInit, inject } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { ICategorie } from 'app/shared/model/categorie.model';

import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CategorieService } from './categorie.service';
import { CategorieDeleteDialogComponent } from './categorie-delete-dialog.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

import { PanelModule } from 'primeng/panel';

import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'jhi-categorie',
  templateUrl: './categorie.component.html',
  imports: [WarehouseCommonModule, PanelModule, ButtonModule, RouterModule],
})
export class CategorieComponent implements OnInit {
  protected categorieService = inject(CategorieService);
  protected modalService = inject(NgbModal);

  categories: ICategorie[];
  itemsPerPage: number;
  links: any;
  page: number;
  predicate: string;
  ascending: boolean;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    this.categories = [];
    this.itemsPerPage = ITEMS_PER_PAGE;
    this.page = 0;
    this.links = {
      last: 0,
    };
    this.predicate = 'id';
    this.ascending = true;
  }

  loadAll(): void {
    this.categorieService
      .query({
        sort: this.sort(),
      })
      .subscribe((res: HttpResponse<ICategorie[]>) => this.paginateCategories(res.body, res.headers));
  }

  reset(): void {
    this.page = 0;
    this.categories = [];
    this.loadAll();
  }

  loadPage(page: number): void {
    this.page = page;
    this.loadAll();
  }

  ngOnInit(): void {
    this.loadAll();
    this.registerChangeInCategories();
  }

  trackId(index: number, item: ICategorie): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  registerChangeInCategories(): void {
    this.reset();
  }

  delete(categorie: ICategorie): void {
    const modalRef = this.modalService.open(CategorieDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.categorie = categorie;
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  protected paginateCategories(data: ICategorie[] | null, headers: HttpHeaders): void {
    const headersLink = headers.get('link');
    // this.links = this.parseLinks.parse(headersLink ? headersLink : '');
    if (data) {
      for (let i = 0; i < data.length; i++) {
        this.categories.push(data[i]);
      }
    }
  }
}
