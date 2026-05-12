export enum StatutLegal {
  SANS_LISTE = 'SANS_LISTE',
  LISTE_I = 'LISTE_I',
  LISTE_II = 'LISTE_II',
  STUPEFIANTS = 'STUPEFIANTS',
  PSO = 'PSO',
}

export const STATUT_LEGAL_OPTIONS = [
  { value: StatutLegal.SANS_LISTE, label: 'Sans liste', description: 'Sans ordonnance — dispensation libre' },
  { value: StatutLegal.LISTE_I, label: 'Liste I', description: 'Ordonnance obligatoire, renouvelable' },
  { value: StatutLegal.LISTE_II, label: 'Liste II', description: 'Ordonnance obligatoire, non renouvelable' },
  { value: StatutLegal.STUPEFIANTS, label: 'Stupéfiants', description: 'Ordonnance sécurisée, retour interdit' },
  { value: StatutLegal.PSO, label: 'PSO', description: 'Prescription sécurisée obligatoire' },
];

