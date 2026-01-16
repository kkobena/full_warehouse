import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Button } from 'primeng/button';
import { KeyboardShortcut } from '../../entities/sales/selling-home/racourci/keyboard-shortcuts.service';
import { SellingHomeShortcutsService } from '../../entities/sales/selling-home/racourci/selling-home-shortcuts.service';

interface ShortcutDisplay {
  keys: string[];
  description: string;
  badge?: string;
}

interface CategoryDisplay {
  title: string;
  icon: string;
  shortcuts: ShortcutDisplay[];
  order: number; // For sorting categories
}

@Component({
  selector: 'jhi-shortcuts-help-dialog',
  imports: [CommonModule, Button],
  styleUrls: ['../../entities/common-modal.component.scss'],
  template: `
    <div class="modal-header">
      <h4 class="modal-title">
        <i class="bi bi-keyboard"></i> Raccourcis Clavier - PharmaSmart
        @if (isRunningInTauri) {
          <span class="badge bg-success ms-2">Mode Desktop</span>
        } @else {
          <span class="badge bg-info ms-2">Mode Web</span>
        }
      </h4>
      <button type="button" class="btn-close" aria-label="Close" (click)="dismiss()"></button>
    </div>

    <div class="modal-body">
      <!-- Environment Info -->
      <div [class]="isRunningInTauri ? 'alert alert-success mb-4' : 'alert alert-info mb-4'">
        <i class="bi bi-info-circle"></i>
        @if (isRunningInTauri) {
          <strong>Mode Application :</strong> Vous avez accès à tous les raccourcis, y compris les raccourcis Ctrl avancés qui ne
          sont pas disponibles dans le navigateur.
        } @else {
          <strong>Mode Navigateur Web :</strong> Les raccourcis Ctrl (comme Ctrl+S, Ctrl+P) sont réservés par le navigateur. Utilisez
          les touches F1-F11 et Alt+Lettre à la place.
        }
      </div>

      <!-- Quick Start Guide -->
      <div class="alert alert-light border mb-4">
        <h6 class="mb-2"><i class="bi bi-lightbulb-fill text-warning"></i> Démarrage Rapide</h6>
        <div class="quick-start-flow">
          <kbd>F2</kbd>
          <span class="flow-arrow">→</span>
          <span class="flow-text">Rechercher produit</span>
          <span class="flow-arrow">→</span>
          <kbd>F3</kbd>
          <span class="flow-arrow">→</span>
          <span class="flow-text">Quantité</span>
          <span class="flow-arrow">→</span>
          <kbd>F5</kbd>
          <span class="flow-arrow">→</span>
          <span class="flow-text">Ajouter</span>
          <span class="flow-arrow">→</span>
          <kbd>F9</kbd>
          <span class="flow-arrow">→</span>
          <span class="flow-text">Finaliser</span>
        </div>
      </div>

      <!-- Shortcuts Grid -->
      <div class="shortcuts-grid">
        @for (category of sortedCategories; track category.title) {
          <div class="shortcut-category mb-4" [class.desktop-only]="category.title.includes('Desktop')">
            <h5 class="category-title">
              <i class="bi {{ category.icon }}"></i>
              {{ category.title }}
              @if (category.title.includes('Desktop')) {
                <span class="badge bg-success badge-sm ms-2">Application</span>
              }
              @if (category.title.includes('Web')) {
                <span class="badge bg-info badge-sm ms-2">Web</span>
              }
            </h5>
            <div class="shortcuts-list">
              @for (shortcut of category.shortcuts; track shortcut.keys) {
                <div class="shortcut-item">
                  <div class="shortcut-keys">
                    @for (key of shortcut.keys; track key; let isLast = $last) {
                      <kbd [class.modifier-key]="isModifierKey(key)">{{ key }}</kbd>
                      @if (!isLast) {
                        <span class="key-separator">+</span>
                      }
                    }
                    @if (shortcut.badge) {
                      <span
                        [class]="
                          'badge ms-2 ' +
                          (shortcut.badge === 'Essentiel' || shortcut.badge === 'Important'
                            ? 'bg-danger'
                            : shortcut.badge === 'Desktop'
                              ? 'bg-success'
                              : shortcut.badge === 'Web'
                                ? 'bg-info'
                                : shortcut.badge === 'Admin'
                                  ? 'bg-warning'
                                  : 'bg-secondary')
                        "
                        >{{ shortcut.badge }}</span
                      >
                    }
                  </div>
                  <div class="shortcut-description">{{ shortcut.description }}</div>
                </div>
              }
            </div>
          </div>
        }
      </div>

      <!-- Tips Section -->
      <div class="tips-section mt-4 p-3 bg-light rounded">
        <h6 class="mb-3"><i class="bi bi-star-fill text-warning"></i> Conseils d'Utilisation</h6>
        <ul class="small mb-0">
          @if (isRunningInTauri) {
            <li><strong>Mode Expert :</strong> Les raccourcis Ctrl permettent un flux de travail ultra-rapide</li>
            <li><strong>Ctrl+K :</strong> Recherche omnidirectionnelle pour trouver n'importe quoi rapidement</li>
            <li><strong>Ctrl+Enter :</strong> Finalisation express sans quitter le clavier</li>
            <li><strong>Productivité :</strong> Mémorisez Ctrl+F (produit), Ctrl+E (client), Ctrl+S (attente)</li>
          } @else {
            <li><strong>Débutants :</strong> Commencez par F2 (produit) → F3 (quantité) → F9 (finaliser)</li>
            <li><strong>Navigation :</strong> Les touches Alt+Lettre sont sûres et ne conflictent pas avec le navigateur</li>
            <li><strong>Touches F :</strong> F1-F11 fonctionnent même dans les champs de saisie</li>
          }
          <li><strong>Annulation :</strong> La touche Échap annule toujours l'action en cours</li>
          <li><strong>Aide :</strong> Appuyez sur F1 n'importe quand pour réafficher cette aide</li>
        </ul>
      </div>

      <!-- Legend -->
      <div class="legend-section mt-3 p-3 border rounded">
        <h6 class="mb-2"><i class="bi bi-info-square"></i> Légende</h6>
        <div class="d-flex flex-wrap gap-3">
          <span><span class="badge bg-danger">Essentiel</span> = Raccourci indispensable</span>
          <span><span class="badge bg-danger">Important</span> = Très utilisé</span>
          @if (isRunningInTauri) {
            <span><span class="badge bg-success">Desktop</span> = Exclusif application</span>
            <span><span class="badge bg-warning">Admin</span> = Droits requis</span>
          } @else {
            <span><span class="badge bg-info">Web</span> = Optimisé navigateur</span>
          }
        </div>
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
        max-height: 75vh;
        overflow-y: auto;
      }

      .quick-start-flow {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-wrap: wrap;
        gap: 0.5rem;
        padding: 1rem;
        background: white;
        border-radius: 6px;
      }

      .flow-arrow {
        color: #3498db;
        font-weight: bold;
        font-size: 1.2rem;
      }

      .flow-text {
        font-size: 0.9rem;
        color: #6c757d;
        font-weight: 500;
      }

      .shortcuts-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
        gap: 1.5rem;
      }

      .shortcut-category {
        border: 1px solid #dee2e6;
        border-radius: 8px;
        padding: 1rem;
        background: #ffffff;
        transition: all 0.3s ease;
      }

      .shortcut-category:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        transform: translateY(-2px);
      }

      .shortcut-category.desktop-only {
        border-left: 4px solid #28a745;
      }

      .category-title {
        font-size: 1rem;
        font-weight: 600;
        color: #2c3e50;
        margin-bottom: 1rem;
        padding-bottom: 0.5rem;
        border-bottom: 2px solid #3498db;
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .category-title i {
        color: #3498db;
      }

      .category-title .badge-sm {
        font-size: 0.7rem;
        padding: 0.2rem 0.4rem;
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
        padding: 0.6rem;
        border-radius: 4px;
        transition: background-color 0.2s;
        gap: 1rem;
      }

      .shortcut-item:hover {
        background-color: #f8f9fa;
      }

      .shortcut-keys {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        min-width: 140px;
        flex-shrink: 0;
      }

      kbd {
        display: inline-block;
        padding: 0.3rem 0.6rem;
        font-size: 0.85rem;
        font-weight: 600;
        line-height: 1;
        color: #495057;
        background-color: #f8f9fa;
        border: 1px solid #ced4da;
        border-radius: 4px;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        font-family: 'Courier New', monospace;
        transition: all 0.2s;
      }

      kbd:hover {
        background-color: #e9ecef;
        transform: scale(1.05);
      }

      kbd.modifier-key {
        background-color: #e9ecef;
        color: #495057;
        border-color: #adb5bd;
        font-weight: 700;
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
        background: #d4edda !important;
      }

      .tips-section h6 {
        color: #155724;
        font-weight: 600;
      }

      .tips-section ul {
        list-style: none;
        padding-left: 0;
      }

      .tips-section li {
        padding: 0.4rem 0;
        color: #155724;
      }

      .tips-section li::before {
        content: '✓ ';
        color: #28a745;
        font-weight: bold;
        margin-right: 0.5rem;
      }

      .legend-section {
        background: #f8f9fa;
      }

      .legend-section h6 {
        color: #495057;
        font-weight: 600;
      }

      .alert {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .bi {
        font-size: 1rem;
      }

      @media (max-width: 768px) {
        .shortcuts-grid {
          grid-template-columns: 1fr;
        }

        .shortcut-item {
          flex-direction: column;
          align-items: flex-start;
        }

        .shortcut-description {
          text-align: left;
          margin-top: 0.5rem;
        }

        .quick-start-flow {
          font-size: 0.85rem;
        }
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

        .shortcut-category:hover {
          box-shadow: none;
          transform: none;
        }
      }
    `,
  ],
})
export class ShortcutsHelpDialogComponent implements OnInit {
  public activeModal = inject(NgbActiveModal);
  public shortcutsService?: SellingHomeShortcutsService;

