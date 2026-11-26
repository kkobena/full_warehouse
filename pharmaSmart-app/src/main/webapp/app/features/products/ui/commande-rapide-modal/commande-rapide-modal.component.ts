import {
  afterRenderEffect,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  OnInit,
  signal,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ButtonComponent, CardComponent, InputNumberComponent, SelectComponent } from 'app/shared/ui';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { IProduit } from 'app/shared/model/produit.model';
import { IFournisseurProduit } from 'app/shared/model/fournisseur-produit.model';
import { CommandeService } from 'app/entities/commande/commande.service';
import { SuggestionService } from 'app/entities/commande/suggestion/suggestion.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { CommandCommonService } from 'app/entities/commande/command-common.service';
import { DevisePipe } from 'app/shared/utils/devise';

@Component({
  selector: 'app-commande-rapide-modal',
  templateUrl: './commande-rapide-modal.component.html',
  styleUrls: ['./commande-rapide-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent, SelectComponent, InputNumberComponent, CardComponent, DevisePipe],
})
export class CommandeRapideModalComponent implements OnInit {
  produit!: IProduit;

  protected fournisseurs = signal<IFournisseurProduit[]>([]);
  protected loading = signal(false);
  protected submitting = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    fournisseurProduit: [null as IFournisseurProduit | null],
    quantite: [1 as number | null],
  });

  protected readonly selectedFournisseurProduit = toSignal(this.form.controls.fournisseurProduit.valueChanges, {
    initialValue: this.form.controls.fournisseurProduit.value,
  });
  /** Un champ vidé vaut 0 : la commande reste alors refusée. */
  protected readonly quantite = computed(() => this.quantiteSaisie() ?? 0);

  private readonly quantiteSaisie = toSignal(this.form.controls.quantite.valueChanges, {
    initialValue: this.form.controls.quantite.value,
  });
  private readonly activeModal = inject(NgbActiveModal);
  private readonly commandeService = inject(CommandeService);
  private readonly suggestionService = inject(SuggestionService);
  private readonly notificationService = inject(NotificationService);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  // Le champ n'existe qu'une fois les fournisseurs chargés : la requête ne peut pas être `required`.
  // Elle porte sur <app-input-number> : sans `read: ElementRef`, elle rendrait l'instance du composant.
  private readonly quantiteInput = viewChild<unknown, ElementRef<HTMLElement>>('quantiteInput', { read: ElementRef });
  private focusPose = false;

  constructor() {
    // Le champ apparaît après le chargement : le focus l'attend, puis n'y revient plus.
    afterRenderEffect(() => {
      const el = this.quantiteInput()?.nativeElement.querySelector('input');
      if (!el || this.focusPose) return;
      this.focusPose = true;
      el.focus();
      el.select();
    });
  }

  ngOnInit(): void {
    this.loading.set(true);
    this.suggestionService
      .getFournisseursProduit(this.produit.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => {
          this.fournisseurs.set(list);
          const principal = list.find(f => f.principal) ?? list[0] ?? null;
          this.form.setValue({ fournisseurProduit: principal, quantite: principal?.qteMinimaleCommande ?? 1 });
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  protected onFournisseurChange(): void {
    const fp = this.selectedFournisseurProduit();
    if (fp?.qteMinimaleCommande) {
      this.form.controls.quantite.setValue(fp.qteMinimaleCommande);
    }
  }

  protected get totalEstime(): number {
    const fp = this.selectedFournisseurProduit();
    if (!fp?.prixAchat) return 0;
    return this.quantite() * fp.prixAchat;
  }

  protected onSubmit(): void {
    const fp = this.selectedFournisseurProduit();
    if (!fp || this.quantite() < 1) return;
    this.submitting.set(true);

    this.commandeService
      .createCommandeRapide(this.produit.totalQuantity, fp.id!, this.quantite())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const commande = res.body;
          if (commande?.commandeId) {
            this.commandCommonService.pendingOpenCommandeId.set(commande.commandeId);
          }
          this.commandCommonService.navigateToCommandesAPasser();
          this.activeModal.close(true);
          this.router.navigate(['/commande']);
        },
        error: () => {
          this.notificationService.error('Erreur lors de la création de la commande');
          this.submitting.set(false);
        },
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
