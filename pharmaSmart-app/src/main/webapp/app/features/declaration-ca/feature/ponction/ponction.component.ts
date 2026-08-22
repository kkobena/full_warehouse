import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgbDateStruct} from '@ng-bootstrap/ng-bootstrap';

import {ButtonComponent} from 'app/shared/ui/button/button.component';
import {CardComponent} from 'app/shared/ui/card/card.component';
import {ToolbarComponent} from 'app/shared/ui/toolbar/toolbar.component';
import {DataTableComponent} from 'app/shared/ui/data-table/data-table.component';
import {HintComponent} from 'app/shared/ui/hint/hint.component';
import {DevisePipe} from 'app/shared/utils/devise';
import {InputNumberComponent} from 'app/shared/ui/input-number/input-number.component';
import {KpiStripComponent} from 'app/shared/ui/kpi-strip/kpi-strip.component';
import {KpiItemComponent} from 'app/shared/ui/kpi-strip/kpi-item.component';
import {
  AppPillOption,
  PillSelectorComponent
} from 'app/shared/ui/pill-selector/pill-selector.component';
import {PharmaDatePickerComponent} from 'app/shared/date-picker/pharma-date-picker.component';
import {NGB_DATE_TO_ISO} from 'app/shared/util/warehouse-util';
import {formatCurrencyWithUnit} from 'app/shared/utils/format-utils';
import {NotificationService} from 'app/shared/services/notification.service';
import {
  NgbConfirmDialogService
} from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {
  DeclarationCaApiService,
  ModeCalculPonction,
  PonctionAssiette,
  PonctionParam,
  PonctionSimulation,
} from '../../data-access/services/declaration-ca-api.service';

/**
 * Écran de ponction : assiette, saisie, simulation, validation.
 *
 * <p>Le fil conducteur est qu'on ne valide **jamais** sans avoir simulé. Toute modification d'un
 * paramètre invalide la simulation affichée : laisser valider sur un résultat périmé produirait une
 * ponction que le pharmacien n'a pas vue.
 */
