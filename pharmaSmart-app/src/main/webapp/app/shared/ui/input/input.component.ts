import { Component, ChangeDetectionStrategy, input, model } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';

/**
 * Reusable input component wrapping PrimeNG input text
 * 
 * @example
 * <app-input
 *   [(value)]="name"
 *   placeholder="Enter name"
 *   [disabled]="isLoading()"
 *   [required]="true"
 * />
 */
@Component({
  selector: 'app-input',
  imports: [CommonModule, FormsModule, InputTextModule],
  template: `
    <input
      pInputText
      [(ngModel)]="value"
      [placeholder]="placeholder()"
      [disabled]="disabled()"
      [required]="required()"
      [readonly]="readonly()"
      [size]="size()"
      [class]="customClass()"
      [style]="style()"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InputComponent {
  /** Input value (two-way binding) */
  value = model<string>('');
  
  /** Placeholder text */
  placeholder = input<string>('');
  
  /** Disable input */
  disabled = input<boolean>(false);
  
  /** Required field */
  required = input<boolean>(false);
  
  /** Read-only input */
  readonly = input<boolean>(false);
  
  /** Input size */
  size = input<number | undefined>(undefined);
  
  /** Custom CSS class */
  customClass = input<string>('');
  
  /** Inline styles */
  style = input<Record<string, any>>({});
}
