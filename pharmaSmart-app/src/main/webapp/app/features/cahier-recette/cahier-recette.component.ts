import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BadgeComponent, ButtonComponent } from '../../shared/ui';
import { CAHIER_RECETTE, FonctionnaliteRecette, ModuleRecette } from './cahier-recette.model';

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
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './cahier-recette.component.scss',
})
export class CahierRecetteComponent {
  protected readonly modules = withoutHidden(CAHIER_RECETTE);

  protected readonly searchText = signal('');
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

  protected printReport(): void {
    setTimeout(() => window.print(), 0);
  }
}
