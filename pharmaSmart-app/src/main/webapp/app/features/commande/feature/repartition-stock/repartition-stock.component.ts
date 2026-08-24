import { Component, inject, ViewChild, viewChild, ChangeDetectionStrategy, input, signal } from '@angular/core';
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
  changeDetection: ChangeDetectionStrategy.OnPush,
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
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  @ViewChild('nav', { static: true }) nav!: NgbNav;

  protected readonly search = signal('');
  protected readonly dtStart = signal<NgbDateStruct | null>(TODAY_NGB_DATE());
  protected readonly dtEnd = signal<NgbDateStruct | null>(TODAY_NGB_DATE());
  protected activeTab = 'historique';
  protected readonly appendTo = APPEND_TO;

  // Filtres avancés historique
  protected readonly filterTypeRepartition = signal<string>('TOUT');
  protected readonly filterUserId = signal<number | null>(null);
  protected readonly filterStorageId = signal<number | null>(null);
  protected readonly users = signal<IUser[]>([]);
  protected readonly storages = signal<Storage[]>([]);

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
        this.users.set(res.body ?? []);
      },
    });
  }

  private loadStorages(): void {
    this.storageService.fetchUserStorages().subscribe({
      next: (res: HttpResponse<Storage[]>) => {
        this.storages.set(res.body ?? []);
      },
    });
  }

  onSearch(): void {
    if (this.activeTab === 'historique') {
      this.repartitionList()?.onSearch();
    }
  }

  onResetFilters(): void {
    this.search.set('');
    this.dtStart.set(TODAY_NGB_DATE());
    this.dtEnd.set(TODAY_NGB_DATE());
    this.filterTypeRepartition.set('TOUT');
    this.filterUserId.set(null);
    this.filterStorageId.set(null);
    this.repartitionList()?.onSearch();
  }

  exportToPdf(): void {
    this.repartitionStockService
      .exportToPdf({
        dateDebut: NGB_DATE_TO_ISO(this.dtStart()),
        dateFin: NGB_DATE_TO_ISO(this.dtEnd()),
        searchTerm: this.search() || null,
        typeRepartition: this.filterTypeRepartition() !== 'TOUT' ? this.filterTypeRepartition() : undefined,
        userId: this.filterUserId() ?? undefined,
        storageId: this.filterStorageId() ?? undefined,
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
