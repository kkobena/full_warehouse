import { Component, Input, OnInit } from '@angular/core';
import { AjustementStatut } from '../../../shared/model/enumerations/ajustement-statut.model';
import { IUser } from '../../../core/user/user.model';
import { IAjust } from '../../../shared/model/ajust.model';
import { IAjustement } from '../../../shared/model/ajustement.model';
import { ITEMS_PER_PAGE } from '../../../config/pagination.constants';
import { UserService } from '../../../core/user/user.service';
import { TranslateService } from '@ngx-translate/core';
import { AjustementService } from '../ajustement.service';
import { Router } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import moment from 'moment';
import { IDelivery } from '../../../shared/model/delevery.model';
import { saveAs } from 'file-saver';
import { NgxSpinnerService } from 'ngx-spinner';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

@Component({
  standalone: true,
  selector: 'jhi-list-ajustement',
  templateUrl: './list-ajustement.component.html',
  imports: [WarehouseCommonModule, ButtonModule, TableModule],
})
export class ListAjustementComponent implements OnInit {
  @Input() search: string;
  @Input() fromDate: Date;
  @Input() toDate: Date;
  @Input() user?: IUser | null;
  protected ajustementStatut: AjustementStatut = AjustementStatut.CLOSED;
  protected rowData: IAjust[] = [];
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected ajustements?: IAjustement[];

  constructor(
    protected userService: UserService,
    public translate: TranslateService,
    protected ajustementService: AjustementService,
    protected router: Router,
    private spinner: NgxSpinnerService,
  ) {}

  ngOnInit(): void {
    this.onSearch();
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'dateMtv') {
      result.push('dateMtv');
    }
    return result;
  }

  onSearch(): void {
    this.loadPage();
  }

  exportPdf(delivery: IDelivery): void {
    this.spinner.show();
    this.ajustementService.exportToPdf(delivery.id).subscribe({
      next: blod => {
        this.spinner.hide();
        saveAs(blod);
      },
      error: () => this.spinner.hide(),
    });
  }

  protected onSuccess(data: IAjust[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/ajustement'], {
        queryParams: this.buildQuery(page),
      });
    }
    this.rowData = data || [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  private buildQuery(page?: number): any {
    const pageToLoad: number = page || this.page || 1;
    return {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      fromDate: this.fromDate ? moment(this.fromDate).format('yyyy-MM-DD') : null,
      toDate: this.toDate ? moment(this.toDate).format('yyyy-MM-DD') : null,
      userId: this.user.id,
      statut: this.ajustementStatut,
    };
  }

  private loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;

    this.ajustementService.queryAjustement(this.buildQuery(page)).subscribe({
      next: (res: HttpResponse<IAjust[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
      error: () => this.onError(),
    });
  }
}
