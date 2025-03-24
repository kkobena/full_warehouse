import { Component, computed, inject, signal } from '@angular/core';
import { ActivitySummaryService } from './activity-summary.service';
import { ChiffreAffaire } from './model/chiffre-affaire.model';
import { GroupeFournisseurAchat } from './model/groupe-fournisseur-achat.model';
import { ReglementTiersPayant } from './model/reglement-tiers-payant.model';
import { AchatTiersPayant } from './model/achat-tiers-payant.model';
import { CommonModule } from '@angular/common';
import { Toolbar } from 'primeng/toolbar';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { FormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { Subscription } from 'rxjs';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'jhi-activity-summary',
  imports: [CommonModule, Toolbar, DatePicker, FloatLabel, FormsModule, Button, IconField, InputIcon, InputText, TableModule],
  templateUrl: './activity-summary.component.html',
  styleUrl: './activity-summary.component.scss',
})
export class ActivitySummaryComponent {
  protected loadingPdf = false;
  protected chiffreAffaire: ChiffreAffaire | null = null;
  protected groupeFournisseurAchats: GroupeFournisseurAchat[] | null = [];
  protected reglementTiersPayants: ReglementTiersPayant[] | null = [];
  protected achatTiersPayant: AchatTiersPayant[] | null = [];
  protected fromDate: Date | null = new Date();
  protected toDate: Date | null = new Date();
  protected searchAchat: string | null = null;
  protected searchReglement: string | null = null;
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly activitySummaryService = inject(ActivitySummaryService);
  private primngtranslate: Subscription;
  // protected scrollHeight = 'calc(100vh - 350px)';
  protected loadingCa = signal(false);
  protected loadingAchat = signal(false);
  protected loadingReglement = signal(false);
  protected loadingAchatTp = signal(false);
  protected loadingBtn = computed(() => this.loadingCa() || this.loadingAchat() || this.loadingReglement() || this.loadingAchatTp());

  constructor() {
    this.translate.use('fr');
    this.primngtranslate = this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });

    this.loadAll();
  }

  protected loadAll(): void {
    const query = this.buildRequest();
    this.queryCa(query);
    this.getGroupeFournisseurAchat(query);
    this.getReglementTiersPayants(query);
    this.getAchatTiersPayant(query);
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

  protected printAll(): void {
    this.loadingPdf = true;
    this.activitySummaryService.onPrintPdf(this.buildRequest()).subscribe({
      next: blod => {
        this.loadingPdf = false;
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
      error: () => (this.loadingPdf = false),
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
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      searchAchat: this.searchAchat,
      searchReglement: this.searchReglement,
      page: 0,
      size: 99999,
    };
  }

  protected getRecetteTotal(): number {
    return this.chiffreAffaire?.recettes?.reduce((acc, val) => acc + val.montantReel, 0) || 0;
  }

  protected getTotalMouvementCaisse(): number {
    return this.chiffreAffaire?.mouvementCaisses?.reduce((acc, val) => acc + val.montant, 0) || 0;
  }
}
