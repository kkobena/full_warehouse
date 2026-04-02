import { AfterViewInit, Component, DestroyRef, inject, OnInit, signal, viewChild } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { SelectModule } from "primeng/select";
import { InputNumber, InputNumberModule } from "primeng/inputnumber";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { IProduit } from "app/shared/model/produit.model";
import { IFournisseurProduit } from "app/shared/model/fournisseur-produit.model";
import { CommandeService } from "app/entities/commande/commande.service";
import { SuggestionService } from "app/entities/commande/suggestion/suggestion.service";
import { NotificationService } from "app/shared/services/notification.service";
import { CommandCommonService } from "app/entities/commande/command-common.service";

@Component({
  selector: "app-commande-rapide-modal",
  templateUrl: "./commande-rapide-modal.component.html",
  styleUrls: ["./commande-rapide-modal.component.scss"],
  imports: [CommonModule, FormsModule, ButtonModule, SelectModule, InputNumberModule]
})
export class CommandeRapideModalComponent implements OnInit, AfterViewInit {
  produit!: IProduit;

  protected fournisseurs = signal<IFournisseurProduit[]>([]);
  protected loading = signal(false);
  protected submitting = signal(false);
  protected selectedFournisseurProduit: IFournisseurProduit | null = null;
  protected quantite = 1;

  private readonly activeModal = inject(NgbActiveModal);
  private readonly commandeService = inject(CommandeService);
  private readonly suggestionService = inject(SuggestionService);
  private readonly notificationService = inject(NotificationService);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly quantiteInput = viewChild.required<InputNumber>("quantiteInput");

  ngOnInit(): void {
    this.loading.set(true);
    this.suggestionService.getFournisseursProduit(this.produit.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => {
          this.fournisseurs.set(list);
          const principal = list.find(f => f.principal) ?? list[0] ?? null;
          this.selectedFournisseurProduit = principal;
          this.quantite = principal?.qteMinimaleCommande ?? 1;
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      const el = this.quantiteInput()?.input;
      if (el) {
        el.nativeElement.focus();
        //el.nativeElement.value=String(1);
        el.nativeElement.select();
      }

    }, 150);
  }

  protected onFournisseurChange(): void {
    const fp = this.selectedFournisseurProduit;
    if (fp?.qteMinimaleCommande) {
      this.quantite = fp.qteMinimaleCommande;
    }
  }

  protected get totalEstime(): number {
    const fp = this.selectedFournisseurProduit;
    if (!fp?.prixAchat) return 0;
    return this.quantite * fp.prixAchat;
  }

  protected onSubmit(): void {
    const fp = this.selectedFournisseurProduit;
    if (!fp || this.quantite < 1) return;
    this.submitting.set(true);

    this.commandeService.createCommandeRapide(this.produit.totalQuantity, fp.id!, this.quantite)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          const commande = res.body;
          if (commande?.commandeId) {
            this.commandCommonService.pendingOpenCommandeId.set(commande.commandeId);
          }
          this.commandCommonService.navigateToCommandesAPasser();
          this.activeModal.close(true);
          this.router.navigate(["/commande"]);
        },
        error: () => {
          this.notificationService.error("Erreur lors de la création de la commande");
          this.submitting.set(false);
        }
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
