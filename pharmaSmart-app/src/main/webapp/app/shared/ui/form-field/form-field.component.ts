import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Reusable form field wrapper component
 * Provides consistent layout and styling for form inputs with labels and error messages
 * 
 * @example
 * <app-form-field
 *   label="Product Name"
 *   [required]="true"
 *   [error]="nameError()"
 * >
 *   <app-input [(ngModel)]="productName" />
 * </app-form-field>
 */
@Component({
  selector: 'app-form-field',
  imports: [CommonModule],
  template: `
    <div class="form-field" [class.has-error]="error()">
      @if (label()) {
        <label class="form-field-label">
          {{ label() }}
          @if (required()) {
            <span class="text-danger">*</span>
          }
        </label>
      }
      
      <div class="form-field-control">
        <ng-content />
      </div>
      
      @if (error()) {
        <small class="form-field-error text-danger">
          {{ error() }}
        </small>
      }
      
      @if (hint() && !error()) {
        <small class="form-field-hint text-muted">
          {{ hint() }}
        </small>
      }
    </div>
  `,
  styles: [`
    .form-field {
      margin-bottom: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    
    .form-field-label {
      font-weight: 500;
      margin-bottom: 0.25rem;
    }
    
    .form-field-control {
      width: 100%;
    }
    
    .form-field-error,
    .form-field-hint {
      display: block;
      margin-top: 0.25rem;
      font-size: 0.875rem;
    }
    
    .form-field.has-error .form-field-control ::ng-deep input,
    .form-field.has-error .form-field-control ::ng-deep textarea,
    .form-field.has-error .form-field-control ::ng-deep select {
      border-color: var(--red-500);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormFieldComponent {
  /** Field label */
  label = input<string>('');
  
  /** Mark field as required */
  required = input<boolean>(false);
  
  /** Error message to display */
  error = input<string>('');
  
  /** Hint/help text */
  hint = input<string>('');
}
