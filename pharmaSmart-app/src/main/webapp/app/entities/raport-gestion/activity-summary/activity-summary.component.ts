import { Component, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ActivitySummaryService } from './activity-summary.service';
import { ChiffreAffaire } from './model/chiffre-affaire.model';
import { GroupeFournisseurAchat } from './model/groupe-fournisseur-achat.model';
import { ReglementTiersPayant } from './model/reglement-tiers-payant.model';
import { AchatTiersPayant } from './model/achat-tiers-payant.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from '../../../shared/util/warehouse-util';
import { BlobDownloadService } from '../../../shared/services/blob-download.service';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../../shared/services/notification.service';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import {
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  ToolbarComponent
} from '../../../shared/ui';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';

@Component({
  selector: 'jhi-activity-summary',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ButtonComponent,
    DataTableComponent,
    IconFieldComponent,
    ToolbarComponent,
    PharmaDatePickerComponent
  ],
  templateUrl: './activity-summary.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './activity-summary.component.scss',
})
export class ActivitySummaryComponent {
  protected loadingPdf = false;
  protected chiffreAffaire: ChiffreAffaire | null = null;
  protected groupeFournisseurAchats: GroupeFournisseurAchat[] | null = [];
  protected reglementTiersPayants: ReglementTiersPayant[] | null = [];
  protected achatTiersPayant: AchatTiersPayant[] | null = [];
  protected fromDate: NgbDateStruct | null = TODAY_NGB_DATE();
  protected toDate: NgbDateStruct | null = TODAY_NGB_DATE();
  protected searchAchat: string | null = null;
  protected searchReglement: string | null = null;
  // protected scrollHeight = 'calc(100vh - 350px)';
  protected loadingCa = signal(false);
  protected loadingAchat = signal(false);
  protected loadingReglement = signal(false);
  protected loadingAchatTp = signal(false);
  protected loadingBtn = computed(() => this.loadingCa() || this.loadingAchat() || this.loadingReglement() || this.loadingAchatTp());
  private readonly activitySummaryService = inject(ActivitySummaryService);
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly notificationService = inject(NotificationService);
  constructor() {
    this.loadAll();
  }

  protected loadAll(): void {
    const query = this.buildRequest();
    this.queryCa(query);
    this.getGroupeFournisseurAchat(query);
    this.getReglementTiersPayants(query);
    this.getAchatTiersPayant(query);
  }

  protected printAll(): void {
    this.loadingPdf = true;
    this.activitySummaryService
      .onPrintPdf(this.buildRequest())
      .pipe(finalize(() => (this.loadingPdf = false)))
      .subscribe({
        next: blob => this.blobDownloadService.downloadPdf(blob, 'rapport-activite'),
        error: () => this.notificationService.error("Une erreur est survenue lors de l'export PDF"),
      });
  }

  protected getRecetteTotal(): number {
    return this.chiffreAffaire?.recettes?.reduce((acc, val) => acc + val.montantReel, 0) || 0;
  }

  protected getTotalMouvementCaisse(): number {
    return this.chiffreAffaire?.mouvementCaisses?.reduce((acc, val) => acc + val.montant, 0) || 0;
  }

  private queryCa(query: any): void {
    this.loadingCa.set(true);
    this.activitySummaryService.queryCa(query).subscribe({
      next: res => {
        this.chiffreAffaire = res.body;
        this.loadingCa.set(false);
      },
      error: err => this.loadingCa.set(false),
    });
  }

  private getGroupeFournisseurAchat(query: any): void {
    this.loadingAchat.set(true);
    this.activitySummaryService.getGroupeFournisseurAchat(query).subscribe({
      next: res => {
        this.groupeFournisseurAchats = res.body;
        this.loadingAchat.set(false);
      },
      error: err => this.loadingAchat.set(false),
    });
  }

  private getReglementTiersPayants(query: any): void {
    this.loadingReglement.set(true);
    this.activitySummaryService.getReglementTiersPayants(query).subscribe({
      next: res => {
        this.reglementTiersPayants = res.body;
        this.loadingReglement.set(false);
      },
      error: err => this.loadingReglement.set(false),
    });
  }

  private getAchatTiersPayant(query: any): void {
    this.loadingAchatTp.set(true);
    this.activitySummaryService.getAchatTiersPayant(query).subscribe({
      next: res => {
        this.achatTiersPayant = res.body;
        this.loadingAchatTp.set(false);
      },
      error: err => this.loadingAchatTp.set(false),
    });
  }

  private buildRequest(): any {
    return {
      fromDate: NGB_DATE_TO_ISO(this.fromDate),
      toDate: NGB_DATE_TO_ISO(this.toDate),
      searchAchat: this.searchAchat,
      searchReglement: this.searchReglement,
      page: 0,
      size: 99999,
    };
  }
}
