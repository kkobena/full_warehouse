import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';

import { SemoisService } from './semois.service';
import { ClasseCriticite, getClasseCriticiteInfo } from 'app/shared/model/semois/classe-criticite.model';
import { IAggregationStatus, IInitAllResponse } from 'app/shared/model/semois/semois-configuration.model';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';

interface IPreviewClassificationItem {
  produitId: number;
  libelle: string;
  codeCip: string;
  abcRotation: 'A' | 'B' | 'C';
  rotationRate: number;
  classeSemoisProposee: ClasseCriticite;
  coefficientSecurite: number;
}

@Component({
  selector: 'jhi-semois-config-masse',
  templateUrl: './semois-config-masse.component.html',
  styleUrl: './semois-config-masse.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    ToolbarModule,
    NgbNavModule,
    TableModule,
    Tag,
    InputTextModule,
    WarehouseCommonModule
  ]
})
export default class SemoisConfigMasseComponent implements OnInit {
  activeTab = 'classification-abc'; // Tab actif par défaut
  isProcessing = signal<boolean>(false);
  isPreviewLoading = signal<boolean>(false);
  previewData = signal<IPreviewClassificationItem[] | null>(null);
  aggregationStatus = signal<IAggregationStatus | null>(null);
  initAllResult = signal<IInitAllResponse | null>(null);
  importResult = signal<string | null>(null);

  // Configuration auto
  autoConfig = {
    includeNoSales: false,
    overwriteExisting: false
  };

  importNbMois = 12;

  private readonly semoisService = inject(SemoisService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly applicationConfigService = inject(ApplicationConfigService);

  ngOnInit(): void {
    this.loadAggregationStatus();
  }

  goBack(): void {
    this.router.navigate(['/semois/suggestions']);
  }



  previewAutoClassification(): void {
    this.isPreviewLoading.set(true);

    // Appel à l'endpoint de rotation ABC pour récupérer les données
    const stockRotationUrl = this.applicationConfigService.getEndpointFor('api/reports/stock-rotation');

    this.http.get<any[]>(stockRotationUrl, { observe: 'response' }).subscribe({
      next: (res: HttpResponse<any[]>) => {
        const rotations = res.body ?? [];
        const preview: IPreviewClassificationItem[] = [];

        rotations.forEach(rotation => {
          // Skip products without sales if not included
          if (!this.autoConfig.includeNoSales && (!rotation.qtySoldLast12Months || rotation.qtySoldLast12Months === 0)) {
            return;
          }

          const rotationRate = rotation.rotationRateAnnual ?? 0;
          const abcRotation = rotation.categorieABC ?? 'C';

          // Logique de classification SEMOIS basée sur rotation ABC
          let classeSemois: ClasseCriticite;

          if (rotationRate < 1) {
            classeSemois = ClasseCriticite.D; // Très faible rotation
          } else if (abcRotation === 'A' && rotationRate >= 6) {
            classeSemois = ClasseCriticite.A; // Forte rotation
          } else if (abcRotation === 'B' || (rotationRate >= 3 && rotationRate < 6)) {
            classeSemois = ClasseCriticite.B; // Rotation moyenne
          } else if (abcRotation === 'C' || (rotationRate >= 1 && rotationRate < 3)) {
            classeSemois = ClasseCriticite.C; // Faible rotation
          } else {
            classeSemois = ClasseCriticite.D; // Très faible
          }

          const coefficientSecurite = getClasseCriticiteInfo(classeSemois).coefficientDefaut;

          preview.push({
            produitId: rotation.produitId,
            libelle: rotation.libelle,
            codeCip: rotation.codeCip,
            abcRotation,
            rotationRate,
            classeSemoisProposee: classeSemois,
            coefficientSecurite
          });
        });

        this.previewData.set(preview);
        this.isPreviewLoading.set(false);
      },
      error: () => {
        this.isPreviewLoading.set(false);
        alert('Erreur lors du chargement de la prévisualisation. Vérifiez que l\'analyse ABC de rotation est disponible.');
      }
    });
  }

  confirmAutoClassification(): void {
    if (!this.previewData() || this.previewData()!.length === 0) {
      alert('Aucune donnée à appliquer. Veuillez d\'abord prévisualiser.');
      return;
    }

    if (
      !confirm(
        `Confirmer la création/mise à jour de ${this.previewData()!.length} configurations SEMOIS basées sur l'analyse ABC ?\n\n` +
        `${this.autoConfig.overwriteExisting ? 'ATTENTION : Les configurations existantes seront écrasées.' : 'Les configurations existantes seront préservées.'}`
      )
    ) {
      return;
    }

    this.isProcessing.set(true);

    // Créer les configurations une par une (batch API pourrait être ajouté côté backend)
    const preview = this.previewData()!;
    let processed = 0;
    let errors = 0;

    preview.forEach(item => {
      this.semoisService
        .initializeConfiguration({
          produitId: item.produitId,
          classeCriticite: item.classeSemoisProposee
        })
        .subscribe({
          next: () => {
            processed++;
            if (processed + errors === preview.length) {
              this.isProcessing.set(false);
              alert(`Terminé !\n${processed} configurations créées.\n${errors} erreurs.`);
              this.previewData.set(null);
            }
          },
          error: () => {
            errors++;
            if (processed + errors === preview.length) {
              this.isProcessing.set(false);
              alert(`Terminé !\n${processed} configurations créées.\n${errors} erreurs.`);
              this.previewData.set(null);
            }
          }
        });
    });
  }

  getPreviewCountByClasse(classe: string): number {
    if (!this.previewData()) return 0;
    return this.previewData()!.filter(p => p.classeSemoisProposee === classe).length;
  }


  confirmInitAll(): void {
    if (!confirm('Confirmer l\'initialisation de tous les produits actifs sans configuration SEMOIS ?\n\nClasse par défaut : B (Rotation moyenne)')) {
      return;
    }

    this.isProcessing.set(true);
    this.semoisService.initializeAllConfigurations().subscribe({
      next: (res: HttpResponse<IInitAllResponse>) => {
        this.initAllResult.set(res.body);
        this.isProcessing.set(false);
      },
      error: () => {
        this.isProcessing.set(false);
        alert('Erreur lors de l\'initialisation.');
      }
    });
  }


  confirmImportHistorique(): void {
    if (
      !confirm(
        `Confirmer l'import de ${this.importNbMois} mois de données historiques ?\n\n` +
        `Cette opération peut prendre plusieurs minutes.\n` +
        `Les mois importés seront gelés automatiquement.`
      )
    ) {
      return;
    }

    this.isProcessing.set(true);
    this.semoisService.importHistoricalData({ nbMois: this.importNbMois }).subscribe({
      next: res => {
        this.importResult.set(res.body?.message ?? 'Import réussi');
        this.isProcessing.set(false);
        this.loadAggregationStatus(); // Rafraîchir le statut
      },
      error: () => {
        this.isProcessing.set(false);
        alert('Erreur lors de l\'import historique.');
      }
    });
  }



  loadAggregationStatus(): void {
    this.semoisService.getAggregationStatus().subscribe({
      next: res => {
        this.aggregationStatus.set(res.body);
      },
      error: () => {
        console.error('Erreur lors du chargement du statut d\'agrégation');
      }
    });
  }


  getClasseLabel(classe?: ClasseCriticite): string {
    return getClasseCriticiteInfo(classe).label;
  }

  getClasseSeverity(classe?: ClasseCriticite): 'danger' | 'success' | 'info' | 'warn' | 'secondary' {
    return getClasseCriticiteInfo(classe).severity;
  }
}
