import { Component, DestroyRef, inject, OnInit, ChangeDetectionStrategy, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { HttpResponse } from "@angular/common/http";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbDateParserFormatter, NgbDatepickerModule, NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";
import { FrenchDateParserFormatter } from "../../../../config/french-date-parser-formatter";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { CATEGORIE_TIRERS_PAYANT, MODE_EDITIONS_FACTURE } from "../../../../shared/constants/data-constants";
import { CodeValue } from "../../../../shared/code-value";

import { IGroupeTiersPayant } from "../../../../shared/model/groupe-tierspayant.model";
import { ITiersPayant } from "../../../../shared/model";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { GroupeTiersPayantService } from "../../../../entities/groupe-tiers-payant/groupe-tierspayant.service";

import { FactureApiService } from "../../data-access/services/facture-api.service";
import {
  IDossierFacture,
  IEditionSearchParams,
  IFactureEditionResponse,
  ITiersPayantDossierFacture
} from "../../data-access/models";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import {
  ButtonComponent,
  DataTableComponent,
  FloatLabelComponent,
  HeaderCheckboxComponent,
  IconFieldComponent,
  MultiSelectComponent,
  RowCheckboxComponent,
  SelectComponent,
  SwitchComponent
} from "../../../../shared/ui";


@Component({
  selector: "app-facturation-edition",
  providers: [
    { provide: NgbDateParserFormatter, useClass: FrenchDateParserFormatter }
  ],
  imports: [
    CommonModule,
    FormsModule,
    NgbDatepickerModule,
    PharmaDatePickerComponent,
    PharmaDatePickerComponent,
    ButtonComponent,
    DataTableComponent,
    FloatLabelComponent,
    HeaderCheckboxComponent,
    IconFieldComponent,
    MultiSelectComponent,
    RowCheckboxComponent,
    SelectComponent,
    SwitchComponent
  ],
  templateUrl: "./facturation-edition.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./facturation-edition.component.scss"
})
export class FacturationEditionComponent implements OnInit {
  protected readonly modeEditions: CodeValue[] = MODE_EDITIONS_FACTURE;
  protected readonly typeTiersPayants: CodeValue[] = CATEGORIE_TIRERS_PAYANT;
  protected readonly minLength = 2;

  protected modeEdition: string | null = null;
  protected readonly typeTiersPayant = signal<string | null>(null);
  protected factureProvisoire = false;
  protected modelStartDate: NgbDateStruct = this.todayNgb();
  protected modelEndDate: NgbDateStruct = this.todayNgb();

  protected readonly groupeTiersPayants = signal<IGroupeTiersPayant[]>([]);
  protected readonly selectedGroupeTiersPayants = signal<IGroupeTiersPayant[]>([]);
  protected readonly tiersPayants = signal<ITiersPayant[]>([]);
  protected readonly selectedTiersPayants = signal<ITiersPayant[]>([]);

  protected readonly tiersPayantDossierFactures = signal<ITiersPayantDossierFacture[]>([]);
  protected readonly dossierFactures = signal<IDossierFacture[]>([]);
  protected readonly selectedDossiers = signal<IDossierFacture[]>([]);
  protected readonly selectedTiersPayantDossiers = signal<ITiersPayantDossierFacture[]>([]);

  protected readonly totalItems = signal(0);
  protected readonly totalItemsTp = signal(0);
  protected readonly itemsPerPage = 15;
  protected readonly page = signal(0);
  protected readonly pageTp = signal(0);

  protected readonly searching = signal(false);
  protected readonly editing = signal(false);
  protected readonly exporting = signal(false);

  private readonly factureApiService = inject(FactureApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  ngOnInit(): void {
    this.loadGroupTiersPayant();
    this.loadTiersPayants();
  }

  onModeEditionChange(): void {
    this.resetResults();
  }

  onSearch(): void {
    if (this.modeEdition === "SELECTION_BON") {
      this.loadBons();
    } else {
      this.loadData();
    }
  }

  onEdit(): void {
    this.confirmDialog.onConfirm(
      () => this.doEdit(),
      "Édition de factures",
      "Confirmer la génération des factures pour la sélection ?"
    );
  }

  searchTiersPayant(query: string): void {
    this.loadTiersPayants(query);
  }

  searchGroupTiersPayant(query: string): void {
    this.loadGroupTiersPayant(query);
  }

  lazyLoadingTp(event: any): void {
    if (event) {
      this.pageTp.set(Math.floor((event.first ?? 0) / (event.rows ?? this.itemsPerPage)));
      this.searching.set(true);
      this.factureApiService
        .queryEditionData({ page: this.pageTp(), size: event.rows, ...this.buildSearchParams() })
        .pipe(finalize(() => (this.searching.set(false))), takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => this.onDataSuccess(res.body, res.headers),
          error: err => this.onError(err)
        });
    }
  }

  lazyLoadingBons(event: any): void {
    if (event) {
      this.page.set(Math.floor((event.first ?? 0) / (event.rows ?? this.itemsPerPage)));
      this.searching.set(true);
      this.factureApiService
        .queryBons({ page: this.page(), size: event.rows, ...this.buildSearchParams() })
        .pipe(finalize(() => (this.searching.set(false))), takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => this.onBonsSuccess(res.body, res.headers),
          error: err => this.onError(err)
        });
    }
  }

  private loadTiersPayants(search = ""): void {
    this.tiersPayantService
      .query({ page: 0, search, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (res: HttpResponse<ITiersPayant[]>) => (this.tiersPayants.set(res.body ?? [])) });
  }

  private loadGroupTiersPayant(search = ""): void {
    this.groupeTiersPayantService
      .query({ page: 0, search, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (res: HttpResponse<IGroupeTiersPayant[]>) => (this.groupeTiersPayants.set(res.body ?? [])) });
  }

  private loadBons(): void {
    this.searching.set(true);
    this.page.set(0);
    this.factureApiService
      .queryBons({ page: 0, size: this.itemsPerPage, ...this.buildSearchParams() })
      .pipe(finalize(() => (this.searching.set(false))), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.onBonsSuccess(res.body, res.headers),
        error: err => this.onError(err)
      });
  }

  private loadData(): void {
    this.searching.set(true);
    this.pageTp.set(0);
    this.factureApiService
      .queryEditionData({ page: 0, size: this.itemsPerPage, ...this.buildSearchParams() })
      .pipe(finalize(() => (this.searching.set(false))), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.onDataSuccess(res.body, res.headers),
        error: err => this.onError(err)
      });
  }

  private doEdit(): void {
    this.editing.set(true);
    this.factureApiService
      .editInvoices(this.buildEditionParams())
      .pipe(finalize(() => (this.editing.set(false))), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res: HttpResponse<IFactureEditionResponse>) => {
          if (res.body) {
            this.onEditionSuccess(res.body);
          }
        },
        error: err => this.onError(err)
      });
  }

  private onEditionSuccess(response: IFactureEditionResponse): void {
    this.notificationService.success("Factures générées avec succès", "Édition réussie");
    this.confirmDialog.onConfirm(
      () => this.downloadPdf(response),
      "Impression des factures",
      "Voulez-vous télécharger le PDF des factures générées ?"
    );
    this.resetResults();
  }

  private downloadPdf(response: IFactureEditionResponse): void {
    this.exporting.set(true);
    this.factureApiService
      .exportAllInvoices(response)
      .pipe(finalize(() => (this.exporting.set(false))), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (blob) => this.downloadDocumentService.downloadPdf(blob, "factures_edition"),
        error: err => this.onError(err)
      });
  }

  private onBonsSuccess(data: IDossierFacture[] | null, headers: any): void {
    this.dossierFactures.set(data ?? []);
    this.totalItems.set(Number(headers.get("X-Total-Count")));
  }

  private onDataSuccess(data: ITiersPayantDossierFacture[] | null, headers: any): void {
    this.tiersPayantDossierFactures.set(data ?? []);
    this.totalItemsTp.set(Number(headers.get("X-Total-Count")));
  }

  private onError(err: any): void {
    this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur");
  }

  private resetResults(): void {
    this.tiersPayantDossierFactures.set([]);
    this.dossierFactures.set([]);
    this.selectedDossiers.set([]);
    this.selectedTiersPayantDossiers.set([]);
    this.typeTiersPayant.set(null);
    this.selectedGroupeTiersPayants.set([]);
    this.selectedTiersPayants.set([]);
  }

  private buildSearchParams(): IEditionSearchParams {
    return {
      startDate: this.ngbDateToIso(this.modelStartDate),
      endDate: this.ngbDateToIso(this.modelEndDate),
      modeEdition: this.modeEdition ?? "ALL",
      factureProvisoire: this.factureProvisoire,
      groupIds: this.selectedGroupeTiersPayants().map(g => g.id),
      tiersPayantIds: this.selectedTiersPayants().map(tp => tp.id),
      categorieTiersPayants: this.typeTiersPayant() ? [this.typeTiersPayant()] : [],
      all: !this.modeEdition
    };
  }

  private todayNgb(): NgbDateStruct {
    const d = new Date();
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }

  private ngbDateToIso(date: NgbDateStruct | null): string | null {
    if (!date) return null;
    return `${date.year}-${String(date.month).padStart(2, "0")}-${String(date.day).padStart(2, "0")}`;
  }

  private buildEditionParams(): IEditionSearchParams {
    const base = this.buildSearchParams();
    if (this.modeEdition === "SELECTION_BON") {
      return { ...base, ids: this.selectedDossiers().map(d => d.id!) };
    }
    if (this.modeEdition === "SELECTED" || this.modeEdition === "GROUP") {
      return { ...base, ids: this.selectedTiersPayantDossiers().map(d => d.id) };
    }
    return base;
  }
}
