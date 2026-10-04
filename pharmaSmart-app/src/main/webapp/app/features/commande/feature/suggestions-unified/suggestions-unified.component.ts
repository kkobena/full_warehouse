import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from "@angular/core";
import {forkJoin} from "rxjs";
import {CommonModule} from "@angular/common";
import {NgbModal, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {AppSubtab, BadgeComponent, ButtonComponent, SubtabBarComponent} from "app/shared/ui";
import {SuggestionHomeComponent} from "../suggestion/suggestion-home.component";
import {SemoisSuggestionsComponent} from "../semois-suggestions/semois-suggestions.component";
import {SemoisClasseConfigComponent} from "../semois-classe-config/semois-classe-config.component";
import {
  CommandeRequestedHomeComponent
} from "../commande-requested-home/commande-requested-home.component";
import {AppListBonsComponent} from "../../ui/list-bons/list-bons.component";
import {
  CommandCommonService,
  SuggestionsSource
} from "app/entities/commande/command-common.service";
import {
  SemoisFraicheur,
  SuggestionService
} from "app/entities/commande/suggestion/suggestion.service";
import {DeliveryService} from "app/entities/commande/delevery/delivery.service";

@Component({
  selector: "app-suggestions-unified",
  templateUrl: "./suggestions-unified.component.html",
  styleUrls: ["./suggestions-unified.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ButtonComponent,
    BadgeComponent,
    SubtabBarComponent,
    NgbTooltip,
    SuggestionHomeComponent,
    SemoisSuggestionsComponent,
    CommandeRequestedHomeComponent,
    AppListBonsComponent
  ]
})
export class SuggestionsUnifiedComponent implements OnInit {
  readonly semoisFraicheur = signal<SemoisFraicheur | null>(null);
  /** Badge : nb suggestions GENEREE + VALIDEE (tab Réapprovisionnement) */
  readonly countReappro = signal<number>(0);
  /** Badge : nb commandes REQUESTED (tab Commandes à passer, toujours affiché) */
  readonly countCommandesAPasser = signal<number>(0);
  /** Badge : nb bons RECEIVED en attente de saisie */
  readonly countBonsLivraison = signal<number>(0);
  private readonly commandCommonService = inject(CommandCommonService);
  /**
   * Source active — computed dérivé directement du signal service.
   * Toujours en phase avec le contenu (@if) et le style de l'onglet, qu'on navigue
   * manuellement (clic) ou programmatiquement (facade après validation).
   */
  readonly activeSource = computed(() => this.commandCommonService.suggestionsActiveSource());

  /** Onglets des quatre sources ; la pastille des propositions et des réceptions n'apparaît que s'il y a quelque chose à traiter. */
  protected readonly onglets = computed<AppSubtab[]>(() => [
    {
      id: 'REAPPRO',
      label: 'Propositions d\'achat',
      icon: 'pi pi-inbox',
      tooltip: 'Propositions d\'achat générées automatiquement selon la VMM — en attente de validation',
      badge: this.countReappro() > 0 ? this.countReappro() : null,
      badgeSeverity: 'primary',
    },
    {
      id: 'COMMANDES_A_PASSER',
      label: 'Commandes fournisseurs',
      icon: 'pi pi-cart-plus',
      tooltip: 'Bons de commande en cours de préparation, prêts à être transmis aux fournisseurs',
      badge: this.countCommandesAPasser(),
      badgeSeverity: 'success',
    },
    {
      id: 'BONS_DE_LIVRAISON',
      label: 'Réceptions',
      icon: 'pi pi-truck',
      tooltip: 'Bons de livraison reçus des fournisseurs, en attente de réception en stock',
      badge: this.countBonsLivraison() > 0 ? this.countBonsLivraison() : null,
      badgeSeverity: 'warn',
    },
    {
      id: 'ANALYSE',
      label: 'Tableau de bord stock',
      icon: 'pi pi-chart-bar',
      tooltip: 'Tableau de bord : niveaux de stock par classe A/B/C/D et seuils de rupture',
    },
  ]);
  private readonly suggestionService = inject(SuggestionService);
  private readonly deliveryService = inject(DeliveryService);
  private readonly modalService = inject(NgbModal);

  get semoisFraicheurLabel(): string {
    const f = this.semoisFraicheur();
    if (!f) {
      return "";
    }
    if (f.calculeRecent) {
      return "VMM · À jour";
    }
    if (f.dernierCalcul) {
      const d = new Date(f.dernierCalcul);
      return `VMM · ${d.toLocaleDateString("fr-FR")}`;
    }
    return "VMM · Non initialisée";
  }

  get semoisFraicheurSeverity(): "success" | "warn" | "danger" | "secondary" {
    const f = this.semoisFraicheur();
    if (!f || !f.dernierCalcul) {
      return "danger";
    }
    return f.calculeRecent ? "success" : "warn";
  }

  ngOnInit(): void {
    this.loadBadges();
    this.suggestionService.getSemoisFraicheur().subscribe({
      next: f => this.semoisFraicheur.set(f),
      error: () => this.semoisFraicheur.set(null)
    });
  }

  /** Recharge les compteurs de badges pour tous les onglets. */
  loadBadges(): void {
    forkJoin({
      generee: this.suggestionService.countByStatut("GENEREE"),
      validee: this.suggestionService.countByStatut("VALIDEE"),
      requested: this.deliveryService.countByStatut("REQUESTED"),
      received: this.deliveryService.countByStatut("RECEIVED")
    }).subscribe({
      next: ({generee, validee, requested, received}) => {
        this.countReappro.set(generee + validee);
        this.countCommandesAPasser.set(requested);
        this.countBonsLivraison.set(received);
      },
      error: () => this.countReappro.set(0)
    });
  }

  setSource(source: SuggestionsSource): void {
    this.commandCommonService.suggestionsActiveSource.set(source);
    this.loadBadges();
  }

  ouvrirConfigCriticite(): void {
    this.modalService.open(SemoisClasseConfigComponent, {
      size: "xl",
      scrollable: true,
      centered: true,
      backdrop: "static"
    });
  }
}

