import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable } from '@angular/core';

import { ChromeThemeService } from 'app/core/theme/chrome-theme.service';
import { ThemeService } from 'app/core/theme/theme.service';
import { accentColor, surfaceBorder, textColor, textColorSecondary } from 'app/shared/chart-color-helper';

/** Nombre de séries de `--pharma-chart-serie-*` (`_pharma-themes.scss`). */
const SERIES = 8;

export interface ChartThemeColors {
  /** Série principale : l'accent du thème. */
  accent: string;
  text: string;
  textMuted: string;
  border: string;
  /** Une couleur par série quand il y en a plusieurs, contrastée sur fond clair. */
  series: string[];
}

/**
 * Couleurs des graphiques lues sur les jetons du thème, relues quand le thème ou le style de poste change : un graphique qui en
 * dépend se redessine aux nouvelles couleurs sans recharger la page.
 */
@Injectable({ providedIn: 'root' })
export class ChartThemeColorsService {
  private readonly document = inject(DOCUMENT);
  private readonly theme = inject(ThemeService).theme;
  private readonly chrome = inject(ChromeThemeService).chrome;

  readonly colors = computed<ChartThemeColors>(() => {
    this.theme();
    this.chrome();
    const style = getComputedStyle(this.document.documentElement);
    return {
      accent: accentColor(style),
      text: textColor(style),
      textMuted: textColorSecondary(style),
      border: surfaceBorder(style),
      series: Array.from({ length: SERIES }, (_, rang) => style.getPropertyValue(`--pharma-chart-serie-${rang + 1}`).trim()),
    };
  });
}
