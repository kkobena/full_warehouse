import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal, input} from "@angular/core";
import {CommonModule} from "@angular/common";
import {NgbDateStruct, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";

import {ButtonComponent} from "app/shared/ui/button/button.component";
import {BadgeComponent} from "app/shared/ui/badge/badge.component";
import {CardComponent} from "app/shared/ui/card/card.component";
import {DataTableComponent} from "app/shared/ui/data-table/data-table.component";
import {SelectableRowDirective} from "app/shared/ui/data-table/selectable-row.directive";
import {DeviseDirective, DevisePipe} from "app/shared/utils/devise";
import {HintComponent} from "app/shared/ui/hint/hint.component";
import {ToolbarComponent} from "app/shared/ui/toolbar/toolbar.component";
import {KpiStripComponent} from "app/shared/ui/kpi-strip/kpi-strip.component";
import {KpiItemComponent} from "app/shared/ui/kpi-strip/kpi-item.component";
import {PharmaDatePickerComponent} from "app/shared/date-picker/pharma-date-picker.component";
import {NGB_DATE_TO_ISO} from "app/shared/util/warehouse-util";
import {NotificationService} from "app/shared/services/notification.service";
import {
  NgbConfirmDialogService
} from "app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {
  DeclarationCaApiService,
  Ponction,
  PonctionLigne,
  StatutPonction
} from "../../data-access/services/declaration-ca-api.service";
import {BlobDownloadService} from "../../../../shared/services/blob-download.service";
import {formatCurrencyWithUnit} from "app/shared/utils/format-utils";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

/**
 * Les cumuls du bandeau, calculés sur la période affichée.
 *
 * <p>Les montants ne retiennent que les ponctions <strong>validées</strong> : une ponction annulée a
 * été rétablie sur ses ventes, son montant n'est plus retiré de quoi que ce soit et l'additionner
 * gonflerait un total qui ne correspond à rien de déclaré.
 */
interface ResumePonctions {
  nombre: number;
  nombreValidees: number;
  nombreAnnulees: number;
  caReel: number;
  caDeclare: number;
  montantPonctionne: number;
  nombreVentes: number;
  /** Part réellement retirée du CA après exclusions, en pourcentage. */
  tauxEffectif: number;
}

/**
 * Historique des ponctions : ce qui a été fait, par qui, et comment le défaire.
 *
 * <p>Présenté en maître / détail plutôt qu'en tableau imbriqué : une ponction peut toucher plusieurs
 * centaines de ventes, et les déplier dans la ligne repousserait la suite de la liste hors de
 * l'écran. Le panneau latéral garde la liste visible pendant qu'on lit le détail.
 *
 * <p>Le filtre de période est appliqué par le serveur, sur le critère du recouvrement : une ponction
 * à cheval sur deux mois reste visible depuis l'un comme depuis l'autre.
 */