@Component({
  selector: 'app-ponction',
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    CardComponent,
    ToolbarComponent,
    DataTableComponent,
    HintComponent,
    DevisePipe,
    InputNumberComponent,
    KpiStripComponent,
    KpiItemComponent,
    PillSelectorComponent,
    PharmaDatePickerComponent,
  ],
  templateUrl: './ponction.component.html',
  styleUrl: './ponction.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PonctionComponent implements OnInit {
  protected readonly dateDebut = signal<NgbDateStruct | null>(null);
  protected readonly dateFin = signal<NgbDateStruct | null>(null);
  protected readonly modeCalcul = signal<ModeCalculPonction>('POURCENTAGE');
  protected readonly valeur = signal<number | null>(null);
  protected readonly plafond = signal<number | null>(null);
  /** Le plafond de l'officine, rappelé sous le champ pour dire ce qu'on surcharge. */
  protected readonly plafondDefaut = signal<number | null>(null);
  protected readonly simulation = signal<PonctionSimulation | null>(null);
  protected readonly chargement = signal(false);
  protected readonly modeOptions: AppPillOption[] = [
    {label: 'Pourcentage', value: 'POURCENTAGE', icon: 'pi pi-percentage'},
    {label: 'Montant fixe', value: 'MONTANT_FIXE', icon: 'pi pi-money-bill'},
  ];
  /**
   * L'assiette de la période, obtenue sans objectif.
   *
   * <p>Séparée de la simulation à dessein : celle-ci exige un objectif et le refuse dès qu'il
   * dépasse le maximum, sans jamais dire quel est ce maximum. On répond ici à la question qui vient
   * d'abord — combien est-il possible de prélever sur ces dates ?
   */
  protected readonly assiette = signal<PonctionAssiette | null>(null);
  protected readonly chargementAssiette = signal(false);
  /**
   * Le plafond saisi sort-il de l'intervalle admis ?
   *
   * <p>Même parti pris que pour le taux : `app-input-number` ramènerait 150 à 100 en perdant le
   * focus, sans rien dire. La valeur est conservée, signalée, et refusée par le serveur dans les
   * mêmes termes — le contrôle applicatif ne fait ici que l'annoncer plus tôt.
   */
  protected readonly plafondHorsBornes = computed(() => {
    const plafond = this.plafond();
    return plafond !== null && (plafond > 100 || plafond < 1);
  });
  /** Les dates suffisent : ni objectif ni mode de calcul n'entrent dans l'assiette. */
  protected readonly periodeComplete = computed(
    () => !!this.dateDebut() && !!this.dateFin() && !this.plafondHorsBornes(),
  );
  /**
   * Le taux global demandé dépasse-t-il le plafond par vente ?
   *
   * <p>Aucune vente ne cédant plus que ce plafond, un taux supérieur est mécaniquement
   * inatteignable. Le champ n'est pourtant pas borné en dur : `app-input-number` ramènerait la
   * valeur au maximum en perdant le focus, sans un mot, et l'utilisateur verrait son 60 devenir 35
   * sans comprendre. Il garde donc sa saisie, la voit signalée, et lit pourquoi elle est refusée.
   */
  protected readonly tauxAuDessusDuPlafond = computed(() => {
    if (this.modeCalcul() !== 'POURCENTAGE') {
      return false;
    }
    const valeur = this.valeur();
    const plafond = this.plafond();
    return valeur !== null && plafond !== null && valeur > plafond;
  });
  protected readonly saisieComplete = computed(
    () => !!this.dateDebut() && !!this.dateFin() && (this.valeur() ?? 0) > 0 && !this.tauxAuDessusDuPlafond() && !this.plafondHorsBornes(),
  );
  /** Valider exige une simulation à jour ET un objectif atteignable. */
  protected readonly peutValider = computed(() => {
    const simulation = this.simulation();
    return !!simulation && simulation.objectifAtteignable && simulation.montantPonctionne > 0;
  });
  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  ngOnInit(): void {
    this.api.parametresPonction().subscribe(parametres => {
      this.plafondDefaut.set(parametres.plafondDefaut);
      if (this.plafond() === null) {
        this.plafond.set(parametres.plafondDefaut);
      }
    });
  }

  /** Toute retouche d'un paramètre périme le résultat affiché. */
  protected invaliderSimulation(): void {
    this.simulation.set(null);
  }

  /**
   * L'assiette dépend des dates et du plafond, pas de l'objectif : elle n'est donc pas périmée par
   * une retouche du taux ou du montant, et reste affichée pendant qu'on cherche le bon chiffre.
   */
  protected invaliderAssiette(): void {
    this.assiette.set(null);
    this.invaliderSimulation();
  }

  protected afficherAssiette(): void {
    this.chargementAssiette.set(true);
    this.api
      .assietteePonction(NGB_DATE_TO_ISO(this.dateDebut()!), NGB_DATE_TO_ISO(this.dateFin()!), this.plafond())
      .subscribe({
        next: assiette => {
          this.assiette.set(assiette);
          this.chargementAssiette.set(false);
        },
        error: erreur => {
          this.chargementAssiette.set(false);
          this.notification.error(erreur?.error?.detail ?? "L'assiette n'a pas pu être calculée");
        },
      });
  }

  protected simuler(): void {
    this.chargement.set(true);
    this.api.simuler(this.param()).subscribe({
      next: simulation => {
        this.simulation.set(simulation);
        this.chargement.set(false);
      },
      error: erreur => {
        this.chargement.set(false);
        this.simulation.set(null);
        this.notification.error(erreur?.error?.detail ?? 'La simulation a échoué');
      },
    });
  }

  /**
   * Demande confirmation avant d'appliquer.
   *
   * <p>La ponction n'est réversible que pendant le délai configuré : sans cet avertissement, le
   * pharmacien découvrirait l'irréversibilité au moment où il voudrait revenir en arrière —
   * c'est-à-dire trop tard. Le délai affiché vient du serveur, l'écran ne le devine pas.
   */
  protected demanderConfirmation(): void {
    const sim = this.simulation();
    if (!sim) {
      return;
    }
    const jours = sim.delaiAnnulationJours;
    this.confirmDialog.onConfirm(
      () => this.valider(),
      'Valider la ponction ?',
      `<p><strong>${formatCurrencyWithUnit(sim.montantPonctionne)}</strong> seront retirés du chiffre
       d'affaires à déclarer, sur ${sim.nombreVentesImpactees} vente(s) de la période
       ${sim.dateDebut} → ${sim.dateFin}.</p>
       <p class="mb-0">Cette ponction restera annulable pendant
       <strong>${jours} jour${jours > 1 ? 's' : ''}</strong>. Passé ce délai elle sera
       <strong>définitive</strong>`,
      'pi pi-exclamation-triangle',
    );
  }

  private valider(): void {
    this.chargement.set(true);
    this.api.validerPonction(this.param()).subscribe({
      next: ponction => {
        this.chargement.set(false);
        this.simulation.set(null);
        this.notification.success(
          `Ponction validée : ${formatCurrencyWithUnit(ponction.montantPonctionne)} retirés du CA à déclarer`,
        );
      },
      error: erreur => {
        this.chargement.set(false);
        this.notification.error(erreur?.error?.detail ?? 'La validation a échoué');
      },
    });
  }

  private param(): PonctionParam {
    return {
      dateDebut: NGB_DATE_TO_ISO(this.dateDebut()!),
      dateFin: NGB_DATE_TO_ISO(this.dateFin()!),
      modeCalcul: this.modeCalcul(),
      valeur: this.valeur() ?? 0,
      plafondParVente: this.plafond(),
    };
  }
}
