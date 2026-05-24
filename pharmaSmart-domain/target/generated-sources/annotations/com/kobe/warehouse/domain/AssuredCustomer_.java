package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AssuredCustomer}
 **/
@StaticMetamodel(AssuredCustomer.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AssuredCustomer_ extends Customer_ {

	
	/**
	 * @see #sexe
	 **/
	public static final String SEXE = "sexe";
	
	/**
	 * @see #datNaiss
	 **/
	public static final String DAT_NAISS = "datNaiss";
	
	/**
	 * @see #assurePrincipal
	 **/
	public static final String ASSURE_PRINCIPAL = "assurePrincipal";
	
	/**
	 * @see #numAyantDroit
	 **/
	public static final String NUM_AYANT_DROIT = "numAyantDroit";
	
	/**
	 * @see #ayantDroits
	 **/
	public static final String AYANT_DROITS = "ayantDroits";
	
	/**
	 * @see #clientTiersPayants
	 **/
	public static final String CLIENT_TIERS_PAYANTS = "clientTiersPayants";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AssuredCustomer}
	 **/
	public static volatile EntityType<AssuredCustomer> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AssuredCustomer#sexe}
	 **/
	public static volatile SingularAttribute<AssuredCustomer, String> sexe;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AssuredCustomer#datNaiss}
	 **/
	public static volatile SingularAttribute<AssuredCustomer, LocalDate> datNaiss;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AssuredCustomer#assurePrincipal}
	 **/
	public static volatile SingularAttribute<AssuredCustomer, AssuredCustomer> assurePrincipal;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AssuredCustomer#numAyantDroit}
	 **/
	public static volatile SingularAttribute<AssuredCustomer, String> numAyantDroit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AssuredCustomer#ayantDroits}
	 **/
	public static volatile SetAttribute<AssuredCustomer, AssuredCustomer> ayantDroits;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AssuredCustomer#clientTiersPayants}
	 **/
	public static volatile SetAttribute<AssuredCustomer, ClientTiersPayant> clientTiersPayants;

}

