import { inject, Injectable } from '@angular/core';
import Aura from '@primeuix/themes/aura';
import Lara from '@primeuix/themes/lara';
import Material from '@primeuix/themes/material';
import Nora from '@primeuix/themes/nora';
import { PrimeNG } from 'primeng/config';

export interface Theme {
  name: string;
  type: 'primeng' | 'bootswatch';
  value: any;
}

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private themes: Theme[] = [
    { name: 'Aura', type: 'primeng', value: Aura },
    { name: 'Lara', type: 'primeng', value: Lara },
    { name: 'Material', type: 'primeng', value: Material },
    { name: 'Nora', type: 'primeng', value: Nora },
  ];
  private primengConfig = inject(PrimeNG);

  getThemes(): Theme[] {
    return this.themes;
  }

  setTheme(themeName: string) {
    switch (themeName) {
      case 'Aura':
        this.primengConfig.theme.set({ preset: Aura });
        break;
      case 'Lara':
        this.primengConfig.theme.set({ preset: Lara });
        break;
      case 'Nora':
        this.primengConfig.theme.set({ preset: Nora });
        break;
      case 'Material':
        this.primengConfig.theme.set({ preset: Material });
        break;
      default:
        this.primengConfig.theme.set({ preset: Aura });
    }
    localStorage.setItem('selected-theme', themeName);
  }

  loadCurrentTheme(): void {
    const themeName = localStorage.getItem('selected-theme') || 'Aura';
    if (themeName) {
      this.setTheme(themeName);
    }
  }
}
