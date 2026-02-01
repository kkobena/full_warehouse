import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';

/**
 * Reusable card component wrapping PrimeNG card
 * 
 * @example
 * <app-card
 *   header="Product Details"
 *   subheader="View product information"
 *   [customClass]="'custom-card'"
 * >
 *   <p>Card content goes here</p>
 *   <ng-template #footer>
 *     <app-button label="Edit" />
 *   </ng-template>
 * </app-card>
 */
@Component({
  selector: 'app-card',
  imports: [CommonModule, CardModule],
  template: `
    <p-card
      [header]="header()"
      [subheader]="subheader()"
      [style]="style()"
      [class]="customClass()"
    >
      <ng-content />
    </p-card>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  /** Card header text */
  header = input<string>('');
  
  /** Card subheader text */
  subheader = input<string>('');
  
  /** Inline styles */
  style = input<Record<string, any>>({});
  
  /** Custom CSS class */
  customClass = input<string>('');
}
