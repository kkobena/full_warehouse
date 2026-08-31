export class Storage {
  id?: number;
  name?: string;
  /**
   * LIBELLÉ du type de stockage — « Stock rayon », « Réserve » — et non son code : il vient
   * de `StorageType.getValue()`, destiné à l'affichage. Pour tester la nature d'un stockage,
   * c'est `type` qu'il faut lire.
   */
  storageType?: string;
  /** CODE du type de stockage : `PRINCIPAL`, `SAFETY_STOCK`… C'est lui qu'on compare. */
  type?: string;
  magasinName?: string;
  magasinId?: number;
}
