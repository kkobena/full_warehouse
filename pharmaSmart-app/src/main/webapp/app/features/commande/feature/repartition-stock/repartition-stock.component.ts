import { Component, inject, ViewChild, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { Select } from 'primeng/select';
import { NgbNavModule, NgbNav } from '@ng-bootstrap/ng-bootstrap';
import { APPEND_TO } from 'app/shared/constants/pagination.constants';
import { DATE_FORMAT_ISO_DATE } from 'app/shared/util/warehouse-util';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { handleBlobForTauri } from 'app/shared/util/tauri-util';
import { RepartitionStockService } from '../../../../entities/repartition-stock/repartition-stock.service';
import { UserService } from '../../../../entities/user/service/user.service';
import { StorageService } from '../../../../entities/storage/storage.service';
import { IUser } from '../../../../entities/user/user.model';
import { Storage } from '../../../../entities/storage/storage.model';
import { AppRepartitionListComponent } from './ui/repartition-list/repartition-list.component';
import { AppSuggestionReassortComponent } from './ui/suggestion-reassort/suggestion-reassort.component';
import { AppManualRepartitionComponent } from './ui/manual-repartition/manual-repartition.component';
import { Tooltip } from "primeng/tooltip";

@Component({
  selector: 'app-repartition-stock',
  templateUrl: './repartition-stock.component.html',
  styleUrls: ['./repartition-stock.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    FormsModule,
    ButtonModule,
    ToolbarModule,
    InputTextModule,
    IconField,
    InputIcon,
    DatePicker,
    FloatLabel,
    Select,
    TableModule,
    CardModule,
    DividerModule,
    RouterModule,
    NgbNavModule,
    AppRepartitionListComponent,
    AppSuggestionReassortComponent,
    AppManualRepartitionComponent,
    Tooltip
  ]
})
export class AppRepartitionStockComponent {
  @ViewChild('nav', { static: true }) nav!: NgbNav;

  protected search = '';
  protected dtStart: Date | null = new Date();
  protected dtEnd: Date | null = new Date();
  protected activeTab = 'historique';
  protected readonly appendTo = APPEND_TO;

  // Filtres avancés historique
  protected filterTypeRepartition: string = 'TOUT';
  protected filterUserId: number | null = null;
  protected filterStorageId: number | null = null;
  protected users: IUser[] = [];
  protected storages: Storage[] = [];

  protected typeRepartitionOptions = [
    { label: 'Tous les mouvements', value: 'TOUT' },
    { label: 'Automatique', value: 'AUTO' },
    { label: 'Manuel', value: 'MANUEL' },
  ];

  private readonly repartitionStockService = inject(RepartitionStockService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly userService = inject(UserService);
  private readonly storageService = inject(StorageService);
  private readonly repartitionList = viewChild(AppRepartitionListComponent);
  private readonly suggestionRayonComponent = viewChild<AppSuggestionReassortComponent>('suggestionRayon');
  private readonly suggestionReserveComponent = viewChild<AppSuggestionReassortComponent>('suggestionReserve');

  constructor() {
    this.loadUsers();
    this.loadStorages();
  }

  private loadUsers(): void {
    this.userService.query({ size: 200 }).subscribe({
      next: (res: HttpResponse<IUser[]>) => {
        this.users = res.body ?? [];
      },
    });
  }

  private loadStorages(): void {
    this.storageService.fetchUserStorages().subscribe({
      next: (res: HttpResponse<Storage[]>) => {
        this.storages = res.body ?? [];
      },
    });
  }

  onSearch(): void {
    if (this.activeTab === 'historique') {
      this.repartitionList()?.onSearch();
    }
  }

  onResetFilters(): void {
    this.search = '';
    this.dtStart = new Date();
    this.dtEnd = new Date();
    this.filterTypeRepartition = 'TOUT';
    this.filterUserId = null;
    this.filterStorageId = null;
    this.repartitionList()?.onSearch();
  }

  exportToPdf(): void {
    this.repartitionStockService
      .exportToPdf({
        dateDebut: DATE_FORMAT_ISO_DATE(this.dtStart),
        dateFin: DATE_FORMAT_ISO_DATE(this.dtEnd),
        searchTerm: this.search || null,
        typeRepartition: this.filterTypeRepartition !== 'TOUT' ? this.filterTypeRepartition : undefined,
        userId: this.filterUserId ?? undefined,
        storageId: this.filterStorageId ?? undefined,
      })
      .subscribe(blob => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'Repartition_Stock');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      });
  }

  protected onTabChange(_navChangeEvent: any): void {
    if (this.activeTab === 'rayon') {
      this.suggestionRayonComponent()?.reloadSuggestions();
    } else if (this.activeTab === 'reserve') {
      this.suggestionReserveComponent()?.reloadSuggestions();
    }
  }
}
