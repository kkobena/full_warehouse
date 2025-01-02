import { AfterViewInit, Component, inject } from '@angular/core';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { InputSwitchModule } from 'primeng/inputswitch';
import {
  NgbCalendar,
  NgbDateAdapter,
  NgbDateParserFormatter,
  NgbDatepickerI18n,
  NgbDatepickerModule,
  NgbDateStruct,
} from '@ng-bootstrap/ng-bootstrap';
import { ToolbarModule } from 'primeng/toolbar';
import { TableModule } from 'primeng/table';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { CustomAdapter, CustomDateParserFormatter, CustomDatepickerI18n, I18n } from '../../../shared/util/datepicker-adapter';
import { IGroupeTiersPayant } from '../../../shared/model/groupe-tierspayant.model';
import { ITiersPayant } from '../../../shared/model/tierspayant.model';
import { HttpResponse } from '@angular/common/http';
import { TiersPayantService } from '../../tiers-payant/tierspayant.service';
import { GroupeTiersPayantService } from '../../groupe-tiers-payant/groupe-tierspayant.service';
import { InputTextModule } from 'primeng/inputtext';
import { InvoicePaymentParam, Reglement } from '../model/reglement.model';
import { dateIsoFormatFom, getNgbDateStruct } from '../../../shared/util/warehouse-util';
import { RegelementStateService } from '../regelement-state.service';
import { DividerModule } from 'primeng/divider';
import { RippleModule } from 'primeng/ripple';
import { ReglementService } from '../reglement.service';

@Component({
  selector: 'jhi-factures-reglees',
  standalone: true,
  imports: [
    ButtonModule,
    TableModule,
    WarehouseCommonModule,
    ToolbarModule,
    InputSwitchModule,
    FormsModule,
    FloatLabelModule,
    AutoCompleteModule,
    NgbDatepickerModule,
    InputTextModule,
    DividerModule,
    RippleModule,
  ],
  providers: [
    I18n,
    { provide: NgbDatepickerI18n, useClass: CustomDatepickerI18n },
    { provide: NgbDateAdapter, useClass: CustomAdapter },
    { provide: NgbDateParserFormatter, useClass: CustomDateParserFormatter },
  ],
  templateUrl: './factures-reglees.component.html',
  styleUrl: './factures-reglees.component.scss',
})
export class FacturesRegleesComponent implements AfterViewInit {
  tiersPayantService = inject(TiersPayantService);
  groupeTiersPayantService = inject(GroupeTiersPayantService);
  regelementStateService = inject(RegelementStateService);
  reglementService = inject(ReglementService);
  expandedRows = {};
  protected calendar = inject(NgbCalendar);
  protected today = this.calendar.getToday();
  protected modelStartDate: NgbDateStruct;
  protected modelEndDate: NgbDateStruct;
  protected search: string | null = null;
  protected factureGroup = false;
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayant: IGroupeTiersPayant | undefined;
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayant: ITiersPayant | undefined;
  protected minLength = 2;
  protected loadingBtn = false;
  protected removeAll = false;
  protected datas: Reglement[] = [];
  protected selectedDatas: Reglement[] = [];
  protected scrollHeight = 'calc(100vh - 300px)';

  onSearch(): void {
    this.fetchData();
  }

  onRemoveAll(): void {
    console.log('onRemoveAll: ', this.selectedDatas);
  }

  onView(item: Reglement): void {
    console.log('onView: ', item);
  }

  onPrint(item: Reglement): void {
    console.log('onPrint: ', item);
    this.reglementService.printReceipt(item.id).subscribe();
  }

  onDelete(item: Reglement): void {
    console.log('onDelete: ', item);
  }

  searchTiersPayant(event: any): void {
    this.loadTiersPayants(event.query);
  }

  searchGroupTiersPayant(event: any): void {
    this.loadGroupTiersPayant(event.query);
  }

  loadGroupTiersPayant(search?: string): void {
    const query: string = search || '';
    this.groupeTiersPayantService
      .query({
        page: 0,
        search: query,
        size: 10,
      })
      .subscribe((res: HttpResponse<IGroupeTiersPayant[]>) => {
        this.groupeTiersPayants = res.body || [];
      });
  }

  loadTiersPayants(search?: string): void {
    const query: string = search || '';
    this.tiersPayantService
      .query({
        page: 0,
        search: query,
        size: 10,
      })
      .subscribe({
        next: (res: HttpResponse<ITiersPayant[]>) => {
          this.tiersPayants = res.body || [];
        },
      });
  }

  expandAll() {
    // @ts-ignore
    this.expandedRows = this.datas.reduce((acc, p) => (acc[p.organismeId] = true) && acc, {});
  }

  ngAfterViewInit(): void {
    const previousSearch = this.regelementStateService.invoicePaymentParam();
    if (previousSearch) {
      this.factureGroup = previousSearch.grouped;
      this.search = previousSearch.search;
      if (previousSearch.grouped) {
        this.selectedGroupeTiersPayant = this.groupeTiersPayants.find(t => t.id === previousSearch.organismeId);
      } else {
        this.selectedTiersPayant = this.tiersPayants.find(t => t.id === previousSearch.organismeId);
      }
      this.modelStartDate = getNgbDateStruct(previousSearch.fromDate);
      this.modelEndDate = getNgbDateStruct(previousSearch.toDate);
    } else {
      this.modelStartDate = {
        year: this.today.year,
        month: this.today.month,
        day: this.today.day,
      };
      this.modelEndDate = {
        year: this.today.year,
        month: this.today.month,
        day: this.today.day,
      };
    }
    this.fetchData();
  }

  protected getTotalAmountByGroup(groupeId: number): number {
    return this.datas.filter(d => d.organismeId === groupeId).reduce((acc, cur) => acc + cur.totalAmount, 0);
  }

  private buildSearchParams(): InvoicePaymentParam {
    const params = {
      search: this.search,
      organismeId: this.factureGroup ? this.selectedGroupeTiersPayant?.id : this.selectedTiersPayant?.id,
      fromDate: dateIsoFormatFom(this.modelStartDate),
      toDate: dateIsoFormatFom(this.modelEndDate),
      grouped: this.factureGroup,
    };

    this.regelementStateService.setInvoicePaymentParam(params);
    return params;
  }

  private fetchData(): void {
    this.reglementService.query(this.buildSearchParams()).subscribe({
      next: (res: HttpResponse<Reglement[]>) => {
        this.datas = res.body;
        this.expandAll();
      },
      error: () => {
        this.datas = [];
      },
    });
  }
}
