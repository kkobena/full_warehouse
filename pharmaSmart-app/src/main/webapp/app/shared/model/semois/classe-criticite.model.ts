/**
 * Énumération des classes de criticité SEMOIS
 */
export enum ClasseCriticite {
  A_PLUS = 'A_PLUS', // Produits vitaux (coefficient 1.5)
  A = 'A', // Forte rotation (coefficient 1.0)
  B = 'B', // Rotation moyenne (coefficient 0.7)
  C = 'C', // Faible rotation (coefficient 0.4)
  D = 'D', // Très faible rotation (coefficient 0.2)
}

export interface IClasseCriticiteInfo {
  code: ClasseCriticite;
  label: string;
  description: string;
  coefficientDefaut: number;
  color: string;
  severity: 'success' | 'info' | 'warn' | 'danger' | 'secondary';
}

export const CLASSE_CRITICITE_INFO: Record<ClasseCriticite, IClasseCriticiteInfo> = {
  [ClasseCriticite.A_PLUS]: {
    code: ClasseCriticite.A_PLUS,
    label: 'A+',
    description: 'Produits vitaux (insuline, adrénaline, etc.)',
    coefficientDefaut: 1.5,
    color: '#D32F2F',
    severity: 'danger',
  },
  [ClasseCriticite.A]: {
    code: ClasseCriticite.A,
    label: 'A',
    description: 'Forte rotation',
    coefficientDefaut: 1.0,
    color: '#388E3C',
    severity: 'success',
  },
  [ClasseCriticite.B]: {
    code: ClasseCriticite.B,
    label: 'B',
    description: 'Rotation moyenne',
    coefficientDefaut: 0.7,
    color: '#1976D2',
    severity: 'info',
  },
  [ClasseCriticite.C]: {
    code: ClasseCriticite.C,
    label: 'C',
    description: 'Faible rotation',
    coefficientDefaut: 0.4,
    color: '#F57C00',
    severity: 'warn',
  },
  [ClasseCriticite.D]: {
    code: ClasseCriticite.D,
    label: 'D',
    description: 'Très faible rotation',
    coefficientDefaut: 0.2,
    color: '#757575',
    severity: 'secondary',
  },
};

export function getClasseCriticiteInfo(classe?: ClasseCriticite): IClasseCriticiteInfo {
  return classe ? CLASSE_CRITICITE_INFO[classe] : CLASSE_CRITICITE_INFO[ClasseCriticite.B];
}
