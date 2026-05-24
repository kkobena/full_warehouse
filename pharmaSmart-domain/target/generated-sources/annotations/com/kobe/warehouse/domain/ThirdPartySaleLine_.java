package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ThirdPartySaleLine}
 **/
@StaticMetamodel(ThirdPartySaleLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ThirdPartySaleLine_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #saleDate
	 **/
	public static final String SALE_DATE = "saleDate";
	
	/**
	 * @see #sale
	 **/
	public static final String SALE = "sale";
	
	/**
	 * @see #numBon
	 **/
	public static final String NUM_BON = "numBon";
	
	/**
	 * @see #clientTiersPayant
	 **/
	public static final String CLIENT_TIERS_PAYANT = "clientTiersPayant";
	
	/**
	 * @see #montant
	 **/
	public static final String MONTANT = "montant";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #effectiveUpdateDate
	 **/
	public static final String EFFECTIVE_UPDATE_DATE = "effectiveUpdateDate";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #taux
	 **/
	public static final String TAUX = "taux";
	
	/**
	 * @see #tauxVente
	 **/
	public static final String TAUX_VENTE = "tauxVente";
	
	/**
	 * @see #montantRegle
	 **/
	public static final String MONTANT_REGLE = "montantRegle";
	
	/**
	 * @see #repartitions
	 **/
	public static final String REPARTITIONS = "repartitions";
	
	/**
	 * @see #factureTiersPayant
	 **/
	public static final String FACTURE_TIERS_PAYANT = "factureTiersPayant";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ThirdPartySaleLine}
	 **/
	public static volatile EntityType<ThirdPartySaleLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#id}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#saleDate}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, LocalDate> saleDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#sale}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, ThirdPartySales> sale;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#numBon}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, String> numBon;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#clientTiersPayant}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, ClientTiersPayant> clientTiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#montant}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, Integer> montant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#created}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#updated}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#effectiveUpdateDate}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, LocalDateTime> effectiveUpdateDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#statut}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, ThirdPartySaleStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#taux}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, Short> taux;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#tauxVente}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, Short> tauxVente;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#montantRegle}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, Integer> montantRegle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#repartitions}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, List> repartitions;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySaleLine#factureTiersPayant}
	 **/
	public static volatile SingularAttribute<ThirdPartySaleLine, FactureTiersPayant> factureTiersPayant;

}

