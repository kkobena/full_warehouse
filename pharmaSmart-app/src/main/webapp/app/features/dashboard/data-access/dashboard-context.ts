import { computed, effect, Injectable, signal } from '@angular/core';
import { DashboardContextSettings, PeriodPreset } from '../models/dashboard.model';
import { DEFAULT_CONTEXT, resolvePeriod } from '../models/period';

/**
 * Période et rafraîchissement communs aux tuiles d'un dashboard, façon sélecteur de temps de
 * Kibana. Fourni par l'écran (éditeur ou accueil) : les widgets, créés dans son arbre, l'injectent.
 */
@Injectable()
export class DashboardContext {
  readonly period = signal<PeriodPreset>(DEFAULT_CONTEXT.period);
  readonly refreshSeconds = signal(DEFAULT_CONTEXT.refreshSeconds);
  /** Incrémenté à chaque rafraîchissement, manuel ou automatique : les widgets rechargent. */
  readonly tick = signal(0);

  readonly range = computed(() => resolvePeriod(this.period()));
  readonly settings = computed<DashboardContextSettings>(() => ({ period: this.period(), refreshSeconds: this.refreshSeconds() }));

  constructor() {
    effect(onCleanup => {
      const seconds = this.refreshSeconds();
      if (seconds <= 0) {
        return;
      }
      const timer = setInterval(() => this.refresh(), seconds * 1000);
      onCleanup(() => clearInterval(timer));
    });
  }

  apply(settings: DashboardContextSettings): void {
    this.period.set(settings.period);
    this.refreshSeconds.set(settings.refreshSeconds);
  }

  refresh(): void {
    this.tick.update(t => t + 1);
  }
}
