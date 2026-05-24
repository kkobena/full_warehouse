package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AvoirTiersPayant}
 **/
@StaticMetamodel(AvoirTiersPayant.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AvoirTiersPayant_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #numAvoir
	 **/
	public static final String NUM_AVOIR = "numAvoir";
	
	/**
	 * @see #factureTiersPayant
	 **/
	public static final String FACTURE_TIERS_PAYANT = "factureTiersPayant";
	
	/**
	 * @see #montantAvoir
	 **/
	public static final String MONTANT_AVOIR = "montantAvoir";
	
	/**
	 * @see #montantTva
	 **/
	public static final String MONTANT_TVA = "montantTva";
	
	/**
	 * @see #montantHt
	 **/
	public static final String MONTANT_HT = "montantHt";
	
	/**
	 * @see #motif
	 **/
	public static final String MOTIF = "motif";
	
	/**
	 * @see #avoirDate
	 **/
	public static final String AVOIR_DATE = "avoirDate";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #lignes
	 **/
	public static final String LIGNES = "lignes";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AvoirTiersPayant}
	 **/
	public static volatile EntityType<AvoirTiersPayant> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#id}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#numAvoir}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, String> numAvoir;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#factureTiersPayant}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, FactureTiersPayant> factureTiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#montantAvoir}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, BigDecimal> montantAvoir;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#montantTva}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, BigDecimal> montantTva;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#montantHt}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, BigDecimal> montantHt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#motif}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, String> motif;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#avoirDate}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, LocalDate> avoirDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#statut}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, AvoirStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#user}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#created}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#updated}
	 **/
	public static volatile SingularAttribute<AvoirTiersPayant, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AvoirTiersPayant#lignes}
	 **/
	public static volatile ListAttribute<AvoirTiersPayant, AvoirLine> lignes;

}