  sortedCategories: CategoryDisplay[] = [];
  isRunningInTauri = false;

  ngOnInit(): void {
    if (this.shortcutsService) {
      this.isRunningInTauri = this.shortcutsService.isRunningInTauri();
      this.loadDynamicShortcuts();
    }
  }

  private loadDynamicShortcuts(): void {
    const shortcutsByCategory = this.shortcutsService?.getShortcutsByCategory();
    const categories: CategoryDisplay[] = [];

    if (!shortcutsByCategory) {
      return;
    }

    shortcutsByCategory.forEach((shortcuts, categoryName) => {
      const categoryDisplay: CategoryDisplay = {
        title: categoryName,
        icon: this.getCategoryIcon(categoryName),
        shortcuts: shortcuts.map(s => this.convertToDisplay(s)),
        order: this.getCategoryOrder(categoryName),
      };
      categories.push(categoryDisplay);
    });

    // Sort categories by order
    this.sortedCategories = categories.sort((a, b) => a.order - b.order);
  }

  private convertToDisplay(shortcut: KeyboardShortcut): ShortcutDisplay {
    const keys: string[] = [];

    // Build key combination
    if (shortcut.ctrl) keys.push('Ctrl');
    if (shortcut.alt) keys.push('Alt');
    if (shortcut.shift) keys.push('Shift');
    if (shortcut.meta) keys.push('Cmd');

    // Format the main key
    const mainKey = this.formatKey(shortcut.key);
    keys.push(mainKey);

    return {
      keys,
      description: shortcut.description,
      badge: shortcut.badge,
    };
  }

