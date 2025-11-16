import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';

interface ShortcutCategory {
  title: string;
  icon: string;
  shortcuts: Shortcut[];
}

interface Shortcut {
  keys: string;
  description: string;
  badge?: string;
}

@Component({
  selector: 'jhi-shortcuts-help-dialog',
  imports: [CommonModule, Button],
  styleUrls: ['../../entities/common-modal.component.scss'],
  template: `
    <div class="modal-header">
      <h4 class="modal-title"><i class="bi bi-keyboard"></i> Raccourcis Clavier - PharmaSmart</h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="dismiss()"></button>
    </div>

    <div class="modal-body">
      <div class="alert alert-info mb-4">
        <i class="bi bi-lightbulb"></i>
        <strong>Astuce :</strong> Appuyez sur <kbd>F1</kbd> n'importe quand pour afficher cette aide.
      </div>

      <div class="shortcuts-grid">
        @for (category of shortcutCategories; track category.title) {
          <div class="shortcut-category mb-4">
            <h5 class="category-title">
              <i class="bi {{ category.icon }}"></i>
              {{ category.title }}
            </h5>
            <div class="shortcuts-list">
              @for (shortcut of category.shortcuts; track shortcut.keys) {
                <div class="shortcut-item">
                  <div class="shortcut-keys">
                    @for (key of parseKeys(shortcut.keys); track key; let isLast = $last) {
                      <kbd>{{ key }}</kbd>
                      @if (!isLast) {
                        <span class="key-separator">+</span>
                      }
                    }
                    @if (shortcut.badge) {
                      <span class="badge bg-success ms-2">{{ shortcut.badge }}</span>
                    }
                  </div>
                  <div class="shortcut-description">{{ shortcut.description }}</div>
                </div>
              }
            </div>
          </div>
        }
      </div>

      <div class="tips-section mt-4 p-3 bg-light rounded">
        <h6 class="mb-3"><i class="bi bi-star"></i> Conseils d'Utilisation</h6>
        <ul class="small mb-0">
          <li><strong>Débutants :</strong> Commencez par F2 (produit) → F3 (quantité) → F9 (finaliser)</li>
          <li><strong>Experts :</strong> Utilisez Alt + touches pour éviter les conflits avec le navigateur</li>
          <li><strong>Navigation :</strong> Les raccourcis F1-F4 fonctionnent même dans les champs de saisie</li>
          <li><strong>Annulation :</strong> La touche Échap annule toujours l'action en cours</li>
        </ul>
      </div>
    </div>

    <div class="modal-footer">
      <p-button
        class="mr-2"
        (click)="printShortcuts()"
        icon="pi pi-print"
        label="Imprimer"
        raised="true"
        severity="secondary"
        type="button"
      ></p-button>
      <p-button (click)="dismiss()" icon="pi pi-times" label="Fermer" raised="true" severity="primary" type="button"></p-button>
    </div>
  `,
  styles: [
    `
      .modal-body {
        max-height: 70vh;
        overflow-y: auto;
      }

      .shortcuts-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 1.5rem;
      }

      .shortcut-category {
        border: 1px solid #dee2e6;
        border-radius: 8px;
        padding: 1rem;
        background: #ffffff;
      }

      .category-title {
        font-size: 1rem;
        font-weight: 600;
        color: #2c3e50;
        margin-bottom: 1rem;
        padding-bottom: 0.5rem;
        border-bottom: 2px solid #3498db;
      }

      .category-title i {
        color: #3498db;
        margin-right: 0.5rem;
      }

      .shortcuts-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .shortcut-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.5rem;
        border-radius: 4px;
        transition: background-color 0.2s;
      }

      .shortcut-item:hover {
        background-color: #f8f9fa;
      }

      .shortcut-keys {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        min-width: 120px;
      }

      kbd {
        display: inline-block;
        padding: 0.25rem 0.5rem;
        font-size: 0.85rem;
        font-weight: 600;
        line-height: 1;
        color: #495057;
        background-color: #f8f9fa;
        border: 1px solid #ced4da;
        border-radius: 4px;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
        font-family: 'Courier New', monospace;
      }

      .key-separator {
        font-weight: bold;
        color: #6c757d;
        padding: 0 0.25rem;
      }

      .shortcut-description {
        flex: 1;
        text-align: right;
        font-size: 0.9rem;
        color: #495057;
      }

      .tips-section {
        border: 1px solid #d4edda;
      }

      .tips-section h6 {
        color: #155724;
        font-weight: 600;
      }

      .tips-section h6 i {
        color: #ffc107;
      }

      .tips-section ul {
        list-style: none;
        padding-left: 0;
      }

      .tips-section li {
        padding: 0.25rem 0;
        color: #495057;
      }

      .tips-section li::before {
        content: '▸ ';
        color: #28a745;
        font-weight: bold;
        margin-right: 0.5rem;
      }

      .alert {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .bi {
        font-size: 1rem;
      }

      @media print {
        .modal-header,
        .modal-footer {
          display: none;
        }

        .modal-body {
          max-height: none;
        }

        .shortcuts-grid {
          grid-template-columns: repeat(2, 1fr);
        }

        .shortcut-item {
          page-break-inside: avoid;
        }
      }
    `,
  ],
})
export class ShortcutsHelpDialogComponent {
  shortcutCategories: ShortcutCategory[] = [
    {
      title: 'Navigation Principale',
      icon: 'bi-compass',
      shortcuts: [
        { keys: 'F1', description: "Afficher l'aide des raccourcis", badge: 'Essentiel' },
        { keys: 'F2', description: 'Rechercher un produit' },
        { keys: 'F3', description: 'Modifier la quantité' },
        { keys: 'F4', description: 'Sélectionner un client' },
        { keys: 'F5', description: 'Ajouter le produit au panier' },
        { keys: 'Échap', description: 'Annuler/Quitter' },
      ],
    },
    {
      title: 'Types de Vente',
      icon: 'bi-cart-check',
      shortcuts: [
        { keys: 'Alt+1', description: 'Vente Comptant' },
        { keys: 'Alt+2', description: 'Vente Assurance' },
        { keys: 'Alt+3', description: 'Vente Carnet' },
      ],
    },
    {
      title: 'Finalisation',
      icon: 'bi-check-circle',
      shortcuts: [
        { keys: 'F9', description: 'Finaliser la vente (paiement)', badge: 'Important' },
        { keys: 'F10', description: 'Mettre en attente' },
        { keys: 'F11', description: 'Voir ventes en attente' },
      ],
    },
    {
      title: 'Gestion Quantité',
      icon: 'bi-plus-minus',
      shortcuts: [
        { keys: 'Alt+↑', description: 'Augmenter de +1' },
        { keys: 'Alt+↓', description: 'Diminuer de -1' },
        { keys: 'Alt+Shift+↑', description: 'Augmenter de +10' },
        { keys: 'Alt+Shift+↓', description: 'Diminuer de -10' },
      ],
    },
    {
      title: 'Actions Produit',
      icon: 'bi-box-seam',
      shortcuts: [
        { keys: 'Suppr', description: 'Retirer ligne sélectionnée' },
        { keys: 'F6', description: 'Effacer sélection produit' },
        { keys: 'F7', description: 'Voir détails stock' },
      ],
    },
    {
      title: 'Remises',
      icon: 'bi-percent',
      shortcuts: [
        { keys: 'Alt+R', description: 'Appliquer remise' },
        { keys: 'Alt+Shift+R', description: 'Retirer remise' },
      ],
    },
    {
      title: 'Navigation Rapide',
      icon: 'bi-cursor',
      shortcuts: [
        { keys: 'Alt+V', description: 'Champ vendeur' },
        { keys: 'Alt+C', description: 'Recherche client' },
        { keys: 'Alt+P', description: 'Recherche produit' },
        { keys: 'Alt+Q', description: 'Champ quantité' },
      ],
    },
    {
      title: 'Impression',
      icon: 'bi-printer',
      shortcuts: [
        { keys: 'Alt+I', description: 'Imprimer facture' },
        { keys: 'Alt+T', description: 'Imprimer ticket' },
      ],
    },
  ];

  constructor(public activeModal: NgbActiveModal) {}

  parseKeys(keys: string): string[] {
    return keys.split('+');
  }

  printShortcuts(): void {
    window.print();
  }

  dismiss(): void {
    this.activeModal.dismiss('cancel');
  }
}
