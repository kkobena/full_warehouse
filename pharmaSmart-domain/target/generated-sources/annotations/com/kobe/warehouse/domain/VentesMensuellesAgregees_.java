package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.VentesMensuellesAgregees}
 **/
@StaticMetamodel(VentesMensuellesAgregees.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class VentesMensuellesAgregees_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #anneeMois
	 **/
	public static final String ANNEE_MOIS = "anneeMois";
	
	/**
	 * @see #quantiteVendue
	 **/
	public static final String QUANTITE_VENDUE = "quantiteVendue";
	
	/**
	 * @see #montantCa
	 **/
	public static final String MONTANT_CA = "montantCa";
	
	/**
	 * @see #nombreVentes
	 **/
	public static final String NOMBRE_VENTES = "nombreVentes";
	
	/**
	 * @see #isFrozen
	 **/
	public static final String IS_FROZEN = "isFrozen";
	
	/**
	 * @see #freezeDate
	 **/
	public static final String FREEZE_DATE = "freezeDate";
	
	/**
	 * @see #estRuptureFournisseur
	 **/
	public static final String EST_RUPTURE_FOURNISSEUR = "estRuptureFournisseur";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.VentesMensuellesAgregees}
	 **/
	public static volatile EntityType<VentesMensuellesAgregees> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#id}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#produit}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#anneeMois}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, String> anneeMois;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#quantiteVendue}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, Integer> quantiteVendue;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#montantCa}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, Integer> montantCa;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#nombreVentes}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, Integer> nombreVentes;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#isFrozen}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, Boolean> isFrozen;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#freezeDate}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, LocalDateTime> freezeDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#estRuptureFournisseur}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, Boolean> estRuptureFournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#createdAt}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VentesMensuellesAgregees#updatedAt}
	 **/
	public static volatile SingularAttribute<VentesMensuellesAgregees, LocalDateTime> updatedAt;

}

