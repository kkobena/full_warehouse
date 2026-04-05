import { Component, inject, OnInit, signal } from "@angular/core";
import { CommonModule, DecimalPipe } from "@angular/common";
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { SelectModule } from "primeng/select";
import { InputNumberModule } from "primeng/inputnumber";
import { TableModule } from "primeng/table";
import { TagModule } from "primeng/tag";

import { LotPerimes } from "../model/lot-perimes";
import {
  RetourBonBatchResult,
  RetourFournisseurBatchRequest,
  RetourFournisseurRequest
} from "../model/retour-fournisseur-request";
import { IMotifRetourProduit } from "../../../shared/model/motif-retour-produit.model";
import { ModifRetourProduitService } from "../../motif-retour-produit/motif-retour-produit.service";
import { RetourBonService } from "../../commande/retour_fournisseur/retour-bon.service";
import { NotificationService } from "../../../shared/services/notification.service";

@Component({
  selector: "jhi-retour-groupe-perime-dialog",
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    SelectModule,
    InputNumberModule,
    TableModule,
    TagModule,
    DecimalPipe
  ],
  templateUrl: "./retour-groupe-perime-dialog.component.html",
  styleUrls: ["./retour-lot-pereme-groupe.scss"]
})
export class RetourGroupePerimeDialogComponent implements OnInit {
  lots: LotPerimes[] = [];

  protected readonly activeModal = inject(NgbActiveModal);
  private readonly fb = inject(FormBuilder);
  private readonly motifService = inject(ModifRetourProduitService);
  private readonly retourBonService = inject(RetourBonService);
  private readonly notificationService = inject(NotificationService);

  protected motifs = signal<IMotifRetourProduit[]>([]);
  protected isSaving = signal(false);
  protected result = signal<RetourBonBatchResult | null>(null);
  protected form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      motifRetourId: [null, Validators.required],
      items: this.fb.array(
        this.lots.map(lot =>
          this.fb.group({
            lotId: [lot.id],
            quantity: [lot.quantity, [Validators.required, Validators.min(1), Validators.max(lot.quantity)]],
            included: [true]
          })
        )
      )
    });
    this.loadMotifs();
  }

  protected get itemsArray(): FormArray {
    return this.form.get("items") as FormArray;
  }

  protected getItemGroup(i: number): FormGroup {
    return this.itemsArray.at(i) as FormGroup;
  }

  protected get selectedCount(): number {
    return this.itemsArray.controls.filter(c => c.get("included")?.value).length;
  }

  protected get totalAvoir(): number {
    return this.itemsArray.controls.reduce((sum, ctrl, i) => {
      if (!ctrl.get("included")?.value) return sum;
      const qty: number = ctrl.get("quantity")?.value ?? 0;
      return sum + qty * (this.lots[i]?.prixAchat ?? 0);
    }, 0);
  }

  protected canSave(): boolean {
    return this.form.valid && this.selectedCount > 0 && !this.isSaving() && !this.result();
  }

  protected save(): void {
    if (!this.canSave()) {
      this.form.markAllAsTouched();
      return;
    }

    const motifRetourId = this.form.get("motifRetourId")!.value;
    const lotsRequests: RetourFournisseurRequest[] = this.itemsArray.controls
      .map((ctrl, i) => {
        if (!ctrl.get("included")?.value) return null;
        return {
          lotId: this.lots[i].id,
          motifRetourId,
          quantity: ctrl.get("quantity")!.value
        } as RetourFournisseurRequest;
      })
      .filter((r): r is RetourFournisseurRequest => r !== null);

    const batchRequest: RetourFournisseurBatchRequest = { lots: lotsRequests };

    this.isSaving.set(true);
    this.retourBonService.createFromExpiredLots(batchRequest).subscribe({
      next: res => {
        this.isSaving.set(false);
        const batchResult = res.body!;
        this.result.set(batchResult);
        if (batchResult.totalErrors === 0) {
          this.notificationService.success(
            `${batchResult.totalCreated} retour(s) fournisseur créé(s) avec succès.`,
            "Succès"
          );
        } else if (batchResult.totalCreated > 0) {
          this.notificationService.warning(
            `${batchResult.totalCreated} retour(s) créé(s), ${batchResult.totalErrors} erreur(s).`,
            "Résultat partiel"
          );
        } else {
          this.notificationService.error(
            "Aucun retour fournisseur n'a pu être créé.",
            "Erreur"
          );
        }
      },
      error: () => {
        this.isSaving.set(false);
        this.notificationService.error("Une erreur est survenue lors du traitement batch.", "Erreur");
      }
    });
  }

  protected close(): void {
    if (this.result() && this.result()!.totalCreated > 0) {
      this.activeModal.close(this.result());
    } else {
      this.activeModal.dismiss();
    }
  }

  private loadMotifs(): void {
    this.motifService.query({ page: 0, size: 999 }).subscribe(res => {
      this.motifs.set(res.body ?? []);
    });
  }
}

