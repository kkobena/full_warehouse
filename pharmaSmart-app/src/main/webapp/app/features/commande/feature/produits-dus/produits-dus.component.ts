import {ChangeDetectionStrategy, Component, computed, inject, OnInit, output, signal} from "@angular/core";
import {CommonModule} from "@angular/common";
import {NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {BadgeComponent, ButtonComponent, DataTableComponent} from "app/shared/ui";
import {CommandeService} from "app/entities/commande/commande.service";
import {IProduitDuNonCommande} from "app/shared/model/produit-du.model";

/**
 * « Qu'ai-je promis que je n'ai pas encore commandé ? »
 *
 * Le circuit d'avoir client savait rapprocher les avoirs d'une livraison reçue, mais rien ne
 * disait l'inverse : un produit dû à un client et jamais commandé n'apparaissait nulle part. Le
 * mécanisme était réactif — il exploitait la réception — sans jamais déclencher la commande.
 */
@Component({
  selector: "app-produits-dus",
  templateUrl: "./produits-dus.component.html",
  styleUrls: ["./produits-dus.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, DataTableComponent, BadgeComponent, ButtonComponent, NgbTooltip],
})
export class ProduitsDusComponent implements OnInit {
  /** Remonte le nombre de produits dus pour la pastille de l'onglet. */
  readonly countChange = output<number>();

  protected readonly produits = signal<IProduitDuNonCommande[]>([]);
  protected readonly isLoading = signal(false);

  /** Unités promises, tous produits confondus. */
  protected readonly totalDu = computed(() =>
    this.produits().reduce((somme, p) => somme + p.quantiteDue, 0),
  );

  /** Produits dont le stock couvre déjà le dû : servables au comptoir, sans rien commander. */
  protected readonly servablesMaintenant = computed(
    () => this.produits().filter(p => p.stockDisponible >= p.quantiteDue).length,
  );

  private readonly commandeService = inject(CommandeService);

  ngOnInit(): void {
    this.charger();
  }

  protected charger(): void {
    this.isLoading.set(true);
    this.commandeService.lireProduitsDusNonCommandes().subscribe({
      next: produits => {
        this.produits.set(produits);
        this.countChange.emit(produits.length);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  /** Jours d'attente du plus ancien client. */
  protected joursDAttente(produit: IProduitDuNonCommande): number {
    const depuis = new Date(produit.attenteDepuis).getTime();
    return Math.max(0, Math.floor((Date.now() - depuis) / 86400000));
  }

  /**
   * Nombre de personnes en attente. Les deux chiffres du serveur ne mesurent pas la même chose :
   * un avoir au porteur n'a pas de client identifié, et un même client peut avoir demandé deux
   * fois. On annonce le plus grand, qui est le nombre de promesses à tenir.
   */
  protected enAttente(produit: IProduitDuNonCommande): number {
    return Math.max(produit.nbClients, produit.nbDemandes);
  }

  /** Un dû que le stock couvre déjà n'appelle pas une commande mais un appel au client. */
  protected servableMaintenant(produit: IProduitDuNonCommande): boolean {
    return produit.stockDisponible >= produit.quantiteDue;
  }

  protected severiteAttente(produit: IProduitDuNonCommande): "danger" | "warn" | "secondary" {
    const jours = this.joursDAttente(produit);
    if (jours >= 14) {
      return "danger";
    }
    return jours >= 7 ? "warn" : "secondary";
  }
}
