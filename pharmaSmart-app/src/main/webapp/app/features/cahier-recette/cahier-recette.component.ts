import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BadgeComponent, ButtonComponent } from '../../shared/ui';
import { CAHIER_RECETTE, FonctionnaliteRecette, ModuleRecette, ScenarioRecette } from './cahier-recette.model';
import { CahierRecetteService, CaptureServie, CheminsMenu, IndexCaptures } from './cahier-recette.service';
import { BlobDownloadService } from 'app/shared/services/blob-download.service';

interface Selection {
  moduleId: string;
  fonctionnaliteNom: string;
}

/** Retire les fonctionnalités et scénarios marqués `hidden: true` (défaut : visible). */
function withoutHidden(modules: ModuleRecette[]): ModuleRecette[] {
  return modules
    .map(m => ({
      ...m,
      fonctionnalites: m.fonctionnalites
        .filter(f => !f.hidden)
        .map(f => ({ ...f, scenarios: f.scenarios.filter(s => !s.hidden) }))
        .filter(f => f.scenarios.length > 0),
    }))
    .filter(m => m.fonctionnalites.length > 0);
}

@Component({
  selector: 'app-cahier-recette',
  imports: [CommonModule, FormsModule, ButtonComponent, BadgeComponent],
  templateUrl: './cahier-recette.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './cahier-recette.component.scss',
})
export class CahierRecetteComponent {
  private readonly cahierRecetteService = inject(CahierRecetteService);
  private readonly downloadService = inject(BlobDownloadService);

  protected readonly generatingPdf = signal(false);
  protected readonly generatingModuleId = signal<string | null>(null);
  protected readonly currentYear = new Date().getFullYear();
  protected readonly modules = withoutHidden(CAHIER_RECETTE);

  /**
   * Écrans de la dernière campagne de captures, indexés par scénario. Chargés à part du modèle :
   * `CAHIER_RECETTE` est édité à la main, l'index est écrit par la machine (cf.
   * generate-cahier-recette-json.ts). Vide tant qu'aucune campagne n'a tourné — le guide reste
   * alors textuel, sans emplacement d'image vide.
   */
  private readonly captures = signal<IndexCaptures>({});

  /**
   * Chemins de menu servis par le backend, vides tant que la requête n'a pas répondu — la
   * fiche s'affiche alors sans « Où le trouver » plutôt que d'attendre.
   */
  private readonly navPaths = signal<CheminsMenu>({});

  protected readonly searchText = signal('');
  protected readonly selectedFeatures = signal<Set<string>>(new Set());
  protected readonly selectedFeatureCount = computed(() => this.selectedFeatures().size);
  protected readonly expandedModules = signal<Set<string>>(new Set([this.modules[0]?.id].filter(Boolean) as string[]));
  protected readonly selected = signal<Selection | null>(
    this.modules[0] && this.modules[0].fonctionnalites[0]
      ? { moduleId: this.modules[0].id, fonctionnaliteNom: this.modules[0].fonctionnalites[0].nom }
      : null,
  );

  protected readonly filteredModules = computed<ModuleRecette[]>(() => {
    const q = this.searchText().trim().toLowerCase();
    if (!q) return this.modules;

    return this.modules
      .map(m => ({
        ...m,
        fonctionnalites: m.fonctionnalites.filter(f => {
          const haystack = `${m.nom} ${f.nom} ${f.description ?? ''}`.toLowerCase();
          return haystack.includes(q);
        }),
      }))
      .filter(m => m.fonctionnalites.length > 0);
  });

  protected readonly selectedModule = computed<ModuleRecette | null>(() => {
    const sel = this.selected();
    if (!sel) return null;
    return this.modules.find(m => m.id === sel.moduleId) ?? null;
  });

  protected readonly selectedFonctionnalite = computed<FonctionnaliteRecette | null>(() => {
    const sel = this.selected();
    const module = this.selectedModule();
    if (!sel || !module) return null;
    return module.fonctionnalites.find(f => f.nom === sel.fonctionnaliteNom) ?? null;
  });

  constructor() {
    // Une seule requête pour tout le guide : l'index est court, ce sont les images qu'il
    // désigne — chargées à la demande par le navigateur — qui pèsent.
    this.cahierRecetteService
      .loadCaptures()
      .pipe(takeUntilDestroyed())
      .subscribe(index => this.captures.set(index));

    this.cahierRecetteService
      .loadNavPaths()
      .pipe(takeUntilDestroyed())
      .subscribe(chemins => this.navPaths.set(chemins));
  }

