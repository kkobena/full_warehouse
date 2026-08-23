import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal, input} from "@angular/core";
import {CommonModule} from "@angular/common";
import {NgbTooltip} from "@ng-bootstrap/ng-bootstrap";

import {ButtonComponent} from "app/shared/ui/button/button.component";
import {BadgeComponent} from "app/shared/ui/badge/badge.component";
import {CardComponent} from "app/shared/ui/card/card.component";
import {DataTableComponent} from "app/shared/ui/data-table/data-table.component";
import {SelectableRowDirective} from "app/shared/ui/data-table/selectable-row.directive";
import {DeviseDirective, DevisePipe} from "app/shared/utils/devise";
import {HintComponent} from "app/shared/ui/hint/hint.component";
import {ToolbarComponent} from "app/shared/ui/toolbar/toolbar.component";
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
import {ReactiveFormsModule} from "@angular/forms";

/**
 * Historique des ponctions : ce qui a été fait, par qui, et comment le défaire.
 *
 * <p>Présenté en maître / détail plutôt qu'en tableau imbriqué : une ponction peut toucher plusieurs
 * centaines de ventes, et les déplier dans la ligne repousserait la suite de la liste hors de
 * l'écran. Le panneau latéral garde la liste visible pendant qu'on lit le détail.
 */
@Component({
  selector: "app-ponction-historique",
  imports: [CommonModule, NgbTooltip, ButtonComponent, BadgeComponent, CardComponent, DataTableComponent, SelectableRowDirective, DeviseDirective, DevisePipe, HintComponent, ToolbarComponent, ReactiveFormsModule],
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

  protected readonly ponctions = signal<Ponction[]>([]);
  protected readonly chargement = signal(false);
  protected readonly chargementDetail = signal(false);
  protected readonly selection = signal<Ponction | null>(null);
  protected readonly detail = signal<PonctionLigne[]>([]);
  protected readonly panelOpen = computed(() => this.selection() !== null);
  private readonly api = inject(DeclarationCaApiService);
  private readonly notification = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly blobDownloadService = inject(BlobDownloadService);

  ngOnInit(): void {
    this.charger();
  }

  protected charger(): void {
    this.chargement.set(true);
    this.api.historiquePonctions().subscribe({
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