@Component({
  selector: "app-ponction-historique",
  imports: [CommonModule, FormsModule, NgbTooltip, ButtonComponent, BadgeComponent, CardComponent, DataTableComponent, SelectableRowDirective, DeviseDirective, DevisePipe, HintComponent, ToolbarComponent, KpiStripComponent, KpiItemComponent, PharmaDatePickerComponent, ReactiveFormsModule],
  templateUrl: "./ponction-historique.component.html",
  styleUrl: "./ponction-historique.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PonctionHistoriqueComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit alors le libellé du menu — ou son
   * `titre_long` quand la barre nomme plus longuement — au lieu d'une valeur figée dans le gabarit.
   */
  readonly navCode = input<string>('');

  /**
   * Période affichée à l'ouverture : l'année civile en cours.
   *
   * <p>Plus large que le mois retenu par les journaux : une officine ne ponctionne pas toutes les
   * semaines, et s'ouvrir sur un tableau vide donnerait à croire qu'aucune ponction n'existe.
   */
  protected readonly dateDebut = signal<NgbDateStruct>(debutDeLAnnee());
  protected readonly dateFin = signal<NgbDateStruct>(aujourdHui());

  protected readonly ponctions = signal<Ponction[]>([]);
  protected readonly chargement = signal(false);
  protected readonly chargementDetail = signal(false);
  protected readonly selection = signal<Ponction | null>(null);
  protected readonly detail = signal<PonctionLigne[]>([]);
  protected readonly panelOpen = computed(() => this.selection() !== null);

  /**
   * Les cumuls de la période, calculés ici et non demandés au serveur : l'historique est renvoyé
   * en entier, et un second appel donnerait deux vérités calculées à deux instants différents.
   */
  protected readonly resume = computed<ResumePonctions>(() => this.cumuler(this.ponctions()));

  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnInit(): void {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    // Le détail ouvert porte sur une ponction qui peut sortir de la nouvelle période : on referme
    // plutôt que de laisser à l'écran un panneau sans ligne correspondante dans la liste.
    this.fermerDetail();
    this.api
      .historiquePonctions(NGB_DATE_TO_ISO(this.dateDebut()), NGB_DATE_TO_ISO(this.dateFin()))
      .subscribe({
        next: ponctions => {
          this.ponctions.set(ponctions);
          this.chargement.set(false);
        },
        error: () => {
          this.chargement.set(false);
          this.notification.error("Impossible de charger l'historique");
        }
      });
  }

  protected onSelection(ponction: Ponction | null): void {
    this.selection.set(ponction);
    if (!ponction) {
      this.detail.set([]);
      return;
    }
    this.chargementDetail.set(true);
    this.api.detailPonction(ponction.id).subscribe({
      next: lignes => {
        this.detail.set(lignes);
        this.chargementDetail.set(false);
      },
      error: () => {
        this.chargementDetail.set(false);
        this.notification.error("Impossible de charger le détail");
      }
    });
  }

  protected fermerDetail(): void {
    this.selection.set(null);
    this.detail.set([]);
  }

  /**
   * Seule une ponction validée s'annule. Le délai, lui, est vérifié par le serveur : le poste peut
   * être à l'heure de la veille, et une ponction définitive ne doit pas dépendre de son horloge.
   */
  protected annulable(statut: StatutPonction): boolean {
    return statut === "VALIDEE";
  }

  /**
   * Justificatif PDF : la cascade d'assiette, le plafond, puis le détail des ventes.
   *
   * <p>Ouvert dans un onglet plutôt que téléchargé : le pharmacien le consulte le plus souvent pour
   * vérifier un chiffre, pas pour l'archiver.
   */
  protected imprimer(ponction: Ponction, evenement: Event): void {
    evenement.stopPropagation();
    this.api.justificatifPdf(ponction.id).subscribe({
      next: blob => {
        this.blobDownloadService.downloadPdf(blob, "justificatif_ponction");
      },
      error: () => this.notification.error("Le justificatif n'a pas pu être généré")
    });
  }


  /**
   * Demande confirmation, puis annule.
   *
   * <p>L'annulation est réversible — une nouvelle ponction peut être passée sur la même période —
   * mais elle touche des centaines de ventes d'un seul clic, sur une ligne de tableau où le curseur
   * passe forcément. C'est cela qu'on confirme : l'ampleur du geste, pas son irréversibilité.
   *
   * <p>L'événement est stoppé pour que le clic n'ouvre pas aussi le panneau de détail.
   */
  protected demanderAnnulation(ponction: Ponction, evenement: Event): void {
    evenement.stopPropagation();
    this.confirmDialog.onConfirm(
      () => this.annuler(ponction),
      'Annuler cette ponction ?',
      `<p>Les <strong>${formatCurrencyWithUnit(ponction.montantPonctionne)}</strong> retirés du chiffre
       d'affaires à déclarer seront <strong>rétablis</strong> sur les
       ${ponction.nombreVentes} vente(s) de la période ${ponction.dateDebut} → ${ponction.dateFin}.</p>
       <p class="mb-0">La période redeviendra disponible pour une nouvelle ponction.</p>`,
      'pi pi-undo',
    );
  }

  protected severite(statut: StatutPonction): "success" | "secondary" | "danger" {
    switch (statut) {
      case "VALIDEE":
        return "success";
      case "ANNULEE":
        return "secondary";
      default:
        return "danger";
    }
  }

  protected tauxEffectif(ponction: Ponction): string {
    if (ponction.caApresExclusions <= 0) {
      return "—";
    }
    return ((ponction.montantPonctionne * 100) / ponction.caApresExclusions).toFixed(2) + " %";
  }

  /** Cumule la période. Voir {@link ResumePonctions} pour le sort réservé aux ponctions annulées. */
  private cumuler(ponctions: Ponction[]): ResumePonctions {
    const validees = ponctions.filter(ponction => ponction.statut === "VALIDEE");
    const caApresExclusions = validees.reduce((total, p) => total + p.caApresExclusions, 0);
    const montantPonctionne = validees.reduce((total, p) => total + p.montantPonctionne, 0);
    return {
      nombre: ponctions.length,
      nombreValidees: validees.length,
      nombreAnnulees: ponctions.filter(ponction => ponction.statut === "ANNULEE").length,
      caReel: validees.reduce((total, p) => total + p.caReel, 0),
      caDeclare: validees.reduce((total, p) => total + p.caDeclare, 0),
      montantPonctionne,
      nombreVentes: validees.reduce((total, p) => total + p.nombreVentes, 0),
      tauxEffectif: caApresExclusions > 0 ? (montantPonctionne * 100) / caApresExclusions : 0
    };
  }

  private annuler(ponction: Ponction): void {
    this.chargement.set(true);
    this.api.annulerPonction(ponction.id).subscribe({
      next: () => {
        this.notification.success("Ponction annulée : les montants d'origine sont rétablis");
        this.fermerDetail();
        this.charger();
      },
      error: erreur => {
        this.chargement.set(false);
        this.notification.error(erreur?.error?.detail ?? "L'annulation a échoué");
      }
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

/** Le 1er janvier de l'année en cours : l'exercice sur lequel porte la déclaration. */
function debutDeLAnnee(): NgbDateStruct {
  return {year: new Date().getFullYear(), month: 1, day: 1};
}
