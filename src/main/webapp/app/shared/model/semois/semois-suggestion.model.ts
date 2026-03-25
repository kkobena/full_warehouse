import { ClasseCriticite } from './classe-criticite.model';

/**
 * Modèle pour les suggestions de réapprovisionnement SEMOIS
 */
export interface ISemoisSuggestion {
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  fournisseurId?: number;
  fournisseurLibelle?: string;
  classeCriticite?: ClasseCriticite;
  vmm?: number; // Ventes Mensuelles Moyennes (pondérées)
  margeSecurite?: number; // Marge de sécurité calculée
  stockObjectif?: number; // Stock objectif SEMOIS (VMM + marge de sécurité)
  stockActuel?: number; // Stock actuel total du produit
  quantiteACommander?: number; // Quantité à commander (Stock Objectif - Stock Actuel, si > 0)
  delaiLivraisonJours?: number; // Délai de livraison du fournisseur en jours
  coefficientSecurite?: number; // Coefficient de sécurité appliqué
  facteurSaisonnier?: number; // Facteur d'ajustement saisonnier actuel
  dateDernierCalcul?: Date; // Date du dernier calcul SEMOIS
}

export class SemoisSuggestion implements ISemoisSuggestion {
  constructor(
    public produitId?: number,
    public libelle?: string,
    public codeCip?: string,
    public fournisseurId?: number,
    public fournisseurLibelle?: string,
    public classeCriticite?: ClasseCriticite,
    public vmm?: number,
    public margeSecurite?: number,
    public stockObjectif?: number,
    public stockActuel?: number,
    public quantiteACommander?: number,
    public delaiLivraisonJours?: number,
    public coefficientSecurite?: number,
    public facteurSaisonnier?: number,
    public dateDernierCalcul?: Date,
  ) {}

  /**
   * Calcule le taux de couverture en mois
   * @returns Nombre de mois de stock disponible (Stock Actuel / VMM)
   */
  getTauxCouvertureMois(): number {
    if (!this.vmm || this.vmm === 0) {
      return 0;
    }
    return (this.stockActuel ?? 0) / this.vmm;
  }

  /**
   * Calcule la couverture cible en mois (objectif pharmacien).
   * Indique combien de mois de ventes le stock objectif représente.
   * Formule : Stock Objectif / VMM
   * @returns Nombre de mois de couverture cible
   */
  getCouvertureCibleMois(): number {
    if (!this.vmm || this.vmm === 0) {
      return 0;
    }
    return (this.stockObjectif ?? 0) / this.vmm;
  }

  /**
   * Calcule la couverture de la marge de sécurité en mois.
   * Formule : Marge de Sécurité / VMM
   * @returns Nombre de mois de marge de sécurité
   */
  getMargeSecuriteCibleMois(): number {
    if (!this.vmm || this.vmm === 0) {
      return 0;
    }
    return (this.margeSecurite ?? 0) / this.vmm;
  }

  /**
   * Retourne la classe CSS de couleur pour la couverture cible.
   * Même logique que getTauxCouvertureMois mais appliquée au stock objectif.
   */
  getCouvertureCibleClass(): string {
    const cible = this.getCouvertureCibleMois();
    if (cible < 0.5) return 'text-danger';
    if (cible < 1.0) return 'text-warning';
    if (cible <= 3.0) return 'text-success';
    return 'text-info';
  }

  /**
   * Vérifie si le produit est en rupture potentielle
   * @returns true si le stock actuel est inférieur à la marge de sécurité
   */
  estEnRupture(): boolean {
    if (this.stockActuel === undefined || this.margeSecurite === undefined) {
      return false;
    }
    return this.stockActuel < this.margeSecurite;
  }

  /**
   * Vérifie si le produit est en surstock
   * @returns true si le stock actuel dépasse 150% du stock objectif
   */
  estEnSurstock(): boolean {
    if (this.stockActuel === undefined || !this.stockObjectif || this.stockObjectif === 0) {
      return false;
    }
    return this.stockActuel > this.stockObjectif * 1.5;
  }

  /**
   * Calcule l'écart entre stock actuel et stock objectif
   * @returns Écart en unités (positif = surstock, négatif = sous-stock)
   */
  getEcartStockObjectif(): number {
    if (this.stockActuel === undefined || this.stockObjectif === undefined) {
      return 0;
    }
    return this.stockActuel - this.stockObjectif;
  }

  /**
   * Calcule l'écart en pourcentage par rapport au stock objectif
   * @returns Écart en % (positif = surstock, négatif = sous-stock)
   */
  getEcartStockObjectifPourcent(): number {
    if (!this.stockObjectif || this.stockObjectif === 0) {
      return 0;
    }
    return (this.getEcartStockObjectif() / this.stockObjectif) * 100.0;
  }

  /**
   * Vérifie si un réapprovisionnement est nécessaire
   * @returns true si quantiteACommander > 0
   */
  necessiteReappro(): boolean {
    return !!this.quantiteACommander && this.quantiteACommander > 0;
  }

  /**
   * Obtient le niveau d'urgence du réapprovisionnement
   * @returns "URGENT" si stock < marge sécurité, "NORMAL" si stock < objectif, "OK" sinon
   */
  getNiveauUrgence(): 'URGENT' | 'NORMAL' | 'OK' {
    if (this.estEnRupture()) {
      return 'URGENT';
    }
    if (this.necessiteReappro()) {
      return 'NORMAL';
    }
    return 'OK';
  }

  /**
   * Calcule le nombre de jours de stock restant
   * @returns Nombre de jours approximatif de stock (Stock Actuel / VMM * 30)
   */
  getJoursStockRestant(): number {
    if (!this.vmm || this.vmm === 0 || this.stockActuel === undefined) {
      return 0;
    }
    const vmmJour = this.vmm / 30.0;
    return Math.ceil(this.stockActuel / vmmJour);
  }

  /**
   * Vérifie si le calcul est récent (< 24h)
   * @returns true si le calcul date de moins de 24h
   */
  estCalculRecent(): boolean {
    if (!this.dateDernierCalcul) {
      return false;
    }
    const oneDayAgo = new Date();
    oneDayAgo.setDate(oneDayAgo.getDate() - 1);
    return new Date(this.dateDernierCalcul) > oneDayAgo;
  }
}
