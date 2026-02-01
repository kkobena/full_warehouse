/**
 * Common imports file to replace NgModules
 * Use this to import common Angular modules and shared components
 */
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

// PrimeNG Modules
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { TooltipModule } from 'primeng/tooltip';

/**
 * Common Angular and third-party modules
 * Import this array in standalone components that need these common modules
 */
export const COMMON_IMPORTS = [
  CommonModule,
  FormsModule,
  ReactiveFormsModule,
  TranslateModule,
  FontAwesomeModule,
  NgbModule,
  // PrimeNG
  ButtonModule,
  InputTextModule,
  TableModule,
  CardModule,
  DividerModule,
  AutoCompleteModule,
  TooltipModule,
] as const;

/**
 * Common shared components
 * Import this array in components that need alert components
 */
export const COMMON_COMPONENTS = [] as const;

/**
 * Type helper for common imports
 */
export type CommonImports = typeof COMMON_IMPORTS;
export type CommonComponents = typeof COMMON_COMPONENTS;
