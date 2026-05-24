export  enum SalesStatut {
  PROCESSING = 'PROCESSING', //Status d'une pré-vente en cours de traitement
  PENDING = 'PENDING', //Status d'une pré-vente validée qui peut être rappeler pour être traité comme vente en cours
  CLOSE = 'CLOSE',
  PAID = 'PAID',
  UNPAID = 'UNPAID',
  ENABLE = 'ENABLE',
  DISABLE = 'DISABLE',
  DELETED = 'DELETED',
  CLOSED = 'CLOSED', //Status d'une vente cloturée
  ACTIVE = 'ACTIVE', // Status d'une vente en cours
  DEVIS = 'DEVIS', // Status d'une vente en devis
  CANCELED = 'CANCELED',
}
