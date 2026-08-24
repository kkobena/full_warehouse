import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal, input} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';

import { BadgeComponent } from 'app/shared/ui/badge/badge.component';
import { ButtonComponent } from 'app/shared/ui/button/button.component';
import { CardComponent } from 'app/shared/ui/card/card.component';
import { ToolbarComponent } from 'app/shared/ui/toolbar/toolbar.component';
import { HintComponent } from 'app/shared/ui/hint/hint.component';
import { PharmaDatePickerComponent } from 'app/shared/date-picker/pharma-date-picker.component';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { NotificationService } from 'app/shared/services/notification.service';
import { Anomalie, DeclarationCaApiService } from '../../data-access/services/declaration-ca-api.service';

/**
 * Contrôle de cohérence du chiffre d'affaires déclaré.
 *
 * <p>Le montant déclaré vit à trois endroits — les lignes, la vente, les règlements — et rien dans le
 * schéma n'empêche ces valeurs de diverger. Cet écran répond à une seule question : « puis-je faire
 * confiance à ce que mes états affichent ? »
 *
 * <p>Une anomalie est montrée avec sa <strong>conséquence</strong> et quelques exemples. Un compteur
 * seul — « 12 anomalies » — n'aiderait personne à comprendre ce qu'il risque, ni à retrouver les
 * ventes concernées.
 */
@Component({
  selector: 'app-audit-declaration-ca',
  imports: [
    CommonModule,
    FormsModule,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    ToolbarComponent,
    HintComponent,
    PharmaDatePickerComponent,
  ],
  templateUrl: './audit.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit alors le libellé du menu — ou son
   * `titre_long` quand la barre nomme plus longuement — au lieu d'une valeur figée dans le gabarit.
   */
  readonly navCode = input<string>('');

  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);

  protected readonly dateDebut = signal<NgbDateStruct | null>(null);
  protected readonly dateFin = signal<NgbDateStruct | null>(null);
  protected readonly anomalies = signal<Anomalie[]>([]);
  protected readonly chargement = signal(false);
  protected readonly dejaControle = signal(false);

  protected readonly rompus = computed(() => this.anomalies().filter(a => a.nombreAnomalies > 0));
  protected readonly toutEstSain = computed(() => this.dejaControle() && this.rompus().length === 0);

  ngOnInit(): void {
    this.controler();
  }

  /** Sans période saisie, le contrôle porte sur tout l'historique — c'est le cas par défaut. */
  protected controler(): void {
    this.chargement.set(true);
    const debut = this.dateDebut();
    const fin = this.dateFin();
    const borne = debut && fin;
    this.api.auditer(borne ? NGB_DATE_TO_ISO(debut) : undefined, borne ? NGB_DATE_TO_ISO(fin) : undefined).subscribe({
      next: anomalies => {
        this.anomalies.set(anomalies);
        this.dejaControle.set(true);
        this.chargement.set(false);
      },
      error: () => {
        this.chargement.set(false);
        this.notification.error('Le contrôle de cohérence a échoué');
      },
    });
  }
}
