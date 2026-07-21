import { Component, inject, ViewChild, viewChild, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { NgbDateStruct, NgbNavModule, NgbNav, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, IconFieldComponent, SelectComponent, ToolbarComponent } from 'app/shared/ui';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { APPEND_TO } from 'app/shared/constants/pagination.constants';
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from 'app/shared/util/warehouse-util';
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

@Component({
  selector: 'app-repartition-stock',
  templateUrl: './repartition-stock.component.html',
  styleUrls: ['./repartition-stock.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    IconFieldComponent,
    PharmaDatePickerComponent,
    SelectComponent,
    RouterModule,
    NgbNavModule,
    NgbTooltip,
    AppRepartitionListComponent,
    AppSuggestionReassortComponent,
    AppManualRepartitionComponent,
  ]
})
export class AppRepartitionStockComponent {
  @ViewChild('nav', { static: true }) nav!: NgbNav;

  protected search = '';
  protected dtStart: NgbDateStruct | null = TODAY_NGB_DATE();
  protected dtEnd: NgbDateStruct | null = TODAY_NGB_DATE();
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
    this.dtStart = TODAY_NGB_DATE();
    this.dtEnd = TODAY_NGB_DATE();
    this.filterTypeRepartition = 'TOUT';
    this.filterUserId = null;
    this.filterStorageId = null;
    this.repartitionList()?.onSearch();
  }

  exportToPdf(): void {
    this.repartitionStockService
      .exportToPdf({
        dateDebut: NGB_DATE_TO_ISO(this.dtStart),
        dateFin: NGB_DATE_TO_ISO(this.dtEnd),
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
