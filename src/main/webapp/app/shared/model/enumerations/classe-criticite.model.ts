export enum ClasseCriticite {
  A_PLUS = 'A_PLUS',
  A = 'A',
  B = 'B',
  C = 'C',
  D = 'D',
}

export const CLASSE_CRITICITE_OPTIONS = [
  { value: ClasseCriticite.A_PLUS, label: 'A+ — Produits vitaux', description: 'Insulines, anticoagulants, antibiotiques critiques', badge: 'danger' },
  { value: ClasseCriticite.A, label: 'A — Forte rotation', description: 'Paracétamol, AINS courants, OTC populaires', badge: 'warning' },
  { value: ClasseCriticite.B, label: 'B — Rotation moyenne', description: 'Vitamines, compléments alimentaires', badge: 'info' },
  { value: ClasseCriticite.C, label: 'C — Faible rotation', description: 'Produits de niche, spécialités rares', badge: 'secondary' },
  { value: ClasseCriticite.D, label: 'D — Très faible rotation', description: 'Produits en fin de vie', badge: 'secondary' },
];

