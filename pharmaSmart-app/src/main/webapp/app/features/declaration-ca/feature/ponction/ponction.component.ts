import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

import { ButtonComponent } from 'app/shared/ui/button/button.component';
import { CardComponent } from 'app/shared/ui/card/card.component';
import { DataTableComponent } from 'app/shared/ui/data-table/data-table.component';
import { AppPillOption, PillSelectorComponent } from 'app/shared/ui/pill-selector/pill-selector.component';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { NotificationService } from 'app/shared/services/notification.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {
  DeclarationCaApiService,
  ModeCalculPonction,
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
    DataTableComponent,
    PillSelectorComponent,
    PharmaDatePickerComponent,
  ],
  templateUrl: './ponction.component.html',
  styleUrl: './ponction.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PonctionComponent implements OnInit {
  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  protected readonly dateDebut = signal<NgbDateStruct | null>(null);
  protected readonly dateFin = signal<NgbDateStruct | null>(null);
  protected readonly modeCalcul = signal<ModeCalculPonction>('POURCENTAGE');
  protected readonly valeur = signal<number | null>(null);
  protected readonly plafond = signal<number | null>(null);
  /** Le plafond de l'officine, rappelé sous le champ pour dire ce qu'on surcharge. */
  protected readonly plafondDefaut = signal<number | null>(null);
  protected readonly commentaire = signal('');

  protected readonly simulation = signal<PonctionSimulation | null>(null);
  protected readonly chargement = signal(false);

  protected readonly modeOptions: AppPillOption[] = [
    { label: 'Pourcentage', value: 'POURCENTAGE', icon: 'pi pi-percentage' },
    { label: 'Montant fixe', value: 'MONTANT_FIXE', icon: 'pi pi-money-bill' },
  ];

  protected readonly saisieComplete = computed(
    () => !!this.dateDebut() && !!this.dateFin() && (this.valeur() ?? 0) > 0,
  );

  /** Valider exige une simulation à jour ET un objectif atteignable. */
  protected readonly peutValider = computed(() => {
    const simulation = this.simulation();
    return !!simulation && simulation.objectifAtteignable && simulation.montantPonctionne > 0;
  });

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
      `<p><strong>${sim.montantPonctionne.toLocaleString('fr')} F</strong> seront retirés du chiffre
       d'affaires à déclarer, sur ${sim.nombreVentesImpactees} vente(s) de la période
       ${sim.dateDebut} → ${sim.dateFin}.</p>
       <p class="mb-0">Cette ponction restera annulable pendant
       <strong>${jours} jour${jours > 1 ? 's' : ''}</strong>. Passé ce délai elle sera
       <strong>définitive</strong> : les chiffres auront pu être lus, imprimés ou transmis, et les
       défaire rendrait un état déjà sorti impossible à reproduire.</p>`,
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
          `Ponction validée : ${ponction.montantPonctionne.toLocaleString('fr')} F retirés du CA à déclarer`,
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
      commentaire: this.commentaire() || undefined,
    };
  }
}
