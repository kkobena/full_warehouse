import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ChartColorsUtilsService {
  colors = signal<string[]>([]);
  hoverColors = signal<string[]>([]);
  textColor = signal<string>('');
  textColorSecondary = signal<string>('');
  surfaceBorder = signal<string>('');
  private documentStyle: CSSStyleDeclaration;

  constructor() {
    this.documentStyle = getComputedStyle(document.documentElement);
    this.textColor.set(this.documentStyle.getPropertyValue('--p-text-color'));
    this.textColorSecondary.set(this.documentStyle.getPropertyValue('--p-text-color-secondary'));
    this.surfaceBorder.set(this.documentStyle.getPropertyValue('--p-surface-border'));
    this.colors.set([
      this.documentStyle.getPropertyValue('--p-blue-300'),
      this.documentStyle.getPropertyValue('--p-yellow-300'),
      this.documentStyle.getPropertyValue('--p-green-300'),
      this.documentStyle.getPropertyValue('--p-pink-300'),
      this.documentStyle.getPropertyValue('--p-orange-300'),
      this.documentStyle.getPropertyValue('--p-red-300'),
    ]);
    this.hoverColors.set([
      this.documentStyle.getPropertyValue('--p-blue-200'),
      this.documentStyle.getPropertyValue('--p-yellow-200'),
      this.documentStyle.getPropertyValue('--p-green-200'),
      this.documentStyle.getPropertyValue('--p-pink-200'),
      this.documentStyle.getPropertyValue('--p-orange-200'),
      this.documentStyle.getPropertyValue('--p-red-200'),
    ]);
  }
}
