import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal, input} from "@angular/core";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {finalize} from "rxjs/operators";
import {FormsModule} from "@angular/forms";
import {NgbDateStruct} from "@ng-bootstrap/ng-bootstrap";
import {ButtonComponent, SelectComponent, ToolbarComponent} from "../../../../shared/ui";
import {
  PharmaDatePickerComponent
} from "../../../../shared/date-picker/pharma-date-picker.component";

import {NGB_DATE_TO_ISO, TODAY_NGB_DATE} from "../../../../shared/util/warehouse-util";
import {NotificationService} from "../../../../shared/services/notification.service";
import {ErrorService} from "../../../../shared/error.service";

import {AbilityService} from "app/core/auth/ability.service";
import {DiffereStore} from "../../data-access/store/differe.store";
import {DiffereApiService} from "../../data-access/services/differe-api.service";
import {IDiffereSearchParams} from "../../data-access/models";
import {DiffereKpiBannerComponent} from "../../ui/differe-kpi-banner/differe-kpi-banner.component";
import {DiffereListComponent} from "../../ui/differe-list/differe-list.component";
import {
  DiffereDetailPanelComponent
} from "../../ui/differe-detail-panel/differe-detail-panel.component";
import {BlobDownloadService} from "../../../../shared/services/blob-download.service";
import { HintComponent } from 'app/shared/ui/hint/hint.component';

type StatutDiffere = "PAYE" | "IMPAYE";

@Component({
  selector: "app-differes-home",
  imports: [
    HintComponent,
    FormsModule,
    ButtonComponent,
    PharmaDatePickerComponent,
    SelectComponent,
    ToolbarComponent,
    DiffereKpiBannerComponent,
    DiffereListComponent,
    DiffereDetailPanelComponent
  ],
  templateUrl: "./differes-home.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./differes-home.component.scss"
})
export class DifferesHomeComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu — ou son `titre_long`
   * quand la barre nomme plus longuement. Un écran atteint depuis deux menus affiche donc le nom
   * de celui par lequel on est entré.
   */
  readonly navCode = input<string>('');

  protected readonly store = inject(DiffereStore);
  protected readonly panelOpen = computed(() => this.store.panelOpen());
  // Toolbar state
  protected readonly modelStartDate = signal<NgbDateStruct | undefined>(undefined);
  protected modelEndDate: NgbDateStruct = TODAY_NGB_DATE();
  protected customerId: number | null = null;
  protected statut: StatutDiffere = "IMPAYE";
  protected readonly typesDifferes: { id: StatutDiffere; label: string }[] = [
    {id: "IMPAYE", label: "En cours"},
    {id: "PAYE", label: "Soldé"}
  ];
  protected readonly loadingPdf = signal(false);
  // Signal transmis à la liste
  protected readonly currentSearchParams = signal<IDiffereSearchParams | null>(null);
  // Hint premier usage
  private readonly ability = inject(AbilityService);
  protected readonly canExecute = this.ability.canSignal("execute", "differes");
  protected readonly canExport = this.ability.canSignal("export", "differes");
  private readonly differeApiService = inject(DiffereApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly blobDownload = inject(BlobDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate.set({year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate()});
  }

  ngOnInit(): void {
    if (!this.store.clientsLoaded()) {
      this.loadClients();
    }
    this.onSearch();
  }

  onSearch(): void {
    this.currentSearchParams.set(this.buildParams());
  }

  exportPdf(): void {
    this.loadingPdf.set(true);
    this.differeApiService
      .exportListToPdf(this.buildParams())
      .pipe(
        finalize(() => (this.loadingPdf.set(false))),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => {
          this.blobDownload.downloadPdf(blob, "differes");
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export PDF")
      });
  }


  private loadClients(): void {
    this.differeApiService
      .findClients()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.store.setClients(res.body ?? []),
        error: () => this.store.setClients([])
      });
  }

  private buildParams(): IDiffereSearchParams {
    const params: IDiffereSearchParams = {
      paymentStatuses: [this.statut],
      fromDate: NGB_DATE_TO_ISO(this.modelStartDate()),
      toDate: NGB_DATE_TO_ISO(this.modelEndDate)
    };
    if (this.customerId) {
      params.customerId = this.customerId;
    }
    this.store.setSearchParams(params);
    return params;
  }
}