  private formatKey(key: string): string {
    // Special key formatting
    const keyMap: Record<string, string> = {
      arrowup: '↑',
      arrowdown: '↓',
      arrowleft: '←',
      arrowright: '→',
      escape: 'Échap',
      delete: 'Suppr',
      backspace: '⌫',
      enter: 'Entrée',
      ' ': 'Espace',
    };

    const lowerKey = key.toLowerCase();
    if (keyMap[lowerKey]) {
      return keyMap[lowerKey];
    }

    // F-keys stay uppercase
    if (key.match(/^F\d+$/i)) {
      return key.toUpperCase();
    }

    // Single letters stay as-is
    if (key.length === 1) {
      return key.toUpperCase();
    }

    // Everything else capitalize first letter
    return key.charAt(0).toUpperCase() + key.slice(1);
  }

  private getCategoryIcon(categoryName: string): string {
    const iconMap: Record<string, string> = {
      Aide: 'bi-question-circle',
      Navigation: 'bi-compass',
      'Navigation Rapide': 'bi-cursor',
      'Actions Produit': 'bi-box-seam',
      'Types de Vente': 'bi-cart-check',
      Finalisation: 'bi-check-circle',
      'Gestion Quantité': 'bi-plus-minus',
      Remises: 'bi-percent',
      Impression: 'bi-printer',
      '⚡ Desktop': 'bi-lightning-charge',
      '🌐 Web': 'bi-globe',
    };

    return iconMap[categoryName] || 'bi-keyboard';
  }

  private getCategoryOrder(categoryName: string): number {
    const orderMap: Record<string, number> = {
      Aide: 1,
      Navigation: 2,
      'Actions Produit': 3,
      'Types de Vente': 4,
      Finalisation: 5,
      'Gestion Quantité': 6,
      Remises: 7,
      'Navigation Rapide': 8,
      Impression: 9,
      '⚡ Desktop': 10,
      '🌐 Web': 11,
    };

    return orderMap[categoryName] || 99;
  }

  isModifierKey(key: string): boolean {
    return ['Ctrl', 'Alt', 'Shift', 'Cmd'].includes(key);
  }

  printShortcuts(): void {
    window.print();
  }

  dismiss(): void {
    this.activeModal.dismiss('cancel');
  }
}
