package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ReconciliationStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur}
 **/
@StaticMetamodel(ReconciliationFactureFournisseur.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ReconciliationFactureFournisseur_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #factureReference
	 **/
	public static final String FACTURE_REFERENCE = "factureReference";
	
	/**
	 * @see #factureDate
	 **/
	public static final String FACTURE_DATE = "factureDate";
	
	/**
	 * @see #factureMontantHT
	 **/
	public static final String FACTURE_MONTANT_HT = "factureMontantHT";
	
	/**
	 * @see #factureTVA
	 **/
	public static final String FACTURE_TVA = "factureTVA";
	
	/**
	 * @see #blMontantHT
	 **/
	public static final String BL_MONTANT_HT = "blMontantHT";
	
	/**
	 * @see #blTVA
	 **/
	public static final String BL_TVA = "blTVA";
	
	/**
	 * @see #ecartHT
	 **/
	public static final String ECART_HT = "ecartHT";
	
	/**
	 * @see #ecartTVA
	 **/
	public static final String ECART_TVA = "ecartTVA";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #avoir
	 **/
	public static final String AVOIR = "avoir";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur}
	 **/
	public static volatile EntityType<ReconciliationFactureFournisseur> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#id}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#factureReference}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, String> factureReference;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#factureDate}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, LocalDate> factureDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#factureMontantHT}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, Integer> factureMontantHT;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#factureTVA}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, Integer> factureTVA;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#blMontantHT}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, Integer> blMontantHT;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#blTVA}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, Integer> blTVA;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#ecartHT}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, Integer> ecartHT;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#ecartTVA}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, Integer> ecartTVA;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#statut}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, ReconciliationStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#createdAt}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#updatedAt}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ReconciliationFactureFournisseur#avoir}
	 **/
	public static volatile SingularAttribute<ReconciliationFactureFournisseur, AvoirFournisseur> avoir;

}

