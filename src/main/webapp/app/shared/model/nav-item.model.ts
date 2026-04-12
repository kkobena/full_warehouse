// Modèle de navigation dynamique — indépendant du NavItem frontend existant
export interface INavNode {
  id: number;
  code: string;           // clé métier unique ex: "commande"
  libelle: string;        // label affiché
  icon?: string;          // "pi pi-send" (PrimeIcons)
  routerLink?: string;    // "/commande"
  badgeType?: 'RUPTURE' | 'PEREMPTION' | 'URGENT' | 'NONE';
  targetType: 'ROUTE' | 'ACTION' | 'GROUP' | 'SECTION' | 'DIVIDER';
  ordre: number;
  children?: INavNode[];
  permissions?: INavPermissions;
}

export interface INavPermissions {
  canDisplay: boolean;
  canAccess: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canExport: boolean;
  canExecute: boolean;
}

export type NavAbilityAction = 'display' | 'access' | 'create' | 'edit' | 'delete' | 'export' | 'execute';

export interface IAbility {
  action: NavAbilityAction;
  subject: string; // = INavNode.code
}

export interface NavReorderPayload {
  navItemId: number;
  newOrdre: number;
  newParentId?: number | null;
}

export interface NavAssignPayload {
  roleName: string;
  assignments: NavItemAssignment[];
}

export interface NavItemAssignment {
  navItemId: number;
  canDisplay: boolean;
  canAccess: boolean;
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canExport: boolean;
  canExecute: boolean;
}

