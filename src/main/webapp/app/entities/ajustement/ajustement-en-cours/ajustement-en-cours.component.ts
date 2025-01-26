import { Component, OnInit } from '@angular/core';
import { AjustementStatut } from '../../../shared/model/enumerations/ajustement-statut.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { IAjust } from '../../../shared/model/ajust.model';
import { IAjustement } from '../../../shared/model/ajustement.model';
import { UserService } from '../../../core/user/user.service';
import { TranslateService } from '@ngx-translate/core';
import { AjustementService } from '../ajustement.service';
import { Router, RouterModule } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import moment from 'moment/moment';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Ripple } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';

@Component({
  selector: 'jhi-ajustement-en-cours',
  templateUrl: './ajustement-en-cours.component.html',
  imports: [WarehouseCommonModule, RouterModule, ConfirmDialogModule, ButtonModule, TableModule, Ripple, TooltipModule],
  providers: [ConfirmationService, DialogService, MessageService],
})
export class AjustementEnCoursComponent implements OnInit {
  protected ajustementStatut: AjustementStatut = AjustementStatut.PENDING;
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
    private confirmationService: ConfirmationService,
    protected router: Router,
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

  confirmDelete(ajust: IAjust): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette ligne  ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps,
      acceptButtonProps: acceptButtonProps,
      accept: () => this.delete(ajust.id),
      key: 'delete',
    });
  }

  delete(id: number): void {
    this.ajustementService.delete(id).subscribe({
      next: () => {
        this.loadPage();
      },
      error: () => {
        this.onError();
      },
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
    const params = this.ajustementService.toolbarParam();
    const pageToLoad: number = page || this.page || 1;
    return {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      search: params.search,
      fromDate: params.fromDate ? moment(params.fromDate).format('yyyy-MM-DD') : null,
      toDate: params.toDate ? moment(params.toDate).format('yyyy-MM-DD') : null,
      userId: params.userId,
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
