import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from "@angular/core";
import { DecimalPipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbModal, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  IconFieldComponent,
  ToolbarComponent
} from "../../../../shared/ui";
// Non réexporté par shared/ui : import direct.
import { PageLayoutComponent } from "../../../../shared/ui/page-layout/page-layout.component";
import { DciApiService, DciQuery } from "../../data-access/services/dci-api.service";
import { IDci, IDciProduit } from "../../models/dci.model";
import { AppBadgeSeverity } from "../../../../shared/ui/badge/badge.component";
import { CardComponent } from "../../../../shared/ui/card/card.component";
import { HintComponent } from "../../../../shared/ui/hint/hint.component";
import { SelectableRowDirective } from "../../../../shared/ui/data-table/selectable-row.directive";
import { DciFormComponent } from "../../ui/dci-form/dci-form.component";
import { DciImportComponent } from "../../ui/dci-import/dci-import.component";

/**
 * Référentiel des Dénominations Communes Internationales.
 *
 * La liste est une `httpResource` pilotée par un signal de requête : modifier la
 * recherche ou la page suffit à relancer l'appel, sans abonnement à gérer. Après
 * une écriture, `reload()` force le rafraîchissement.
 */
@Component({
  selector: "app-dci-home",
  templateUrl: "./dci-home.component.html",
  styleUrl: "./dci-home.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    NgbTooltip,
    PageLayoutComponent,
    ToolbarComponent,
    ButtonComponent,
    IconFieldComponent,
    DataTableComponent,
    SelectableRowDirective,
    CardComponent,
    HintComponent,
    DecimalPipe
  ]
})
export class DciHomeComponent {
  protected readonly page = signal(0);
  protected readonly rows = signal(ITEMS_PER_PAGE);
  protected readonly search = signal("");

  /** Saisie brute du champ de recherche ; n'est reportée qu'à la validation. */
  protected saisie = "";
  protected readonly first = computed(() => this.page() * this.rows());
  protected readonly dcis = computed(() => this.resource.value() ?? []);
  protected readonly loading = computed(() => this.resource.isLoading());
  protected readonly enErreur = computed(() => this.resource.error() !== undefined);

  // ── Maître / détail ────────────────────────────────────────────────────────
  protected readonly selection = signal<IDci | null>(null);
  protected readonly produits = signal<IDciProduit[]>([]);
  protected readonly chargementDetail = signal(false);
  protected readonly panelOpen = computed(() => this.selection() !== null);
  private readonly api = inject(DciApiService);
  protected readonly total = computed(() => this.api.totalFromHeaders(this.resource));
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notification = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly query = computed<DciQuery>(() => ({
    page: this.page(),
    size: this.rows(),
    search: this.search()
  }));
  protected readonly resource = this.api.pagedResource(this.query);

  constructor() {
    // Une erreur de chargement doit se voir : sans cela, la table reste
    // simplement vide et l'utilisateur conclut qu'il n'y a pas de données.
    effect(() => {
      if (this.resource.error()) {
        this.notification.error("Chargement des DCI impossible.");
      }
    });
  }

  protected onSelection(dci: IDci | null): void {
    this.selection.set(dci);
    this.produits.set([]);
    if (!dci?.id) {
      return;
    }
    this.chargementDetail.set(true);
    this.api.produits(dci.id).subscribe({
      next: liste => {
        this.produits.set(liste);
        this.chargementDetail.set(false);
      },
      error: (err: unknown) => {
        this.chargementDetail.set(false);
        this.notification.error(
          this.errorService.getErrorMessage(err, "Chargement des produits impossible.")
        );
      }
    });
  }

  protected fermerDetail(): void {
    this.selection.set(null);
    this.produits.set([]);
  }

  protected severiteStatut(statut: string | null): AppBadgeSeverity {
    return statut === "ENABLE" ? "success" : "secondary";
  }

  protected onSearch(): void {
    this.page.set(0);
    this.search.set(this.saisie.trim());
    // La DCI sélectionnée peut ne plus figurer dans les résultats : garder le
    // panneau ouvert sur une ligne absente de la liste serait déroutant.
    this.fermerDetail();
  }

  protected onLazyLoad(event: AppTableLazyLoadEvent): void {
    const taille = event.rows || ITEMS_PER_PAGE;
    this.rows.set(taille);
    this.page.set(Math.floor((event.first ?? 0) / taille));
  }

  protected async creer(): Promise<void> {
    await this.ouvrirFormulaire();
  }

  /**
   * Les actions de ligne arrêtent la propagation : sans cela, le clic remonte
   * jusqu'à la ligne sélectionnable et ouvre le détail en même temps qu'il
   * déclenche l'action, ce qui donne deux effets pour un seul geste.
   */
  protected voirDetail(dci: IDci, evenement: MouseEvent): void {
    evenement.stopPropagation();
    this.onSelection(dci);
  }

  protected async modifier(dci: IDci, evenement?: MouseEvent): Promise<void> {
    evenement?.stopPropagation();
    await this.ouvrirFormulaire(dci);
  }

  protected async importer(): Promise<void> {
    const ref = this.modalService.open(DciImportComponent, {
      backdrop: "static",
      centered: true,
      size: "lg"
    });
    const aImporte = await ref.result.catch((): boolean => false);
    if (aImporte) {
      this.page.set(0);
      this.resource.reload();
    }
  }

  protected supprimer(dci: IDci, evenement?: MouseEvent): void {
    evenement?.stopPropagation();
    this.confirmDialog.onConfirm(
      () => {
        this.api.delete(dci.id!).subscribe({
          next: () => {
            this.notification.success("DCI supprimée.");
            this.fermerDetail();
            this.resource.reload();
          },
          // Le serveur refuse la suppression d'une DCI rattachée à des produits et
          // dit combien : on relaie SON message plutôt qu'une supposition.
          error: (err: unknown) =>
            this.notification.error(
              this.errorService.getErrorMessage(err, "Suppression impossible.")
            )
        });
      },
      "Supprimer la DCI",
      `Confirmez-vous la suppression de « ${dci.libelle} » ?`,
      "pi pi-exclamation-triangle"
    );
  }

  private async ouvrirFormulaire(dci?: IDci): Promise<void> {
    const ref = this.modalService.open(DciFormComponent, {
      backdrop: "static",
      centered: true,
      size: "lg"
    });
    (ref.componentInstance as DciFormComponent).init(dci);
    const resultat = await ref.result.catch((): IDci | null => null);
    if (resultat) {
      this.notification.success(dci?.id ? "DCI modifiée." : "DCI créée.");
      this.resource.reload();
    }
  }
}
