import { Component, computed, input } from '@angular/core';
import { EtatProduit } from '../model/etat-produit.model';
import { Tooltip } from 'primeng/tooltip';

interface EtatBadge {
  icon: string;
  label: string;
  tooltip: string;
  cssClass: string;
}

@Component({
  selector: 'jhi-eta-produit',
  imports: [Tooltip],
  template: `
    <div class="etat-produit-bar">
      @for (badge of badges(); track badge.label) {
        <span [class]="'etat-badge ' + badge.cssClass" [pTooltip]="badge.tooltip" tooltipPosition="top">
          <i [class]="badge.icon"></i>
          @if (showLabel()) {
            <span class="etat-badge-label">{{ badge.label }}</span>
          }
        </span>
      }
    </div>
  `,
  styles: `
    .etat-produit-bar {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      flex-wrap: nowrap;
    }

    .etat-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 3px;
      font-size: 0.7rem;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 10px;
      line-height: 1;
      white-space: nowrap;
      cursor: default;
      transition: opacity 0.2s ease;
    }

    .etat-badge:hover {
      opacity: 0.85;
    }

    .etat-badge i {
      font-size: 0.65rem;
    }

    .etat-badge-label {
      font-size: 0.65rem;
    }

    /* ── Stock ── */
    .etat-stock-positif {
      background-color: #dcfce7;
      color: #166534;
      border: 1px solid #bbf7d0;
    }

    .etat-stock-zero {
      background-color: #fef9c3;
      color: #854d0e;
      border: 1px solid #fde68a;
    }

    .etat-stock-negatif {
      background-color: #fee2e2;
      color: #991b1b;
      border: 1px solid #fecaca;
    }

    /* ── Workflow ── */
    .etat-suggestion {
      background-color: #dbeafe;
      color: #1e40af;
      border: 1px solid #bfdbfe;
    }

    .etat-commande {
      background-color: #f3e8ff;
      color: #6b21a8;
      border: 1px solid #e9d5ff;
    }

    .etat-entree {
      background-color: #ede9fe;
      color: #5b21b6;
      border: 1px solid #ddd6fe;
    }
  `,
})
export class EtaProduitComponent {
  readonly etatProduit = input.required<EtatProduit>();
  readonly showLabel = input<boolean>(true);
  readonly isSuggestion = input<boolean>(false);
  readonly isCommande = input<boolean>(false);

  protected readonly badges = computed<EtatBadge[]>(() => {
    const etat = this.etatProduit();
    if (!etat) {
      return [];
    }
    const result: EtatBadge[] = [];

    // ── Stock (mutuellement exclusif) ──
    if (etat.stockPositif) {
      result.push({
        icon: 'pi pi-check-circle',
        label: 'En stock',
        tooltip: 'Produit disponible en stock',
        cssClass: 'etat-stock-positif',
      });
    } else if (etat.sockZero) {
      result.push({
        icon: 'pi pi-minus-circle',
        label: 'Rupture',
        tooltip: 'Stock épuisé',
        cssClass: 'etat-stock-negatif',
      });
    } else if (etat.stockNegatif) {
      result.push({
        icon: 'pi pi-exclamation-circle',
        label: 'Stock négatif',
        tooltip: 'Quantité en stock inférieure à zéro',
        cssClass: 'etat-stock-negatif',
      });
    }

    // ── Suggestion ──
    const showSuggestion = this.isSuggestion() ? etat.otherSuggestion : etat.enSuggestion;
    if (showSuggestion) {
      result.push({
        icon: 'pi pi-lightbulb',
        label: this.isSuggestion() ? 'Autre sugg.' : 'En sugg.',
        tooltip: this.isSuggestion() ? 'Déjà présent dans une autre suggestion' : 'Produit inscrit dans une suggestion',
        cssClass: 'etat-suggestion',
      });
    }

    // ── Commande ──
    if (!this.isCommande() && etat.enCommande) {
      result.push({
        icon: 'pi pi-shopping-cart',
        label: 'En commande',
        tooltip: 'Une commande en cours contient ce produit',
        cssClass: 'etat-commande',
      });
    }

    // ── Entrée ──
    if (etat.entree) {
      result.push({
        icon: 'pi pi-download',
        label: 'Reçu',
        tooltip: 'Produit réceptionné récemment',
        cssClass: 'etat-entree',
      });
    }

    return result;
  });
}
