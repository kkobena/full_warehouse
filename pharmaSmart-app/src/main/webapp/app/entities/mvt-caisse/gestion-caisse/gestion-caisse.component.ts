import { Component, inject, OnInit, ChangeDetectionStrategy, input, signal } from "@angular/core";
import { CashRegisterService } from "../../cash-register/cash-register.service";
import { FormsModule } from "@angular/forms";
import { ITEMS_PER_PAGE } from "../../../shared/constants/pagination.constants";
import { IUser } from "../../../core/user/user.model";
import { CashRegister, CashRegisterStatut, MvtCaisse } from "../../cash-register/model/cash-register.model";
import { MvtParamServiceService } from "../mvt-param-service.service";
import { MvtCaisseParams } from "../mvt-caisse-util";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { UserService } from "../../../core/user/user.service";
import { NGB_DATE_TO_ISO } from "../../../shared/util/warehouse-util";
import { CommonModule } from "@angular/common";
import { NotificationService } from "../../../shared/services/notification.service";
import { NgbDateStruct, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { BadgeComponent, ButtonComponent, DataTableComponent, SelectComponent, ToolbarComponent } from "../../../shared/ui";
import { PharmaDatePickerComponent } from "../../../shared/date-picker/pharma-date-picker.component";

@Component({
  selector: "app-gestion-caisse",
  styleUrls: ["./gestion-caisse.component.scss"],
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    DataTableComponent,
    SelectComponent,
    PharmaDatePickerComponent,
    BadgeComponent,
    NgbTooltip
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: "./gestion-caisse.component.html"
})
export class GestionCaisseComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  protected readonly totalItems = signal(0);
  protected readonly loading = signal<boolean | undefined>(undefined);
  protected readonly btnLoading = signal(false);
  protected readonly page = signal(0);
  protected readonly ngbPaginationPage = signal(1);
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected readonly fromDate = signal<NgbDateStruct | null>(null);
  protected readonly toDate = signal<NgbDateStruct | null>(null);
  protected readonly selectedUser = signal<IUser | null>(null);
  protected readonly users = signal<IUser[] | undefined>(undefined);
  protected readonly datas = signal<CashRegister[] | undefined>(undefined);
  protected readonly OPEN = CashRegisterStatut.OPEN;
  protected readonly VALIDATED = CashRegisterStatut.VALIDATED;
  protected readonly CLOSED = CashRegisterStatut.CLOSED;
  private readonly userService = inject(UserService);
  private readonly mvtParamServiceService = inject(MvtParamServiceService);
  private readonly entityService = inject(CashRegisterService);
  private readonly notificationService = inject(NotificationService);

  onSearch(): void {
    this.btnLoading.set(true);
    this.loadPage();
    this.updateParam();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page();
    this.loading.set(true);
    this.entityService
      .query({
        page: pageToLoad,
        ...this.buildParams()
      })
      .subscribe({
        next: (res: HttpResponse<MvtCaisse[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
        complete: () => {
          this.btnLoading.set(false);
        }
      });
  }

  ngOnInit(): void {
    if (this.mvtParamServiceService.mvtCaisseParam()) {
      this.fromDate.set(this.mvtParamServiceService.mvtCaisseParam().fromDate);
      this.toDate.set(this.mvtParamServiceService.mvtCaisseParam().toDate);
      this.selectedUser.set(this.mvtParamServiceService.mvtCaisseParam().selectedUser);
    }
    this.loadUsers();
    this.onSearch();
  }

  protected onPrint(): void {
    this.notificationService.warning("Fonctionnalité non implémentée");
  }

  protected onSuccess(data: CashRegister[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.page.set(page);

    this.datas.set(data || []);
    this.loading.set(false);
  }

  protected onError(): void {
    this.ngbPaginationPage.set(this.page() ?? 1);
    this.btnLoading.set(false);
  }

  private updateParam(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      params.fromDate = this.fromDate();
      params.toDate = this.toDate();
      params.selectedUser = this.selectedUser();
      this.mvtParamServiceService.setMvtCaisseParam(params);
    } else {
      this.setParam();
    }
  }

  private loadUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<IUser[]>) => {
      this.users.set(res.body || []);
    });
  }

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate(),
      toDate: this.toDate(),
      selectedUser: this.selectedUser()
    };
    this.mvtParamServiceService.setMvtCaisseParam(param);
  }

  private buildParams(): any {
    return {
      statuts: [CashRegisterStatut.OPEN, CashRegisterStatut.VALIDATED, CashRegisterStatut.CLOSED, CashRegisterStatut.PENDING],
      size: this.itemsPerPage,
      fromDate: this.fromDate() ? NGB_DATE_TO_ISO(this.fromDate()) : null,
      toDate: this.toDate() ? NGB_DATE_TO_ISO(this.toDate()) : null,
      userId: this.selectedUser()?.id
    };
  }
}
