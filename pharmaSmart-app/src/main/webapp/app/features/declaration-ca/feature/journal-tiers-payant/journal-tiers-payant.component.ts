import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';

import {ButtonComponent} from 'app/shared/ui/button/button.component';
import {CardComponent} from 'app/shared/ui/card/card.component';
import {DataTableComponent} from 'app/shared/ui/data-table/data-table.component';
import {HintComponent} from 'app/shared/ui/hint/hint.component';
import {IconFieldComponent} from 'app/shared/ui/icon-field/icon-field.component';
import {ToolbarComponent} from 'app/shared/ui/toolbar/toolbar.component';
import {KpiStripComponent} from 'app/shared/ui/kpi-strip/kpi-strip.component';
import {KpiItemComponent} from 'app/shared/ui/kpi-strip/kpi-item.component';
import {PharmaDatePickerComponent} from 'app/shared/date-picker/pharma-date-picker.component';
import {NGB_DATE_TO_ISO} from 'app/shared/util/warehouse-util';
import {NotificationService} from 'app/shared/services/notification.service';
import {
  DeclarationCaApiService,
  ExclusionItem,
  JournalKpi,
  JournalLigne,
  JournalVente,
} from '../../data-access/services/declaration-ca-api.service';
import {SelectComponent} from "../../../../shared/ui";

/**
 * Journal des ventes écartées au titre d'un tiers-payant exclu.
 */
@Component({
  selector: 'app-journal-tiers-payant',
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    CardComponent,
    DataTableComponent,
    HintComponent,
    IconFieldComponent,
    ToolbarComponent,
    KpiStripComponent,
    KpiItemComponent,
    PharmaDatePickerComponent,
    SelectComponent,
  ],
  templateUrl: './journal-tiers-payant.component.html',
  styleUrl: './journal-tiers-payant.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalTiersPayantComponent implements OnInit {
  protected readonly dateDebut = signal<NgbDateStruct>(debutDuMois());
  protected readonly dateFin = signal<NgbDateStruct>(aujourdHui());
  protected readonly recherche = signal('');
  protected readonly tiersPayantId = signal<number | null>(null);
  protected readonly ventes = signal<JournalVente[]>([]);
  protected readonly kpi = signal<JournalKpi | null>(null);
  protected readonly chargement = signal(false);
  protected readonly tronque = signal(false);
  /** Seuls les tiers-payants exclus alimentent ce journal : proposer les autres serait trompeur. */
  protected readonly tiersPayants = signal<ExclusionItem[]>([]);
  protected readonly selection = signal<JournalVente | null>(null);
  protected readonly detail = signal<JournalLigne[]>([]);
  protected readonly chargementDetail = signal(false);
  protected readonly panelOpen = computed(() => this.selection() !== null);
  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);

  ngOnInit(): void {
    this.chargerTiersPayants();
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.fermerDetail();
    this.api
      .journal('tiers-payants', {
        dateDebut: NGB_DATE_TO_ISO(this.dateDebut()),
        dateFin: NGB_DATE_TO_ISO(this.dateFin()),
        recherche: this.recherche().trim(),
        tiersPayantId: this.tiersPayantId(),
      })
      .subscribe({
        next: journal => {
          this.ventes.set(journal.ventes);
          this.kpi.set(journal.kpi);
          this.tronque.set(journal.tronque);
          this.chargement.set(false);
        },
        error: () => {
          this.chargement.set(false);
          this.notification.error('Impossible de charger le journal');
        },
      });
  }

  protected onSelection(vente: JournalVente | null): void {
    this.selection.set(vente);
    if (!vente) {
      this.detail.set([]);
      return;
    }
    this.chargementDetail.set(true);
    this.api.lignesDeLaVente(vente.saleId, vente.saleDate).subscribe({
      next: lignes => {
        this.detail.set(lignes);
        this.chargementDetail.set(false);
      },
      error: () => {
        this.chargementDetail.set(false);
        this.notification.error('Impossible de charger le détail de la vente');
      },
    });
  }

  protected fermerDetail(): void {
    this.selection.set(null);
    this.detail.set([]);
  }

  private chargerTiersPayants(): void {
    this.api.lister('tiers-payants', true).subscribe({
      next: items => this.tiersPayants.set(items),
      // Silencieux : le filtre est un confort, son absence n'empêche pas de consulter le journal.
      error: () => this.tiersPayants.set([]),
    });
  }
}

function aujourdHui(): NgbDateStruct {
  const maintenant = new Date();
  return {
    year: maintenant.getFullYear(),
    month: maintenant.getMonth() + 1,
    day: maintenant.getDate()
  };
}

/** Le mois en cours : la période sur laquelle porte la déclaration à venir. */
function debutDuMois(): NgbDateStruct {
  const maintenant = new Date();
  return {year: maintenant.getFullYear(), month: maintenant.getMonth() + 1, day: 1};
}
