import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

import { ButtonComponent } from 'app/shared/ui/button/button.component';
import { DataTableComponent } from 'app/shared/ui/data-table/data-table.component';
import { HintComponent } from 'app/shared/ui/hint/hint.component';
import { IconFieldComponent } from 'app/shared/ui/icon-field/icon-field.component';
import { ToolbarComponent } from 'app/shared/ui/toolbar/toolbar.component';
import { KpiStripComponent } from 'app/shared/ui/kpi-strip/kpi-strip.component';
import { KpiItemComponent } from 'app/shared/ui/kpi-strip/kpi-item.component';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { NotificationService } from 'app/shared/services/notification.service';
import { DeclarationCaApiService, JournalKpi, JournalLigne } from '../../data-access/services/declaration-ca-api.service';

/** Les deux journaux qui se lisent ligne à ligne. Le tiers-payant, lui, se lit par vente. */
export type TypeJournalLignes = 'unites-gratuites' | 'rayons';

/**
 * Journal des lignes écartées du chiffre d'affaires à déclarer — unités gratuites ou rayons exclus.
 *
 * <p>Un seul composant pour les deux, comme pour les écrans d'exclusion : mêmes filtres, même
 * tableau, mêmes indicateurs. Seules deux colonnes changent, et un `@if` coûte moins qu'un second
 * composant qui divergerait dès la première évolution portée d'un seul côté.
 *
 * <p>La marge est la colonne qui justifie l'écran. Un pharmacien qui exclut un rayon voit le chiffre
 * qu'il retire ; il ne voit nulle part ailleurs ce que ce chiffre lui rapportait réellement.
 */
@Component({
  selector: 'app-journal-lignes',
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    DataTableComponent,
    HintComponent,
    IconFieldComponent,
    ToolbarComponent,
    KpiStripComponent,
    KpiItemComponent,
    PharmaDatePickerComponent,
  ],
  templateUrl: './journal-lignes.component.html',
  styleUrl: './journal-lignes.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalLignesComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit alors le libellé du menu — ou son
   * `titre_long` quand la barre nomme plus longuement — au lieu d'une valeur figée dans le gabarit.
   */
  readonly navCode = input<string>('');

  readonly type = input.required<TypeJournalLignes>();
  readonly titre = input.required<string>();
  /** Ce que le journal recense, rappelé au-dessus des filtres. */
  readonly explication = input.required<string>();

  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);

  protected readonly dateDebut = signal<NgbDateStruct>(debutDuMois());
  protected readonly dateFin = signal<NgbDateStruct>(aujourdHui());
  protected readonly recherche = signal('');

  protected readonly lignes = signal<JournalLigne[]>([]);
  protected readonly kpi = signal<JournalKpi | null>(null);
  protected readonly chargement = signal(false);
  protected readonly tronque = signal(false);

  protected readonly estUg = computed(() => this.type() === 'unites-gratuites');

  /** L'icône reprend celle de l'onglet correspondant : l'écran et son menu se répondent. */
  protected readonly icone = computed(() => (this.estUg() ? 'pi pi-gift' : 'pi pi-th-large'));

  ngOnInit(): void {
    this.charger();
  }

  /**
   * Recharge depuis le serveur.
   *
   * <p>À la validation, jamais à la frappe, et côté serveur plutôt que sur les lignes déjà
   * chargées : celles-ci sont plafonnées, et filtrer localement laisserait croire qu'un produit
   * absent du tableau n'a pas été exclu, alors qu'il est simplement au-delà du plafond.
   */
  protected charger(): void {
    this.chargement.set(true);
    this.api
      .journal(this.type(), {
        dateDebut: NGB_DATE_TO_ISO(this.dateDebut()),
        dateFin: NGB_DATE_TO_ISO(this.dateFin()),
        recherche: this.recherche().trim(),
      })
      .subscribe({
        next: journal => {
          this.lignes.set(journal.lignes);
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

}

function aujourdHui(): NgbDateStruct {
  const maintenant = new Date();
  return { year: maintenant.getFullYear(), month: maintenant.getMonth() + 1, day: maintenant.getDate() };
}

/**
 * Le mois en cours par défaut.
 *
 * <p>C'est la période sur laquelle porte la déclaration à venir : l'écran s'ouvre donc sur ce que le
 * pharmacien va effectivement déclarer, et non sur un intervalle vide qu'il faudrait renseigner
 * avant de voir quoi que ce soit.
 */
function debutDuMois(): NgbDateStruct {
  const maintenant = new Date();
  return { year: maintenant.getFullYear(), month: maintenant.getMonth() + 1, day: 1 };
}
