import { DatePipe } from "@angular/common";
import { ChangeDetectionStrategy, Component, computed, input, output, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ButtonComponent, CheckboxComponent } from "app/shared/ui";
import { IOrderLine } from "app/shared/model/order-line.model";
import { IDataMatrixHint } from "app/shared/model/reception-scan-result.model";

/** Ce que l'utilisateur a tranché : la ligne reconnue, et s'il faut mémoriser le code. */
export interface CodeResolutionChoice {
  ligne: IOrderLine;
  memoriser: boolean;
}

/**
 * Contenu de la résolution manuelle d'un code que le catalogue ne connaît pas. Destiné à être
 * enveloppé par `app-modal`.
 *
 * <p>La boîte scannée correspond forcément à une ligne du bon : on fait donc désigner cette
 * ligne, plutôt que de rouvrir une recherche dans tout le catalogue. Le lot et la péremption lus
 * sur la boîte sont affichés pour que le pharmacien confirme qu'il tient bien le bon article.
 */
@Component({
  selector: "app-code-resolution",
  templateUrl: "./code-resolution.component.html",
  styleUrls: ["./code-resolution.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, FormsModule, ButtonComponent, CheckboxComponent],
})
export class CodeResolutionComponent {
  /** Code produit retenu par le parser, tel qu'il sera mémorisé. */
  readonly scannedCode = input<string>("");

  /** Données extraites de la boîte ; absentes si le code n'était pas un DataMatrix. */
  readonly hint = input<IDataMatrixHint | null>(null);

  /** Lignes du bon en cours, parmi lesquelles choisir. */
  readonly lignes = input<IOrderLine[]>([]);

  readonly valide = output<CodeResolutionChoice>();
  readonly annule = output<void>();

  protected readonly filtre = signal("");
  protected readonly ligneChoisie = signal<IOrderLine | null>(null);
  protected readonly memoriser = signal(true);

  protected readonly lignesFiltrees = computed(() => {
    const recherche = this.filtre().trim().toLowerCase();
    if (!recherche) {
      return this.lignes();
    }
    return this.lignes().filter(
      ligne =>
        (ligne.produitLibelle ?? "").toLowerCase().includes(recherche) ||
        (ligne.produitCip ?? "").toLowerCase().includes(recherche),
    );
  });

  protected readonly peutConfirmer = computed(() => this.ligneChoisie() !== null);

  /** Remet le formulaire à son état initial : la modale est réutilisée d'un scan à l'autre. */
  reinitialiser(): void {
    this.filtre.set("");
    this.ligneChoisie.set(null);
    this.memoriser.set(true);
  }

  protected choisir(ligne: IOrderLine): void {
    this.ligneChoisie.set(ligne);
  }

  protected estChoisie(ligne: IOrderLine): boolean {
    return this.ligneChoisie()?.id === ligne.id;
  }

  protected confirmer(): void {
    const ligne = this.ligneChoisie();
    if (!ligne) {
      return;
    }
    this.valide.emit({ ligne, memoriser: this.memoriser() });
  }

  protected annuler(): void {
    this.annule.emit();
  }
}
