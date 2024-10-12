import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { CashRegisterService } from '../../cash-register/cash-register.service';
import { Button } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { FormsModule } from '@angular/forms';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { IUser } from '../../../core/user/user.model';
import { CashRegister, CashRegisterStatut, MvtCaisse } from '../../cash-register/model/cash-register.model';
import { ConfirmationService, MessageService, PrimeNGConfig } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { MvtParamServiceService } from '../mvt-param-service.service';
import { MvtCaisseParams } from '../mvt-caisse-util';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { UserService } from '../../../core/user/user.service';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ToastModule } from 'primeng/toast';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'jhi-gestion-caisse',
  standalone: true,
  providers: [MessageService, ConfirmationService],
  imports: [
    WarehouseCommonModule,
    Button,
    CalendarModule,
    DropdownModule,
    InputTextModule,
    MultiSelectModule,
    ToolbarModule,
    TooltipModule,
    FormsModule,
    ToastModule,
    CardModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './gestion-caisse.component.html',
  styles: ``,
})
export class GestionCaisseComponent implements OnInit, AfterViewInit {
  entityService = inject(CashRegisterService);
  protected totalItems = 0;
  protected loading!: boolean;
  protected btnLoading = false;
  protected page = 0;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
  protected selectedUser: IUser | null = null;
  protected users: IUser[];
  protected datas: CashRegister[];
  protected readonly OPEN = CashRegisterStatut.OPEN;
  protected readonly VALIDATED = CashRegisterStatut.VALIDATED;
  protected readonly CLOSED = CashRegisterStatut.CLOSED;
  private primeNGConfig = inject(PrimeNGConfig);
  private translate = inject(TranslateService);
  private userService = inject(UserService);
  private mvtParamServiceService = inject(MvtParamServiceService);

  ngAfterViewInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  onSearch(): void {
    this.btnLoading = true;
    this.loadPage();
    this.updateParam();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.entityService
      .query({
        statuts: [CashRegisterStatut.OPEN, CashRegisterStatut.VALIDATED, CashRegisterStatut.CLOSED, CashRegisterStatut.PENDING],
        page: pageToLoad,
        ...this.buildParams(),
      })
      .subscribe({
        next: (res: HttpResponse<MvtCaisse[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
        complete: () => {
          this.btnLoading = false;
        },
      });
  }

  ngOnInit(): void {
    if (this.mvtParamServiceService.mvtCaisseParam()) {
      this.fromDate = this.mvtParamServiceService.mvtCaisseParam().fromDate;
      this.toDate = this.mvtParamServiceService.mvtCaisseParam().toDate;
      this.selectedUser = this.mvtParamServiceService.mvtCaisseParam().selectedUser;
    }
    this.loadUsers();
    this.onSearch();
  }

  protected onPrint(): void {
    this.btnLoading = true;
    this.updateParam();
    this.entityService.exportToPdf(this.buildParams()).subscribe({
      next: blod => {
        this.btnLoading = false;
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
      error: () => {
        this.btnLoading = false;
        /*  this.messageService.add({
           severity: 'error',
           summary: 'Error',
           detail: 'Une erreur est survenue',
         }); */
      },
      complete: () => {
        this.btnLoading = false;
      },
    });
  }

  protected onSuccess(data: CashRegister[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;

    this.datas = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.btnLoading = false;
  }

  private updateParam(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      params.fromDate = this.fromDate;
      params.toDate = this.toDate;
      params.selectedUser = this.selectedUser;
      this.mvtParamServiceService.setMvtCaisseParam(params);
    } else {
      this.setParam();
    }
  }

  private loadUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<IUser[]>) => {
      this.users = res.body || [];
    });
  }

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate,
      toDate: this.toDate,
      selectedUser: this.selectedUser,
    };
    this.mvtParamServiceService.setMvtCaisseParam(param);
  }

  private buildParams(): any {
    return {
      size: this.itemsPerPage,
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      userId: this.selectedUser?.id,
    };
  }
}
