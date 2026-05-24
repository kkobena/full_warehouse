export type TypeZone =
  | 'AMBIANT'
  | 'FROID'
  | 'OTC'
  | 'ORDONNANCE'
  | 'TOXIQUE'
  | 'RESERVE'
  | 'PARA';

export interface IRayon {
  id?: number;
  code?: string;
  libelle?: string;
  storageLibelle?: string;
  storageType?: string;
  storageId?: number;
  exclude?: boolean;
  typeZone?: TypeZone;
  position?: string;
}

export const TYPE_ZONE_OPTIONS: { label: string; value: TypeZone }[] = [
  { label: 'Ambiant', value: 'AMBIANT' },
  { label: 'Froid', value: 'FROID' },
  { label: 'OTC', value: 'OTC' },
  { label: 'Ordonnance', value: 'ORDONNANCE' },
  { label: 'Toxique', value: 'TOXIQUE' },
  { label: 'Réserve', value: 'RESERVE' },
  { label: 'Para', value: 'PARA' },
];

export const TYPE_ZONE_SEVERITY: Record<TypeZone, string> = {
  AMBIANT: 'secondary',
  FROID: 'info',
  OTC: 'success',
  ORDONNANCE: 'warn',
  TOXIQUE: 'danger',
  RESERVE: 'contrast',
  PARA: 'secondary',
};
