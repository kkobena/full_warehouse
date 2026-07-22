/**
 * Common imports file to replace NgModules
 * Use this to import common Angular modules and shared components
 */
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';

// Composants du Design System (remplacent les anciens modules PrimeNG)
// NgbModule couvre déjà le remplacement de TooltipModule (NgbTooltip est inclus).
// DividerModule n'a pas d'équivalent : un simple <hr> suffit dans les templates.
import {ButtonComponent, CardComponent, DataTableComponent, SelectSearchComponent} from 'app/shared/ui';

// Contrôle d'accès fin (ABAC)
import {HasAbilityDirective} from 'app/shared/auth/has-ability.directive';
import {HasAbilityPipe} from 'app/shared/auth/has-ability.pipe';

/**
 * Common Angular and third-party modules
 * Import this array in standalone components that need these common modules
 */
export const COMMON_IMPORTS = [
  CommonModule,
  FormsModule,
  ReactiveFormsModule,
  FontAwesomeModule,
  NgbModule,
  // Design System (remplace les anciens modules PrimeNG)
  ButtonComponent,
  DataTableComponent,
  CardComponent,
  SelectSearchComponent,
  // ABAC
  HasAbilityDirective,
  HasAbilityPipe,
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

// Re-exports pour faciliter l'import individuel
export {HasAbilityDirective} from 'app/shared/auth/has-ability.directive';
export {HasAbilityPipe} from 'app/shared/auth/has-ability.pipe';

