import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { Button } from "primeng/button";

@Component({
  selector: "app-reception-help",
  template: `
    <div class="modal-header">
      <h5 class="modal-title"><i class="pi pi-question-circle me-2"></i>Aide à la réception</h5>
      <button type="button" class="btn-close" (click)="activeModal.dismiss()"></button>
    </div>
    <div class="modal-body rh-body">

      <!-- Saisie grille -->
      <section class="rh-section">
        <h6 class="rh-section__title"><i class="pi pi-table me-2"></i>Saisie dans la grille</h6>
        <table class="rh-table">
          <tbody>
          <tr>
            <td class="rh-key"><kbd>Tab</kbd> / <kbd>Shift+Tab</kbd></td>
            <td>Naviguer entre les cellules éditables</td>
          </tr>
          <tr>
            <td class="rh-key"><kbd>Entrée</kbd></td>
            <td>Valider et passer à la ligne suivante</td>
          </tr>
          <tr>
            <td class="rh-key"><kbd>Retour Arr.</kbd></td>
            <td>Activer l'édition en effaçant la valeur</td>
          </tr>
          <tr>
            <td class="rh-key"><kbd>Échap</kbd></td>
            <td>Annuler l'édition en cours</td>
          </tr>
          </tbody>
        </table>
      </section>

      <!-- Gestion des lots -->
      <section class="rh-section">
        <h6 class="rh-section__title"><i class="pi pi-box me-2"></i>Gestion des lots</h6>
        <table class="rh-table">
          <tbody>
          <tr>
            <td class="rh-key"><kbd>Espace</kbd></td>
            <td>Ouvrir / fermer le formulaire de lot (colonne "Lots")</td>
          </tr>
          <tr>
            <td class="rh-key">Automatique</td>
            <td>Le formulaire s'ouvre dès que vous saisissez une quantité reçue (si gestion de lots activée)</td>
          </tr>
          <tr>
            <td class="rh-key"><kbd>Tab</kbd></td>
            <td>Naviguer entre les champs lot (N° lot → Qté → UG → Date exp. → Ajouter)</td>
          </tr>
          <tr>
            <td class="rh-key"><kbd>Entrée</kbd></td>
            <td>Ajouter le lot depuis le formulaire</td>
          </tr>
          <tr>
            <td class="rh-key"><kbd>Échap</kbd></td>
            <td>Annuler la modification d'un lot existant</td>
          </tr>
          </tbody>
        </table>

      </section>

      <!-- Scanner CIP -->
      <section class="rh-section">
        <h6 class="rh-section__title"><i class="pi pi-barcode me-2"></i>Scanner CIP — Code-barres 1D</h6>
        <p>
          Scannez le code-barres CIP (EAN-13 / Code 128) pour incrémenter automatiquement la quantité reçue de 1 pour le
          produit correspondant.
        </p>
        <ul class="rh-list">
          <li>Compatible avec tous les lecteurs codes-barres série / USB HID</li>
          <li>La douchette est reconnue automatiquement — aucune configuration requise</li>
          <li>Si le produit est sous gestion de lot, un lot est créé automatiquement</li>
        </ul>
      </section>

      <!-- Scanner DataMatrix -->
      <section class="rh-section rh-section--highlight">
        <h6 class="rh-section__title">
          <i class="pi pi-qrcode me-2"></i>Scanner DataMatrix GS1 — 2D
          <span class="rh-badge">Recommandé</span>
        </h6>
        <div class="rh-fmd-box">
          <div class="rh-fmd-title"><i class="pi pi-shield me-1"></i>Avantages FMD (Falsified Medicines Directive)</div>
          <ul class="rh-list mb-0">
            <li><strong>Saisie de lot entièrement automatique</strong> : N° lot + date expiration en un seul scan —
              aucune frappe clavier
            </li>
            <li><strong>Traçabilité complète</strong> : numéro de série unique enregistré par boîte, conforme à la
              directive européenne
            </li>
            <li><strong>Détection des doublons</strong> : si un numéro de série a déjà été réceptionné, une alerte
              "DOUBLON" est affichée immédiatement
            </li>
          </ul>
        </div>
        <p class="rh-note mt-2">
          <i class="pi pi-lightbulb me-1"></i>
          Pour les produits sans DataMatrix (CIP 1D uniquement), la saisie manuelle du lot reste disponible.
        </p>
      </section>

    </div>
    <div class="modal-footer">
      <p-button type="button"  severity="secondary" [raised]="true"  label="Fermer"  icon="pi pi-times" (click)="activeModal.dismiss()"/>
    </div>
  `,
  imports: [
    Button
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["reception-help.scss"]
})
export class ReceptionHelpComponent {
  protected readonly activeModal = inject(NgbActiveModal);
}
