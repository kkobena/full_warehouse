import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';

/**
 * Reusable button component wrapping PrimeNG button
 * 
 * @example
 * <app-button
 *   label="Save"
 *   icon="pi pi-save"
 *   [loading]="isSaving()"
 *   [disabled]="!isValid()"
 *   severity="primary"
 *   (onClick)="save()"
 * />
 */
@Component({
  selector: 'app-button',
  imports: [ButtonModule],
  template: `
    <p-button
      [label]="label()"
      [icon]="icon()"
      [loading]="loading()"
      [disabled]="disabled()"
      [severity]="severity()"
      [size]="size()"
      [text]="text()"
      [outlined]="outlined()"
      [raised]="raised()"
      [rounded]="rounded()"
      [class]="customClass()"
      (onClick)="onClick.emit()"
    />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ButtonComponent {
  /** Button label text */
  label = input<string>('');
  
  /** Icon class (e.g., 'pi pi-save') */
  icon = input<string>('');
  
  /** Show loading spinner */
  loading = input<boolean>(false);
  
  /** Disable button */
  disabled = input<boolean>(false);
  
  /** Button severity/color theme */
  severity = input<'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'danger' | 'help'>('primary');
  
  /** Button size */
  size = input<'small' | 'large' | undefined>(undefined);
  
  /** Text-only button (no background) */
  text = input<boolean>(false);
  
  /** Outlined button */
  outlined = input<boolean>(false);
  
  /** Raised button with shadow */
  raised = input<boolean>(false);
  
  /** Rounded button */
  rounded = input<boolean>(false);
  
  /** Custom CSS classes */
  customClass = input<string>('');
  
  /** Click event */
  onClick = output<void>();
}
