import { DatePipe } from "@angular/common";
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { BadgeComponent, ButtonComponent, DetailGridComponent, DetailSectionComponent } from "../../../../shared/ui";
import { IProduit } from "../../../../shared/model";
import { CodeAliasType, IProduitCodeAlias } from "../../../../shared/model/reception-scan-result.model";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { ErrorService } from "../../../../shared/error.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ProductsApiService } from "../../data-access/services/products-api.service";

/**
 * Codes alternatifs d'un produit : GTIN du fabricant, EAN d'une autre boîte, code interne d'un
 * grossiste.
 *
 * <p>En Côte d'Ivoire les grossistes n'intègrent pas les codes du DataMatrix : le code encodé par
 * le fabricant ne correspond à rien en base. Mémorisé ici, il est reconnu seul à la réception
 * comme au comptoir.
 */
@Component({
  selector: "app-produit-codes-alias-tab",
  templateUrl: "./produit-codes-alias-tab.component.html",
  styleUrl: "./produit-codes-alias-tab.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, BadgeComponent, ButtonComponent, DetailGridComponent, DetailSectionComponent, NgbTooltip],
})
export class ProduitCodesAliasTabComponent {
  readonly produit = input.required<IProduit>();

  private readonly api = inject(ProductsApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

  protected readonly alias = signal<IProduitCodeAlias[]>([]);
  protected readonly chargement = signal(false);
  protected readonly nouveauCode = signal("");

  /** Un produit désactivé ne doit plus recevoir de nouveau code. */
  protected readonly estDesactive = computed(() => this.produit().status === "DISABLE");

  protected readonly peutAjouter = computed(() => !this.estDesactive() && this.nouveauCode().trim().length > 0);

  constructor() {
    effect(() => {
      const id = this.produit().id;
      if (id) {
        this.charger(id);
      }
    });
  }

  /** Libellés des types : l'utilisateur n'a pas à connaître la nomenclature GS1. */
  protected libelleType(type: CodeAliasType): string {
    switch (type) {
      case "GTIN_14":
        return "GTIN fabricant";
      case "EAN_13":
        return "EAN-13";
      case "EAN_8":
        return "EAN-8";
      case "CIP_13":
        return "CIP-13";
      case "CIP_7":
        return "CIP-7";
      default:
        return "Code interne";
    }
  }

  protected ajouter(): void {
    const id = this.produit().id;
    const code = this.nouveauCode().trim();
    if (!id || !code || this.estDesactive()) {
      return;
    }
    this.api.createCodeAlias(id, code).subscribe({
      next: cree => {
        this.nouveauCode.set("");
        // Le serveur est idempotent : un code déjà présent revient à l'identique.
        this.alias.update(liste => (liste.some(a => a.id === cree.id) ? liste : [cree, ...liste]));
      },
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur"),
    });
  }

  protected supprimer(alias: IProduitCodeAlias): void {
    this.confirmDialog.onConfirm(
      () =>
        this.api.deleteCodeAlias(alias.id).subscribe({
          next: () => this.alias.update(liste => liste.filter(a => a.id !== alias.id)),
          error: err => this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur"),
        }),
      "Retirer ce code",
      `Le code ${alias.code} ne sera plus reconnu au scan. La fiche produit n'est pas modifiée.`,
    );
  }

  private charger(produitId: number): void {
    this.chargement.set(true);
    this.api.getCodesAlias(produitId).subscribe({
      next: liste => {
        this.alias.set(liste);
        this.chargement.set(false);
      },
      error: err => {
        this.chargement.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur");
      },
    });
  }
}