  /**
   * Chemin d'accès affiché : le libellé du menu, relu depuis la base à chaque chargement du
   * guide, suivi du trajet interne à l'écran. Retourne `null` quand rien n'est connu — mieux
   * vaut taire l'indication que montrer au pharmacien un code technique introuvable à l'écran.
   */
  protected accesDe(fonctionnalite: FonctionnaliteRecette): string | null {
    const parties: string[] = [];
    const cheminMenu = fonctionnalite.accesCode ? this.navPaths()[fonctionnalite.accesCode] : undefined;

    if (cheminMenu) {
      parties.push(fonctionnalite.accesSuffixe ? `${cheminMenu} ▸ ${fonctionnalite.accesSuffixe}` : cheminMenu);
    }
    if (fonctionnalite.acces) {
      parties.push(fonctionnalite.acces);
    }
    // Deux portes d'entrée légitimes (un menu et un bouton d'écran) sont présentées comme
    // telles, et non concaténées en un faux chemin unique.
    return parties.length > 0 ? parties.join(' — ou ') : null;
  }

  protected toggleModule(moduleId: string): void {
    this.expandedModules.update(set => {
      const next = new Set(set);
      if (next.has(moduleId)) {
        next.delete(moduleId);
      } else {
        next.add(moduleId);
      }
      return next;
    });
  }

  protected isExpanded(moduleId: string): boolean {
    return this.expandedModules().has(moduleId);
  }

  protected selectFonctionnalite(moduleId: string, fonctionnaliteNom: string): void {
    this.selected.set({ moduleId, fonctionnaliteNom });
    this.expandedModules.update(set => new Set(set).add(moduleId));
  }

  protected isSelected(moduleId: string, fonctionnaliteNom: string): boolean {
    const sel = this.selected();
    return !!sel && sel.moduleId === moduleId && sel.fonctionnaliteNom === fonctionnaliteNom;
  }

  /**
   * Écran illustrant l'étape `ordre` (numérotée à partir de 1), ou `null` si la campagne de
   * captures ne l'a pas photographiée — ou si l'étape reprend l'écran de la précédente, auquel
   * cas la génération a fusionné les deux images. Le rapprochement se fait ici plutôt que dans
   * le gabarit : il repose sur l'ordre, seule donnée qui lie une capture à l'étape jouée.
   */
  protected captureDeLEtape(scenario: ScenarioRecette, ordre: number): CaptureServie | null {
    const indexees = this.captures()[scenario.id] ?? scenario.captures;
    return indexees?.find(capture => capture.ordre === ordre) ?? null;
  }

  protected downloadPdf(): void {
    this.generatingPdf.set(true);
    this.cahierRecetteService.downloadPdf().subscribe({
      next: blob => {
        this.downloadService.downloadPdf(blob, 'guide-fonctionnalites');
        this.generatingPdf.set(false);
      },
      error: () => this.generatingPdf.set(false),
    });
  }

  protected downloadModulePdf(module: ModuleRecette): void {
    this.generatingModuleId.set(module.id);
    this.cahierRecetteService.downloadPdf([module.id]).subscribe({
      next: blob => {
        this.downloadService.downloadPdf(blob, `guide-fonctionnalites-${module.id.toLowerCase()}`);
        this.generatingModuleId.set(null);
      },
      error: () => this.generatingModuleId.set(null),
    });
  }

  protected downloadFeaturePdf(module: ModuleRecette, feature: FonctionnaliteRecette): void {
    const exportId = this.featureExportId(module.id, feature.nom);
    this.generatingModuleId.set(exportId);
    const scenarioIds = feature.scenarios.map(scenario => scenario.id);
    this.cahierRecetteService.downloadPdf([module.id], scenarioIds).subscribe({
      next: blob => {
        this.downloadService.downloadPdf(blob, `guide-${module.id.toLowerCase()}-${this.fileSlug(feature.nom)}`);
        this.generatingModuleId.set(null);
      },
      error: () => this.generatingModuleId.set(null),
    });
  }

  protected featureExportId(moduleId: string, featureName: string): string {
    return `${moduleId}:${featureName}`;
  }

  protected toggleFeatureSelection(moduleId: string, featureName: string): void {
    const key = this.featureExportId(moduleId, featureName);
    this.selectedFeatures.update(selection => {
      const next = new Set(selection);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  protected isFeatureSelected(moduleId: string, featureName: string): boolean {
    return this.selectedFeatures().has(this.featureExportId(moduleId, featureName));
  }

  protected clearFeatureSelection(): void {
    this.selectedFeatures.set(new Set());
  }

  protected downloadSelectedFeaturesPdf(): void {
    if (this.selectedFeatureCount() === 0) return;

    const selected = this.selectedFeatures();
    const moduleIds = new Set<string>();
    const scenarioIds: string[] = [];
    for (const module of this.modules) {
      for (const feature of module.fonctionnalites) {
        if (selected.has(this.featureExportId(module.id, feature.nom))) {
          moduleIds.add(module.id);
          scenarioIds.push(...feature.scenarios.map(scenario => scenario.id));
        }
      }
    }

    this.generatingModuleId.set('selection');
    this.cahierRecetteService.downloadPdf([...moduleIds], scenarioIds).subscribe({
      next: blob => {
        this.downloadService.downloadPdf(blob, 'guide-selection-parcours');
        this.generatingModuleId.set(null);
      },
      error: () => this.generatingModuleId.set(null),
    });
  }

  private fileSlug(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '');
  }
}
